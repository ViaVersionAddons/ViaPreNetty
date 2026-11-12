package viaprenetty.net;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaChannelHandler;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import viaprenetty.LegacyVersionProvider;
import viaprenetty.LegacyVersions;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.storage.LegacyClient;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public final class LegacyConnection extends ChannelDuplexHandler {

    public static final String NAME = "viaprenetty-codec";

    private enum Phase {
        HANDSHAKE,
        OLD_LOGIN,
        ENCRYPTION,
        AWAIT_SPAWN,
        LOGIN,
        PLAY,
        CLOSED
    }

    private final ByteBuf cumulation = Unpooled.buffer();
    private final Queue<ByteBuf> pending = new ArrayDeque<>();
    private ChannelHandlerContext ctx;
    private Phase phase = Phase.HANDSHAKE;
    private ProtocolVersion version;
    private boolean v16;
    private String username;
    private String host;
    private int port;
    private byte[] verifyToken;
    private Cipher decrypt;
    private Cipher encrypt;

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        this.cumulation.release();
        ByteBuf buf;
        while ((buf = this.pending.poll()) != null) {
            buf.release();
        }
    }

    public ProtocolVersion version() {
        return this.version;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof ByteBuf in)) {
            ctx.fireChannelRead(msg);
            return;
        }
        try {
            if (this.phase == Phase.CLOSED) {
                return;
            }
            if (this.decrypt != null) {
                final byte[] data = new byte[in.readableBytes()];
                in.readBytes(data);
                final byte[] plain = this.decrypt.update(data);
                if (plain != null) {
                    this.cumulation.writeBytes(plain);
                }
            } else {
                this.cumulation.writeBytes(in);
            }
        } finally {
            in.release();
        }
        process(ctx);
    }

    private void process(final ChannelHandlerContext ctx) throws Exception {
        while (this.cumulation.isReadable() && this.phase != Phase.CLOSED) {
            this.cumulation.markReaderIndex();
            try {
                if (!step(ctx)) {
                    this.cumulation.resetReaderIndex();
                    break;
                }
            } catch (final IndexOutOfBoundsException e) {
                this.cumulation.resetReaderIndex();
                break;
            }
        }
        this.cumulation.discardSomeReadBytes();
    }

    private boolean step(final ChannelHandlerContext ctx) throws Exception {
        switch (this.phase) {
            case HANDSHAKE -> {
                final int id = this.cumulation.readUnsignedByte();
                if (id != 0x02) {
                    kick(ctx, "Unexpected packet");
                    return true;
                }
                if (this.cumulation.getUnsignedByte(this.cumulation.readerIndex()) == 0) {
                    final String info = readString(this.cumulation);
                    final int split = info.indexOf(';');
                    this.username = split >= 0 ? info.substring(0, split) : info;
                    this.host = "";
                    this.port = 25565;
                    if (split >= 0) {
                        final String address = info.substring(split + 1);
                        final int colon = address.lastIndexOf(':');
                        this.host = colon >= 0 ? address.substring(0, colon) : address;
                        if (colon >= 0) {
                            try {
                                this.port = Integer.parseInt(address.substring(colon + 1));
                            } catch (final NumberFormatException ignored) {
                            }
                        }
                    }
                    final ByteBuf out = ctx.alloc().buffer();
                    out.writeByte(0x02);
                    writeString(out, "-");
                    ctx.writeAndFlush(out);
                    this.phase = Phase.OLD_LOGIN;
                    return true;
                }
                final int protocol = this.cumulation.readUnsignedByte();
                final String name = readString(this.cumulation);
                final String address = readString(this.cumulation);
                final int targetPort = this.cumulation.readInt();
                this.version = LegacyVersions.byProtocolId(protocol);
                if (this.version == null) {
                    kick(ctx, "Unsupported client version (protocol " + protocol + "). Supported: " + LegacyPing.VERSION_NAME);
                    return true;
                }
                this.v16 = this.version.newerThanOrEqualTo(LegacyVersions.R1_6_1);
                this.username = name;
                this.host = address;
                this.port = targetPort;
                sendAuthRequest(ctx);
                this.phase = Phase.ENCRYPTION;
                return true;
            }
            case OLD_LOGIN -> {
                final int id = this.cumulation.readUnsignedByte();
                if (id != 0x01) {
                    kick(ctx, "Unexpected packet");
                    return true;
                }
                final int protocol = this.cumulation.readInt();
                final String name = readString(this.cumulation);
                if (protocol >= 28) {
                    readString(this.cumulation);
                    this.cumulation.skipBytes(11);
                } else if (protocol == 23) {
                    this.cumulation.skipBytes(8);
                    readString(this.cumulation);
                    this.cumulation.skipBytes(8);
                } else {
                    this.cumulation.skipBytes(16);
                }
                this.version = LegacyVersions.byProtocolId(protocol);
                if (this.version == null) {
                    kick(ctx, "Outdated client! Supported: " + LegacyPing.VERSION_NAME);
                    return true;
                }
                if (!name.isEmpty()) {
                    this.username = name;
                }
                this.phase = Phase.LOGIN;
                startServerLogin(ctx);
                return true;
            }
            case ENCRYPTION -> {
                final int id = this.cumulation.readUnsignedByte();
                if (id != 0xFC) {
                    kick(ctx, "Unexpected packet");
                    return true;
                }
                final byte[] secretEncrypted = readBytes(this.cumulation);
                final byte[] tokenEncrypted = readBytes(this.cumulation);
                final KeyPair pair = LegacyCrypto.keyPair();
                final byte[] secret;
                final byte[] token;
                try {
                    secret = LegacyCrypto.decryptRsa(pair.getPrivate(), secretEncrypted);
                    token = LegacyCrypto.decryptRsa(pair.getPrivate(), tokenEncrypted);
                } catch (final Exception e) {
                    kick(ctx, "Encryption failed");
                    return true;
                }
                if (!Arrays.equals(token, this.verifyToken)) {
                    kick(ctx, "Invalid verify token");
                    return true;
                }
                final ByteBuf response = ctx.alloc().buffer(5);
                response.writeByte(0xFC);
                response.writeShort(0);
                response.writeShort(0);
                ctx.writeAndFlush(response);
                this.encrypt = LegacyCrypto.aes(Cipher.ENCRYPT_MODE, secret);
                this.decrypt = LegacyCrypto.aes(Cipher.DECRYPT_MODE, secret);
                if (this.cumulation.isReadable()) {
                    final byte[] rest = new byte[this.cumulation.readableBytes()];
                    this.cumulation.readBytes(rest);
                    this.cumulation.clear();
                    final byte[] plain = this.decrypt.update(rest);
                    if (plain != null) {
                        this.cumulation.writeBytes(plain);
                    }
                    this.cumulation.markReaderIndex();
                }
                this.phase = Phase.AWAIT_SPAWN;
                return true;
            }
            case AWAIT_SPAWN -> {
                final int id = this.cumulation.getUnsignedByte(this.cumulation.readerIndex());
                if (id == 0xCD) {
                    this.cumulation.skipBytes(2);
                    this.phase = Phase.LOGIN;
                    startServerLogin(ctx);
                    return true;
                }
                final ByteBuf frame = readFrame(ctx);
                if (frame == null) {
                    return true;
                }
                this.pending.add(frame);
                return true;
            }
            case LOGIN -> {
                final ByteBuf frame = readFrame(ctx);
                if (frame != null) {
                    this.pending.add(frame);
                }
                return true;
            }
            case PLAY -> {
                final ByteBuf frame = readFrame(ctx);
                if (frame != null) {
                    ctx.fireChannelRead(frame);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private ByteBuf readFrame(final ChannelHandlerContext ctx) {
        final int id = this.cumulation.readUnsignedByte();
        final LegacyServerbound type = LegacyServerbound.byId(id);
        if (type == null) {
            kick(ctx, "Bad packet id " + id);
            return null;
        }
        final int begin = this.cumulation.readerIndex();
        LegacyPacketReader.skip(type, this.version.getVersion(), this.cumulation);
        final int length = this.cumulation.readerIndex() - begin;
        if (type == LegacyServerbound.DISCONNECT) {
            this.phase = Phase.CLOSED;
            ctx.close();
            return null;
        }
        if (type == LegacyServerbound.KEEP_ALIVE && this.phase != Phase.PLAY) {
            return null;
        }
        final int idSize = varIntSize(id);
        final ByteBuf frame = ctx.alloc().buffer(length + idSize + 5);
        writeVarInt(frame, length + idSize);
        writeVarInt(frame, id);
        frame.writeBytes(this.cumulation, begin, length);
        return frame;
    }

    private void sendAuthRequest(final ChannelHandlerContext ctx) {
        this.verifyToken = new byte[4];
        new SecureRandom().nextBytes(this.verifyToken);
        final byte[] publicKey = LegacyCrypto.keyPair().getPublic().getEncoded();
        final ByteBuf out = ctx.alloc().buffer();
        out.writeByte(0xFD);
        writeString(out, "-");
        out.writeShort(publicKey.length);
        out.writeBytes(publicKey);
        out.writeShort(this.verifyToken.length);
        out.writeBytes(this.verifyToken);
        ctx.writeAndFlush(out);
    }

    private void startServerLogin(final ChannelHandlerContext ctx) {
        final UserConnection user = findUser(ctx);
        if (user == null) {
            kick(ctx, "ViaVersion is not active on this connection");
            return;
        }
        LegacyVersionProvider.install();
        user.put(new LegacyClient(this.version, this));
        final ByteBuf handshake = ctx.alloc().buffer();
        writeVarInt(handshake, 0x00);
        writeVarInt(handshake, 4);
        writeUtf(handshake, this.host == null ? "" : this.host);
        handshake.writeShort(this.port);
        writeVarInt(handshake, 2);
        ctx.fireChannelRead(frame(ctx, handshake));
        final ByteBuf hello = ctx.alloc().buffer();
        writeVarInt(hello, 0x00);
        writeUtf(hello, this.username);
        ctx.fireChannelRead(frame(ctx, hello));
    }

    public void onPlayStarted() {
        if (this.phase != Phase.LOGIN) {
            return;
        }
        this.phase = Phase.PLAY;
        this.ctx.channel().eventLoop().execute(() -> {
            ByteBuf frame;
            while ((frame = this.pending.poll()) != null) {
                this.ctx.fireChannelRead(frame);
            }
            try {
                process(this.ctx);
            } catch (final Exception e) {
                this.ctx.fireExceptionCaught(e);
            }
        });
    }

    private static ByteBuf frame(final ChannelHandlerContext ctx, final ByteBuf body) {
        final ByteBuf out = ctx.alloc().buffer(body.readableBytes() + 5);
        writeVarInt(out, body.readableBytes());
        out.writeBytes(body);
        body.release();
        return out;
    }

    private static UserConnection findUser(final ChannelHandlerContext ctx) {
        final ChannelHandler handler = ctx.pipeline().get(Via.getManager().getInjector().getDecoderName());
        if (handler instanceof ViaChannelHandler viaHandler) {
            return viaHandler.connection();
        }
        return null;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        if (!(msg instanceof ByteBuf in) || (this.phase != Phase.LOGIN && this.phase != Phase.PLAY)) {
            ctx.write(msg, promise);
            return;
        }
        final ByteBuf out = ctx.alloc().buffer(in.readableBytes());
        try {
            while (in.isReadable()) {
                final int length = readVarInt(in);
                final int start = in.readerIndex();
                final int id = readVarInt(in);
                final int payload = length - (in.readerIndex() - start);
                out.writeByte(id & 0xFF);
                out.writeBytes(in, payload);
            }
        } finally {
            in.release();
        }
        ctx.write(encryptOut(out), promise);
    }

    private ByteBuf encryptOut(final ByteBuf out) {
        if (this.encrypt == null) {
            return out;
        }
        final byte[] data = new byte[out.readableBytes()];
        out.readBytes(data);
        out.release();
        final byte[] encrypted = this.encrypt.update(data);
        return Unpooled.wrappedBuffer(encrypted == null ? new byte[0] : encrypted);
    }

    public void kick(final ChannelHandlerContext ctx, final String reason) {
        if (this.phase == Phase.CLOSED) {
            return;
        }
        this.phase = Phase.CLOSED;
        final ByteBuf out = ctx.alloc().buffer();
        out.writeByte(0xFF);
        writeString(out, reason.length() > 256 ? reason.substring(0, 256) : reason);
        ctx.writeAndFlush(encryptOut(out)).addListener(ChannelFutureListener.CLOSE);
    }

    static String readString(final ByteBuf buf) {
        final short length = buf.readShort();
        final char[] chars = new char[Math.max(0, length)];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = buf.readChar();
        }
        return new String(chars);
    }

    static void writeString(final ByteBuf buf, final String value) {
        buf.writeShort(value.length());
        for (int i = 0; i < value.length(); i++) {
            buf.writeChar(value.charAt(i));
        }
    }

    private static byte[] readBytes(final ByteBuf buf) {
        final short length = buf.readShort();
        final byte[] data = new byte[Math.max(0, length)];
        buf.readBytes(data);
        return data;
    }

    private static void writeUtf(final ByteBuf buf, final String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, data.length);
        buf.writeBytes(data);
    }

    static void writeVarInt(final ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    static int readVarInt(final ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte current;
        do {
            current = buf.readByte();
            value |= (current & 0x7F) << position;
            position += 7;
            if (position > 35) {
                throw new IllegalStateException("VarInt too big");
            }
        } while ((current & 0x80) != 0);
        return value;
    }

    static int varIntSize(final int value) {
        if ((value & 0xFFFFFF80) == 0) {
            return 1;
        }
        if ((value & 0xFFFFC000) == 0) {
            return 2;
        }
        if ((value & 0xFFE00000) == 0) {
            return 3;
        }
        if ((value & 0xF0000000) == 0) {
            return 4;
        }
        return 5;
    }

}

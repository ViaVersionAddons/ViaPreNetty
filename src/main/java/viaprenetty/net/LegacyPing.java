package viaprenetty.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import org.bukkit.Bukkit;
import viaprenetty.LegacyVersions;

import java.util.concurrent.TimeUnit;

public final class LegacyPing extends ChannelInboundHandlerAdapter {

    public static final String NAME = "viaprenetty-ping";
    public static final String VERSION_NAME = "1.4.7-1.6.4";

    private final ByteBuf buffer = Unpooled.buffer();
    private ScheduledFuture<?> timeout;
    private boolean done;

    public static boolean looksLikePing(final ByteBuf buf) {
        final int readable = buf.readableBytes();
        if (readable == 1) {
            return true;
        }
        final int second = buf.getUnsignedByte(buf.readerIndex() + 1);
        if (second != 0x01) {
            return false;
        }
        return readable == 2 || buf.getUnsignedByte(buf.readerIndex() + 2) == 0xFA;
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        this.buffer.release();
        if (this.timeout != null) {
            this.timeout.cancel(false);
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (!(msg instanceof ByteBuf in)) {
            return;
        }
        try {
            this.buffer.writeBytes(in);
        } finally {
            in.release();
        }
        if (this.done) {
            return;
        }
        final int readable = this.buffer.readableBytes();
        if (readable == 1) {
            schedule(ctx, -1);
            return;
        }
        if (readable == 2) {
            schedule(ctx, LegacyVersions.R1_5_2.getVersion());
            return;
        }
        final int protocol = parseProtocol();
        if (protocol == -2) {
            schedule(ctx, LegacyVersions.R1_6_4.getVersion());
            return;
        }
        respond(ctx, protocol < 0 ? LegacyVersions.R1_6_4.getVersion() : protocol);
    }

    private int parseProtocol() {
        final ByteBuf buf = this.buffer.duplicate();
        try {
            buf.skipBytes(3);
            LegacyConnection.readString(buf);
            buf.readShort();
            return buf.readUnsignedByte();
        } catch (final IndexOutOfBoundsException e) {
            return -2;
        }
    }

    private void schedule(final ChannelHandlerContext ctx, final int protocol) {
        if (this.timeout != null) {
            this.timeout.cancel(false);
        }
        this.timeout = ctx.executor().schedule(() -> respond(ctx, protocol), 150, TimeUnit.MILLISECONDS);
    }

    private void respond(final ChannelHandlerContext ctx, final int protocol) {
        if (this.done) {
            return;
        }
        this.done = true;
        if (this.timeout != null) {
            this.timeout.cancel(false);
        }
        String motd;
        int online;
        int max;
        try {
            motd = Bukkit.getMotd();
            online = Bukkit.getOnlinePlayers().size();
            max = Bukkit.getMaxPlayers();
        } catch (final Throwable e) {
            motd = "A Minecraft Server";
            online = 0;
            max = 20;
        }
        motd = motd.replace('\n', ' ').replace("\0", "");
        final String response;
        if (protocol == -1) {
            response = stripColors(motd).replace("§", "") + "§" + online + "§" + max;
        } else {
            final int shown = LegacyVersions.byProtocolId(protocol) != null ? protocol : LegacyVersions.R1_6_4.getVersion();
            response = "§1\0" + shown + "\0" + VERSION_NAME + "\0" + motd + "\0" + online + "\0" + max;
        }
        final ByteBuf out = ctx.alloc().buffer();
        out.writeByte(0xFF);
        LegacyConnection.writeString(out, response.length() > 256 ? response.substring(0, 256) : response);
        ctx.writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);
    }

    private static String stripColors(final String text) {
        return text.replaceAll("§.", "");
    }

}

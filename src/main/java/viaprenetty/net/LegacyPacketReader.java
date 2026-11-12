package viaprenetty.net;

import io.netty.buffer.ByteBuf;
import viaprenetty.api.LegacyServerbound;

public final class LegacyPacketReader {

    private LegacyPacketReader() {
    }

    public static void skip(final LegacyServerbound type, final int protocol, final ByteBuf buf) {
        final boolean v16 = protocol >= 73;
        switch (type) {
            case KEEP_ALIVE -> buf.skipBytes(4);
            case LOGIN -> {
                buf.skipBytes(4);
                skipString(buf);
                buf.skipBytes(5);
            }
            case CLIENT_PROTOCOL -> {
                buf.skipBytes(1);
                skipString(buf);
                skipString(buf);
                buf.skipBytes(4);
            }
            case CHAT, COMMAND_SUGGESTION, DISCONNECT -> skipString(buf);
            case INTERACT -> buf.skipBytes(9);
            case RESPAWN -> {
                if (protocol >= 28) {
                    buf.skipBytes(8);
                    skipString(buf);
                } else {
                    buf.skipBytes(13);
                    if (protocol >= 23) {
                        skipString(buf);
                    }
                }
            }
            case MOVE_PLAYER_STATUS_ONLY -> buf.skipBytes(1);
            case MOVE_PLAYER_POS -> buf.skipBytes(33);
            case MOVE_PLAYER_ROT -> buf.skipBytes(9);
            case MOVE_PLAYER_POS_ROT -> buf.skipBytes(41);
            case PLAYER_ACTION -> buf.skipBytes(11);
            case USE_ITEM_ON -> {
                buf.skipBytes(10);
                skipItem(buf, protocol);
                if (protocol >= 39) {
                    buf.skipBytes(3);
                }
            }
            case SET_CARRIED_ITEM -> buf.skipBytes(2);
            case SWING -> buf.skipBytes(5);
            case PLAYER_COMMAND -> buf.skipBytes(v16 ? 9 : 5);
            case PLAYER_INPUT -> buf.skipBytes(10);
            case CONTAINER_CLOSE -> buf.skipBytes(1);
            case CONTAINER_CLICK -> {
                buf.skipBytes(7);
                skipItem(buf, protocol);
            }
            case CONTAINER_ACK -> buf.skipBytes(4);
            case SET_CREATIVE_MODE_SLOT -> {
                buf.skipBytes(2);
                skipItem(buf, protocol);
            }
            case CONTAINER_BUTTON_CLICK -> buf.skipBytes(2);
            case SIGN_UPDATE -> {
                buf.skipBytes(10);
                skipString(buf);
                skipString(buf);
                skipString(buf);
                skipString(buf);
            }
            case PLAYER_ABILITIES -> buf.skipBytes(v16 ? 9 : protocol >= 39 ? 3 : 4);
            case CLIENT_INFORMATION -> {
                skipString(buf);
                buf.skipBytes(protocol >= 47 ? 4 : 3);
            }
            case CLIENT_COMMAND -> buf.skipBytes(1);
            case CUSTOM_PAYLOAD -> {
                skipString(buf);
                final short length = buf.readShort();
                if (length > 0) {
                    buf.skipBytes(length);
                }
            }
            case SHARED_KEY -> {
                skipBytes(buf);
                skipBytes(buf);
            }
            case SERVER_PING -> {
                if (v16) {
                    buf.skipBytes(2);
                    skipString(buf);
                    buf.skipBytes(2);
                    final byte pingProtocol = buf.readByte();
                    if (pingProtocol >= 73) {
                        skipString(buf);
                        buf.skipBytes(4);
                    }
                } else {
                    buf.skipBytes(1);
                }
            }
        }
    }

    private static void skipString(final ByteBuf buf) {
        final short length = buf.readShort();
        buf.skipBytes(Math.max(0, length) * 2);
    }

    private static void skipBytes(final ByteBuf buf) {
        final short length = buf.readShort();
        buf.skipBytes(Math.max(0, length));
    }

    private static void skipItem(final ByteBuf buf, final int protocol) {
        final short id = buf.readShort();
        if (id >= 0) {
            buf.skipBytes(3);
            if (protocol < 39 && !viaprenetty.api.LegacyTypes.hasNbt12(id)) {
                return;
            }
            final short tagLength = buf.readShort();
            if (tagLength > 0) {
                buf.skipBytes(tagLength);
            }
        }
    }

}

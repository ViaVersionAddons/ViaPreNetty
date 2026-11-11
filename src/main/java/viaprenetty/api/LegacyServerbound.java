package viaprenetty.api;

import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;

public enum LegacyServerbound implements ServerboundPacketType {

    KEEP_ALIVE(0),
    LOGIN(1),
    CLIENT_PROTOCOL(2),
    CHAT(3),
    INTERACT(7),
    RESPAWN(9),
    MOVE_PLAYER_STATUS_ONLY(10),
    MOVE_PLAYER_POS(11),
    MOVE_PLAYER_ROT(12),
    MOVE_PLAYER_POS_ROT(13),
    PLAYER_ACTION(14),
    USE_ITEM_ON(15),
    SET_CARRIED_ITEM(16),
    SWING(18),
    PLAYER_COMMAND(19),
    PLAYER_INPUT(27),
    CONTAINER_CLOSE(101),
    CONTAINER_CLICK(102),
    CONTAINER_ACK(106),
    SET_CREATIVE_MODE_SLOT(107),
    CONTAINER_BUTTON_CLICK(108),
    SIGN_UPDATE(130),
    PLAYER_ABILITIES(202),
    COMMAND_SUGGESTION(203),
    CLIENT_INFORMATION(204),
    CLIENT_COMMAND(205),
    CUSTOM_PAYLOAD(250),
    SHARED_KEY(252),
    SERVER_PING(254),
    DISCONNECT(255);

    private static final LegacyServerbound[] BY_ID = new LegacyServerbound[256];

    static {
        for (LegacyServerbound packet : values()) {
            BY_ID[packet.id] = packet;
        }
    }

    private final int id;

    LegacyServerbound(final int id) {
        this.id = id;
    }

    public static LegacyServerbound byId(final int id) {
        return id >= 0 && id < 256 ? BY_ID[id] : null;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return name();
    }

}

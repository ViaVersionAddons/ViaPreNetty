package viaprenetty.api;

import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;

public enum LegacyClientbound implements ClientboundPacketType {

    KEEP_ALIVE(0),
    LOGIN(1),
    CHAT(3),
    SET_TIME(4),
    SET_EQUIPPED_ITEM(5),
    SET_DEFAULT_SPAWN_POSITION(6),
    SET_HEALTH(8),
    RESPAWN(9),
    MOVE_PLAYER_STATUS_ONLY(10),
    MOVE_PLAYER_POS(11),
    MOVE_PLAYER_ROT(12),
    PLAYER_POSITION(13),
    SET_CARRIED_ITEM(16),
    PLAYER_SLEEP(17),
    ANIMATE(18),
    ADD_PLAYER(20),
    SPAWN_ITEM(21),
    TAKE_ITEM_ENTITY(22),
    ADD_ENTITY(23),
    ADD_MOB(24),
    ADD_PAINTING(25),
    ADD_EXPERIENCE_ORB(26),
    SET_ENTITY_MOTION(28),
    REMOVE_ENTITIES(29),
    MOVE_ENTITY(30),
    MOVE_ENTITY_POS(31),
    MOVE_ENTITY_ROT(32),
    MOVE_ENTITY_POS_ROT(33),
    TELEPORT_ENTITY(34),
    ROTATE_HEAD(35),
    ENTITY_EVENT(38),
    SET_ENTITY_LINK(39),
    SET_ENTITY_DATA(40),
    UPDATE_MOB_EFFECT(41),
    REMOVE_MOB_EFFECT(42),
    SET_EXPERIENCE(43),
    UPDATE_ATTRIBUTES(44),
    PRE_CHUNK(50),
    LEVEL_CHUNK(51),
    CHUNK_BLOCKS_UPDATE(52),
    BLOCK_UPDATE(53),
    BLOCK_EVENT(54),
    BLOCK_DESTRUCTION(55),
    MAP_BULK_CHUNK(56),
    EXPLODE(60),
    LEVEL_EVENT(61),
    CUSTOM_SOUND(62),
    LEVEL_PARTICLES(63),
    GAME_EVENT(70),
    ADD_GLOBAL_ENTITY(71),
    OPEN_SCREEN(100),
    CONTAINER_CLOSE(101),
    CONTAINER_SET_SLOT(103),
    CONTAINER_SET_CONTENT(104),
    CONTAINER_SET_DATA(105),
    CONTAINER_ACK(106),
    SET_CREATIVE_MODE_SLOT(107),
    UPDATE_SIGN(130),
    MAP_ITEM_DATA(131),
    BLOCK_ENTITY_DATA(132),
    OPEN_SIGN_EDITOR(133),
    AWARD_STATS(200),
    PLAYER_INFO(201),
    PLAYER_ABILITIES(202),
    COMMAND_SUGGESTIONS(203),
    SET_OBJECTIVE(206),
    SET_SCORE(207),
    SET_DISPLAY_OBJECTIVE(208),
    SET_PLAYER_TEAM(209),
    CUSTOM_PAYLOAD(250),
    SHARED_KEY(252),
    SERVER_AUTH_DATA(253),
    DISCONNECT(255);

    private final int id;

    LegacyClientbound(final int id) {
        this.id = id;
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

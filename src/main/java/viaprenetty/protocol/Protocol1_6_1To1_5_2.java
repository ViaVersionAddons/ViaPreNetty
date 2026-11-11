package viaprenetty.protocol;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyText;
import viaprenetty.api.LegacyTypes;
import viaprenetty.api.LegacyTypes.LegacyEntityDataTypes;
import viaprenetty.data.LegacyMappings;
import viaprenetty.data.LegacySoundMappings;
import viaprenetty.storage.EntityTracker1_5;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Protocol1_6_1To1_5_2 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private static final int BOAT = EntityTracker1_5.OBJECT_OFFSET + 1;
    private static final int MINECART = EntityTracker1_5.OBJECT_OFFSET + 10;
    private static final int ENDER_DRAGON = 63;
    private static final int WITHER = 64;
    private static final int WOLF = 95;
    private static final int COW = 92;
    private static final int HORSE = 100;
    private static final int REPLACED_HORSE = 10000;

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_5, LegacyMappings.ITEMS_1_5);

    public Protocol1_6_1To1_5_2() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, true, true);

        registerClientbound(LegacyClientbound.LOGIN, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            final EntityTracker1_5 tracker = wrapper.user().get(EntityTracker1_5.class);
            tracker.clear();
            tracker.setPlayerId(entityId);
        });
        registerClientbound(LegacyClientbound.RESPAWN, wrapper -> wrapper.user().get(EntityTracker1_5.class).clear());
        registerClientbound(LegacyClientbound.CHAT, wrapper -> {
            final String json = wrapper.read(LegacyTypes.STRING);
            final List<String> lines = LegacyText.splitLegacy(LegacyText.json16ToLegacy(json), 119);
            for (int i = 0; i < lines.size() - 1; i++) {
                final PacketWrapper extra = PacketWrapper.create(LegacyClientbound.CHAT, wrapper.user());
                extra.write(LegacyTypes.STRING, lines.get(i));
                extra.send(Protocol1_6_1To1_5_2.class);
            }
            wrapper.write(LegacyTypes.STRING, lines.get(lines.size() - 1));
        });
        registerClientbound(LegacyClientbound.DISCONNECT, wrapper -> {
            final String reason = wrapper.read(LegacyTypes.STRING);
            wrapper.write(LegacyTypes.STRING, reason);
        });
        registerClientbound(LegacyClientbound.SET_HEALTH, wrapper -> {
            final float health = wrapper.read(Types.FLOAT);
            wrapper.write(Types.SHORT, (short) Math.ceil(health));
        });
        registerClientbound(LegacyClientbound.ADD_PLAYER, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            wrapper.passthrough(LegacyTypes.STRING);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            final short item = wrapper.read(Types.SHORT);
            wrapper.write(Types.SHORT, (short) this.helper.itemId(item));
            wrapper.user().get(EntityTracker1_5.class).track(entityId, EntityTracker1_5.PLAYER);
            final List<EntityData> data = wrapper.read(LegacyTypes.ENTITY_DATA_LIST);
            wrapper.write(LegacyTypes.ENTITY_DATA_LIST, LegacyTypes.nonEmpty(transform(EntityTracker1_5.PLAYER, data)));
        });
        registerClientbound(LegacyClientbound.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            final byte type = wrapper.passthrough(Types.BYTE);
            if (type == 77) {
                wrapper.cancel();
                return;
            }
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            final int data = wrapper.read(Types.INT);
            wrapper.write(Types.INT, type == 70 ? this.helper.fallingBlockData(data) : data);
            wrapper.user().get(EntityTracker1_5.class).track(entityId, EntityTracker1_5.OBJECT_OFFSET + type);
        });
        registerClientbound(LegacyClientbound.ADD_MOB, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            short type = wrapper.read(Types.UNSIGNED_BYTE);
            int trackedType = type;
            if (type == HORSE) {
                type = COW;
                trackedType = REPLACED_HORSE;
            }
            wrapper.write(Types.UNSIGNED_BYTE, type);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.SHORT);
            wrapper.user().get(EntityTracker1_5.class).track(entityId, trackedType);
            final List<EntityData> data = wrapper.read(LegacyTypes.ENTITY_DATA_LIST);
            wrapper.write(LegacyTypes.ENTITY_DATA_LIST, LegacyTypes.nonEmpty(transform(trackedType, data)));
        });
        registerClientbound(LegacyClientbound.ADD_PAINTING, wrapper -> wrapper.user().get(EntityTracker1_5.class).track(wrapper.passthrough(Types.INT), EntityTracker1_5.OBJECT_OFFSET + 255));
        registerClientbound(LegacyClientbound.ADD_EXPERIENCE_ORB, wrapper -> wrapper.user().get(EntityTracker1_5.class).track(wrapper.passthrough(Types.INT), EntityTracker1_5.OBJECT_OFFSET + 254));
        registerClientbound(LegacyClientbound.ADD_GLOBAL_ENTITY, wrapper -> wrapper.user().get(EntityTracker1_5.class).track(wrapper.passthrough(Types.INT), EntityTracker1_5.OBJECT_OFFSET + 253));
        registerClientbound(LegacyClientbound.REMOVE_ENTITIES, wrapper -> {
            final EntityTracker1_5 tracker = wrapper.user().get(EntityTracker1_5.class);
            final byte count = wrapper.passthrough(Types.BYTE);
            for (int i = 0; i < (count & 0xFF); i++) {
                tracker.remove(wrapper.passthrough(Types.INT));
            }
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_LINK, wrapper -> {
            final int rider = wrapper.passthrough(Types.INT);
            final int vehicle = wrapper.passthrough(Types.INT);
            final short leash = wrapper.read(Types.UNSIGNED_BYTE);
            if (leash != 0) {
                wrapper.cancel();
                return;
            }
            final EntityTracker1_5 tracker = wrapper.user().get(EntityTracker1_5.class);
            if (rider == tracker.playerId()) {
                tracker.setVehicleId(vehicle);
            }
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            final Integer type = wrapper.user().get(EntityTracker1_5.class).type(entityId);
            final List<EntityData> data = wrapper.read(LegacyTypes.ENTITY_DATA_LIST);
            if (type == null) {
                wrapper.cancel();
                return;
            }
            final List<EntityData> transformed = transform(type, data);
            if (transformed.isEmpty()) {
                wrapper.cancel();
                return;
            }
            wrapper.write(LegacyTypes.ENTITY_DATA_LIST, transformed);
        });
        registerClientbound(LegacyClientbound.CUSTOM_SOUND, wrapper -> {
            final String sound = wrapper.read(LegacyTypes.STRING);
            final String mapped = LegacySoundMappings.to1_5(sound);
            if (mapped == null || mapped.isEmpty()) {
                wrapper.cancel();
                return;
            }
            wrapper.write(LegacyTypes.STRING, mapped);
        });
        registerClientbound(LegacyClientbound.LEVEL_PARTICLES, wrapper -> wrapper.write(LegacyTypes.STRING, this.helper.particle(wrapper.read(LegacyTypes.STRING))));
        registerClientbound(LegacyClientbound.PLAYER_ABILITIES, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.write(Types.BYTE, (byte) (wrapper.read(Types.FLOAT) * 255F));
            wrapper.write(Types.BYTE, (byte) (wrapper.read(Types.FLOAT) * 255F));
        });
        registerClientbound(LegacyClientbound.AWARD_STATS, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.write(Types.BYTE, (byte) (int) wrapper.read(Types.INT));
        });
        cancelClientbound(LegacyClientbound.UPDATE_ATTRIBUTES);
        registerClientbound(LegacyClientbound.UPDATE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte effect = wrapper.passthrough(Types.BYTE);
            if ((effect & 0xFF) > 20) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.REMOVE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte effect = wrapper.passthrough(Types.BYTE);
            if ((effect & 0xFF) > 20) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.OPEN_SCREEN, wrapper -> {
            final short windowId = wrapper.passthrough(Types.UNSIGNED_BYTE);
            final short type = wrapper.passthrough(Types.UNSIGNED_BYTE);
            if (type == 11) {
                wrapper.cancel();
                closeOnServer(wrapper.user(), windowId);
            }
        });
        registerClientbound(LegacyClientbound.CUSTOM_PAYLOAD, wrapper -> this.helper.customPayloadToClient(wrapper, (channel, data) -> {
            if (channel.equals("MC|RPack")) {
                final String url = new String(data, StandardCharsets.UTF_8);
                return new Object[]{"MC|TPack", (url + "\0" + "16").getBytes(StandardCharsets.UTF_8)};
            }
            return new Object[]{channel, data};
        }));

        registerServerbound(LegacyServerbound.PLAYER_COMMAND, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.write(Types.INT, 0);
        });
        registerServerbound(LegacyServerbound.PLAYER_ABILITIES, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.write(Types.FLOAT, (wrapper.read(Types.BYTE) & 0xFF) / 255F);
            wrapper.write(Types.FLOAT, (wrapper.read(Types.BYTE) & 0xFF) / 255F);
        });
        registerServerbound(LegacyServerbound.INTERACT, wrapper -> {
            final int player = wrapper.passthrough(Types.INT);
            final int target = wrapper.passthrough(Types.INT);
            final byte mode = wrapper.passthrough(Types.BYTE);
            final EntityTracker1_5 tracker = wrapper.user().get(EntityTracker1_5.class);
            if (mode == 0 && target == tracker.vehicleId() && player == tracker.playerId()) {
                wrapper.clearPacket();
                wrapper.setPacketType(LegacyServerbound.PLAYER_INPUT);
                wrapper.write(Types.FLOAT, 0F);
                wrapper.write(Types.FLOAT, 0F);
                wrapper.write(Types.BOOLEAN, false);
                wrapper.write(Types.BOOLEAN, true);
                tracker.setVehicleId(-1);
            }
        });
        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, this.helper::customPayloadToServer);
    }

    private void closeOnServer(final UserConnection user, final short windowId) {
        try {
            final PacketWrapper close = PacketWrapper.create(LegacyServerbound.CONTAINER_CLOSE, user);
            close.write(Types.BYTE, (byte) windowId);
            close.sendToServer(Protocol1_6_1To1_5_2.class);
        } catch (final Exception ignored) {
        }
    }

    private List<EntityData> transform(final int type, final List<EntityData> list) {
        this.helper.entityDataItems(list);
        final boolean living = type < EntityTracker1_5.OBJECT_OFFSET || type == REPLACED_HORSE;
        final List<EntityData> out = new ArrayList<>(list.size());
        for (EntityData data : list) {
            final int index = data.id();
            if (living) {
                if (index == 6) {
                    if (type == WITHER || type == ENDER_DRAGON) {
                        out.add(new EntityData(16, LegacyEntityDataTypes.INT, (int) asFloat(data)));
                    }
                    continue;
                }
                if (index == 7) {
                    out.add(new EntityData(8, LegacyEntityDataTypes.INT, data.getValue()));
                    continue;
                }
                if (index == 8) {
                    out.add(new EntityData(9, LegacyEntityDataTypes.BYTE, data.getValue()));
                    continue;
                }
                if (index == 9) {
                    out.add(new EntityData(10, LegacyEntityDataTypes.BYTE, data.getValue()));
                    continue;
                }
                if (index == 10) {
                    out.add(new EntityData(5, LegacyEntityDataTypes.STRING, data.getValue()));
                    continue;
                }
                if (index == 11) {
                    out.add(new EntityData(6, LegacyEntityDataTypes.BYTE, data.getValue()));
                    continue;
                }
            }
            if (type == REPLACED_HORSE && index > 12) {
                continue;
            }
            if (type == EntityTracker1_5.PLAYER && index == 17) {
                out.add(new EntityData(17, LegacyEntityDataTypes.BYTE, (byte) asFloat(data)));
                continue;
            }
            if ((type == BOAT || type == MINECART) && index == 19) {
                out.add(new EntityData(19, LegacyEntityDataTypes.INT, (int) asFloat(data)));
                continue;
            }
            if (type == WOLF && index == 18) {
                out.add(new EntityData(18, LegacyEntityDataTypes.INT, (int) asFloat(data)));
                continue;
            }
            out.add(data);
        }
        return out;
    }

    private static float asFloat(final EntityData data) {
        return data.getValue() instanceof Number number ? number.floatValue() : 0F;
    }

    @Override
    public void init(final UserConnection connection) {
        connection.put(new EntityTracker1_5());
    }

}

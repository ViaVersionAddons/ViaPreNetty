package viaprenetty.protocol;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viarewind.api.type.RewindTypes;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;
import viaprenetty.storage.ItemEntityTracker;

import java.util.List;

public final class Protocol1_4_6To1_4_4 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_4_4, LegacyMappings.ITEMS_1_4_4).withoutBulkSkyFlag();

    public Protocol1_4_6To1_4_4() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, true, true);

        registerClientbound(LegacyClientbound.LOGIN, wrapper -> wrapper.user().get(ItemEntityTracker.class).clear());
        registerClientbound(LegacyClientbound.RESPAWN, wrapper -> wrapper.user().get(ItemEntityTracker.class).clear());
        registerClientbound(LegacyClientbound.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            final byte type = wrapper.passthrough(Types.BYTE);
            if (type == 76) {
                wrapper.cancel();
                return;
            }
            final int x = wrapper.passthrough(Types.INT);
            final int y = wrapper.passthrough(Types.INT);
            final int z = wrapper.passthrough(Types.INT);
            wrapper.read(Types.BYTE);
            wrapper.read(Types.BYTE);
            if (type == 2) {
                final int data = wrapper.read(Types.INT);
                int vx = 0;
                int vy = 0;
                int vz = 0;
                if (data > 0) {
                    vx = wrapper.read(Types.SHORT);
                    vy = wrapper.read(Types.SHORT);
                    vz = wrapper.read(Types.SHORT);
                }
                wrapper.user().get(ItemEntityTracker.class).pending(entityId, new int[]{x, y, z, motion(vx), motion(vy), motion(vz)});
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT);
            final List<EntityData> list = wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST);
            this.helper.entityDataItems(list);
            final ItemEntityTracker tracker = wrapper.user().get(ItemEntityTracker.class);
            if (!tracker.isItem(entityId)) {
                return;
            }
            Item item = null;
            for (EntityData data : list) {
                if (data.id() == 10 && data.dataType().typeId() == LegacyTypes.LegacyEntityDataTypes.ITEM.typeId()) {
                    item = data.value();
                }
            }
            list.removeIf(data -> data.id() == 10);
            final int[] spawn = item != null ? tracker.takePending(entityId) : null;
            if (spawn != null) {
                final PacketWrapper spawnItem = PacketWrapper.create(LegacyClientbound.SPAWN_ITEM, wrapper.user());
                spawnItem.write(Types.INT, entityId);
                spawnItem.write(RewindTypes.COMPRESSED_NBT_ITEM, item);
                spawnItem.write(Types.INT, spawn[0]);
                spawnItem.write(Types.INT, spawn[1]);
                spawnItem.write(Types.INT, spawn[2]);
                spawnItem.write(Types.BYTE, (byte) spawn[3]);
                spawnItem.write(Types.BYTE, (byte) spawn[4]);
                spawnItem.write(Types.BYTE, (byte) spawn[5]);
                spawnItem.send(Protocol1_4_6To1_4_4.class);
            }
            if (list.isEmpty()) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.REMOVE_ENTITIES, wrapper -> {
            final ItemEntityTracker tracker = wrapper.user().get(ItemEntityTracker.class);
            final byte count = wrapper.passthrough(Types.BYTE);
            for (int i = 0; i < (count & 0xFF); i++) {
                tracker.remove(wrapper.passthrough(Types.INT));
            }
        });
        registerClientbound(LegacyClientbound.ADD_PLAYER, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(LegacyTypes.STRING);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            final short item = wrapper.read(Types.SHORT);
            wrapper.write(Types.SHORT, (short) this.helper.itemId(item));
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST));
        });
        registerClientbound(LegacyClientbound.ADD_MOB, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.SHORT);
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST));
        });
        registerClientbound(LegacyClientbound.CUSTOM_PAYLOAD, wrapper -> this.helper.customPayloadToClient(wrapper, null));

        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, this.helper::customPayloadToServer);
    }

    private static int motion(final int velocity) {
        return Math.max(-128, Math.min(127, Math.round(velocity / 8000F * 128F)));
    }

    @Override
    public void init(final UserConnection connection) {
        connection.put(new ItemEntityTracker());
    }

}

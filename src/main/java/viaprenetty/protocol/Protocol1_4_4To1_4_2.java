package viaprenetty.protocol;

import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;

public final class Protocol1_4_4To1_4_2 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_4_2, LegacyMappings.ITEMS_1_4_2).withoutBulkSkyFlag();

    public Protocol1_4_4To1_4_2() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, true, true);

        registerClientbound(LegacyClientbound.SPAWN_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            this.helper.items().toClient(wrapper.passthrough(com.viaversion.viarewind.api.type.RewindTypes.COMPRESSED_NBT_ITEM));
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
            entityData(wrapper);
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
            entityData(wrapper);
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            entityData(wrapper);
        });
        registerClientbound(LegacyClientbound.MAP_ITEM_DATA, wrapper -> {
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.SHORT);
            final short length = wrapper.read(Types.SHORT);
            final byte[] data = new byte[Math.max(0, length)];
            for (int i = 0; i < data.length; i++) {
                data[i] = wrapper.read(Types.BYTE);
            }
            if (data.length > 255) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.UNSIGNED_BYTE, (short) data.length);
            for (byte b : data) {
                wrapper.write(Types.BYTE, b);
            }
        });
        registerClientbound(LegacyClientbound.CUSTOM_PAYLOAD, wrapper -> this.helper.customPayloadToClient(wrapper, null));

        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, this.helper::customPayloadToServer);
    }

    private void entityData(final com.viaversion.viaversion.api.protocol.packet.PacketWrapper wrapper) {
        final java.util.List<com.viaversion.viaversion.api.minecraft.entitydata.EntityData> list = wrapper.read(LegacyTypes.ENTITY_DATA_LIST);
        this.helper.entityDataItems(list);
        wrapper.write(LegacyTypes.ENTITY_DATA_LIST_NBTLESS, list);
    }

}

package viaprenetty.protocol;

import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;

public final class Protocol1_1To1_0 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_0, LegacyMappings.ITEMS_1_0)
        .withoutChunks()
        .withClientItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2)
        .withServerItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2);

    public Protocol1_1To1_0() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, false, true);
        registerClientbound(LegacyClientbound.LEVEL_CHUNK, LegacyClientbound.LEVEL_CHUNK, wrapper -> {
        }, true);
        registerClientbound(LegacyClientbound.CHUNK_BLOCKS_UPDATE, LegacyClientbound.CHUNK_BLOCKS_UPDATE, wrapper -> {
        }, true);
        registerClientbound(LegacyClientbound.BLOCK_UPDATE, LegacyClientbound.BLOCK_UPDATE, wrapper -> {
        }, true);
        registerClientbound(LegacyClientbound.BLOCK_EVENT, LegacyClientbound.BLOCK_EVENT, wrapper -> {
        }, true);
        registerClientbound(LegacyClientbound.LEVEL_EVENT, LegacyClientbound.LEVEL_EVENT, wrapper -> {
        }, true);
        registerClientbound(LegacyClientbound.LOGIN, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(LegacyTypes.STRING);
            wrapper.passthrough(Types.LONG);
            wrapper.read(LegacyTypes.STRING);
        });
        registerClientbound(LegacyClientbound.RESPAWN, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.LONG);
            wrapper.read(LegacyTypes.STRING);
        });
        registerClientbound(LegacyClientbound.SPAWN_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.write(LegacyTypes.NBTLESS_ITEM, this.helper.items().toClient(wrapper.read(LegacyTypes.NBTLESS_ITEM)));
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST_NBTLESS));
        });
        registerClientbound(LegacyClientbound.SET_EQUIPPED_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            final short id = wrapper.read(Types.SHORT);
            final short data = wrapper.read(Types.SHORT);
            if (id < 0) {
                wrapper.write(Types.SHORT, id);
                wrapper.write(Types.SHORT, data);
                return;
            }
            final com.viaversion.viaversion.api.minecraft.item.Item item = this.helper.items().toClient(new com.viaversion.viaversion.api.minecraft.item.DataItem(id, (byte) 1, data, null));
            wrapper.write(Types.SHORT, (short) item.identifier());
            wrapper.write(Types.SHORT, item.data());
        });
        cancelClientbound(LegacyClientbound.CUSTOM_PAYLOAD);

        registerServerbound(LegacyServerbound.RESPAWN, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.LONG);
            wrapper.write(LegacyTypes.STRING, "");
        });
    }

}

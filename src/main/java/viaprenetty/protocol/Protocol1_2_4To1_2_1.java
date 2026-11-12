package viaprenetty.protocol;

import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyChunkRemapper;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;

public final class Protocol1_2_4To1_2_1 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_2_1, LegacyMappings.ITEMS_1_2_1)
        .withoutChunks()
        .withClientItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2)
        .withServerItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2);

    public Protocol1_2_4To1_2_1() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, false, true);

        registerClientbound(LegacyClientbound.LEVEL_CHUNK, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BOOLEAN);
            final int primary = wrapper.passthrough(Types.UNSIGNED_SHORT);
            wrapper.passthrough(Types.UNSIGNED_SHORT);
            final int length = wrapper.read(Types.INT);
            final int unused = wrapper.read(Types.INT);
            final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
            final byte[] compressed = java.util.Arrays.copyOf(rest, Math.min(length, rest.length));
            final byte[] remapped = LegacyChunkRemapper.remapSingle(compressed, primary, this.helper.blocks());
            final byte[] out = remapped != null ? remapped : compressed;
            wrapper.write(Types.INT, out.length);
            wrapper.write(Types.INT, unused);
            wrapper.write(Types.REMAINING_BYTES, out);
        });
        registerClientbound(LegacyClientbound.BLOCK_UPDATE, LegacyClientbound.BLOCK_UPDATE, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.INT);
            final int id = wrapper.read(Types.UNSIGNED_BYTE);
            final int meta = wrapper.read(Types.BYTE) & 15;
            final int mapped = this.helper.blocks().remap(id, meta);
            wrapper.write(Types.UNSIGNED_BYTE, (short) ((mapped >> 4) & 0xFF));
            wrapper.write(Types.BYTE, (byte) (mapped & 15));
        }, true);
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
        registerClientbound(LegacyClientbound.SPAWN_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.write(LegacyTypes.NBTLESS_ITEM, this.helper.items().toClient(wrapper.read(LegacyTypes.NBTLESS_ITEM)));
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST_NBTLESS));
        });
        registerClientbound(LegacyClientbound.BLOCK_EVENT, LegacyClientbound.BLOCK_EVENT, wrapper -> {
        }, true);
        cancelClientbound(LegacyClientbound.PLAYER_ABILITIES);
    }

}

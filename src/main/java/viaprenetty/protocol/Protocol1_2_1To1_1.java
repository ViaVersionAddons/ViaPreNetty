package viaprenetty.protocol;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyChunk11;
import viaprenetty.api.LegacyChunkRemapper;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;
import viaprenetty.storage.ClientWorld12;

public final class Protocol1_2_1To1_1 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_1, LegacyMappings.ITEMS_1_1)
        .withoutChunks()
        .withClientItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2)
        .withServerItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2);

    public Protocol1_2_1To1_1() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, false, true);

        registerClientbound(LegacyClientbound.LOGIN, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(LegacyTypes.STRING);
            final String levelType = wrapper.read(LegacyTypes.STRING);
            final int gameMode = wrapper.read(Types.INT);
            final int dimension = wrapper.read(Types.INT);
            final byte difficulty = wrapper.read(Types.BYTE);
            wrapper.read(Types.BYTE);
            final byte maxPlayers = wrapper.read(Types.BYTE);
            wrapper.write(Types.LONG, 0L);
            wrapper.write(LegacyTypes.STRING, levelType.equalsIgnoreCase("flat") ? "flat" : "default");
            wrapper.write(Types.INT, gameMode);
            wrapper.write(Types.BYTE, (byte) dimension);
            wrapper.write(Types.BYTE, difficulty);
            wrapper.write(Types.BYTE, (byte) -128);
            wrapper.write(Types.BYTE, maxPlayers);
        });
        registerClientbound(LegacyClientbound.RESPAWN, wrapper -> {
            final int dimension = wrapper.read(Types.INT);
            final byte difficulty = wrapper.read(Types.BYTE);
            final byte gameMode = wrapper.read(Types.BYTE);
            wrapper.read(Types.SHORT);
            final String levelType = wrapper.read(LegacyTypes.STRING);
            wrapper.write(Types.BYTE, (byte) dimension);
            wrapper.write(Types.BYTE, difficulty);
            wrapper.write(Types.BYTE, gameMode);
            wrapper.write(Types.SHORT, (short) 128);
            wrapper.write(Types.LONG, 0L);
            wrapper.write(LegacyTypes.STRING, levelType.equalsIgnoreCase("flat") ? "flat" : "default");
        });
        registerClientbound(LegacyClientbound.ADD_MOB, wrapper -> {
            wrapper.passthrough(Types.INT);
            final short type = wrapper.passthrough(Types.UNSIGNED_BYTE);
            if (type == 98 || type == 99) {
                wrapper.cancel();
                return;
            }
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.read(Types.BYTE);
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST_NBTLESS));
        });
        registerClientbound(LegacyClientbound.ADD_ENTITY, wrapper -> {
            wrapper.passthrough(Types.INT);
            if (wrapper.passthrough(Types.BYTE) == 75) {
                wrapper.cancel();
            }
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
        cancelClientbound(LegacyClientbound.ROTATE_HEAD);
        cancelClientbound(LegacyClientbound.BLOCK_ENTITY_DATA);
        registerClientbound(LegacyClientbound.LEVEL_CHUNK, wrapper -> {
            final int chunkX = wrapper.read(Types.INT);
            final int chunkZ = wrapper.read(Types.INT);
            final boolean ground = wrapper.read(Types.BOOLEAN);
            final int primary = wrapper.read(Types.UNSIGNED_SHORT);
            wrapper.read(Types.UNSIGNED_SHORT);
            final int length = wrapper.read(Types.INT);
            wrapper.read(Types.INT);
            final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
            final byte[] raw = LegacyChunkRemapper.inflate(java.util.Arrays.copyOf(rest, Math.min(length, rest.length)));
            if (raw == null || raw.length < Integer.bitCount(primary) * 10240) {
                wrapper.cancel();
                return;
            }
            final int sections = Integer.bitCount(primary);
            LegacyChunkRemapper.remapSections(raw, 0, sections, this.helper.blocks());
            if (ground) {
                final boolean sky = wrapper.user().get(ClientWorld12.class).dimension() == 0;
                writeRegion(wrapper, chunkX << 4, 0, chunkZ << 4, 16, 128, LegacyChunk11.column(raw, primary, sky));
                return;
            }
            wrapper.cancel();
            for (int s = 0; s < 8; s++) {
                if ((primary & (1 << s)) == 0) {
                    continue;
                }
                final PacketWrapper region = PacketWrapper.create(LegacyClientbound.LEVEL_CHUNK, wrapper.user());
                writeRegion(region, chunkX << 4, s << 4, chunkZ << 4, 16, 16, LegacyChunk11.section(raw, primary, s));
                region.send(Protocol1_2_1To1_1.class);
            }
        });
        registerClientbound(LegacyClientbound.CHUNK_BLOCKS_UPDATE, LegacyClientbound.CHUNK_BLOCKS_UPDATE, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            final short count = wrapper.read(Types.SHORT);
            wrapper.read(Types.INT);
            final short[] positions = new short[count];
            final byte[] ids = new byte[count];
            final byte[] metas = new byte[count];
            int kept = 0;
            for (int i = 0; i < count; i++) {
                final int record = wrapper.read(Types.INT);
                final int y = (record >> 16) & 0xFF;
                if (y > 127) {
                    continue;
                }
                final int mapped = this.helper.blocks().remap((record >> 4) & 4095, record & 15);
                positions[kept] = (short) ((((record >> 28) & 15) << 12) | (((record >> 24) & 15) << 8) | y);
                ids[kept] = (byte) (mapped >> 4);
                metas[kept] = (byte) (mapped & 15);
                kept++;
            }
            if (kept == 0) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.SHORT, (short) kept);
            for (int i = 0; i < kept; i++) {
                wrapper.write(Types.SHORT, positions[i]);
            }
            for (int i = 0; i < kept; i++) {
                wrapper.write(Types.BYTE, ids[i]);
            }
            for (int i = 0; i < kept; i++) {
                wrapper.write(Types.BYTE, metas[i]);
            }
        }, true);
        registerClientbound(LegacyClientbound.BLOCK_UPDATE, LegacyClientbound.BLOCK_UPDATE, wrapper -> {
            wrapper.passthrough(Types.INT);
            final short y = wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.passthrough(Types.INT);
            if (y > 127) {
                wrapper.cancel();
                return;
            }
            final int id = wrapper.read(Types.UNSIGNED_BYTE);
            final int meta = wrapper.read(Types.BYTE) & 15;
            final int mapped = this.helper.blocks().remap(id, meta);
            wrapper.write(Types.UNSIGNED_BYTE, (short) ((mapped >> 4) & 0xFF));
            wrapper.write(Types.BYTE, (byte) (mapped & 15));
        }, true);
        registerClientbound(LegacyClientbound.BLOCK_EVENT, LegacyClientbound.BLOCK_EVENT, wrapper -> {
        }, true);
        registerClientbound(LegacyClientbound.LEVEL_EVENT, LegacyClientbound.LEVEL_EVENT, wrapper -> {
            final int effect = wrapper.read(Types.INT);
            wrapper.write(Types.INT, effect == 1009 ? 1008 : effect);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.INT);
            final int data = wrapper.read(Types.INT);
            if (effect == 2001) {
                final int mapped = this.helper.blocks().remap(data & 4095, (data >> 12) & 15);
                wrapper.write(Types.INT, (mapped >> 4) | ((mapped & 15) << 8));
            } else {
                wrapper.write(Types.INT, data);
            }
        }, true);

        registerServerbound(LegacyServerbound.RESPAWN, wrapper -> {
            wrapper.clearPacket();
            wrapper.write(Types.INT, 0);
            wrapper.write(Types.BYTE, (byte) 0);
            wrapper.write(Types.BYTE, (byte) 0);
            wrapper.write(Types.SHORT, (short) 256);
            wrapper.write(LegacyTypes.STRING, "");
        });
    }

    private static void writeRegion(final PacketWrapper wrapper, final int x, final int y, final int z, final int width, final int height, final byte[] data) {
        final byte[] compressed = LegacyChunkRemapper.deflate(data);
        wrapper.write(Types.INT, x);
        wrapper.write(Types.SHORT, (short) y);
        wrapper.write(Types.INT, z);
        wrapper.write(Types.BYTE, (byte) (width - 1));
        wrapper.write(Types.BYTE, (byte) (height - 1));
        wrapper.write(Types.BYTE, (byte) (width - 1));
        wrapper.write(Types.INT, compressed.length);
        wrapper.write(Types.REMAINING_BYTES, compressed);
    }

}

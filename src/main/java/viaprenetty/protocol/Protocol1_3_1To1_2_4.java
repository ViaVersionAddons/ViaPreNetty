package viaprenetty.protocol;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viarewind.api.type.RewindTypes;
import viaprenetty.api.LegacyChunk12;
import viaprenetty.api.LegacyChunkRemapper;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;
import viaprenetty.storage.ClientWorld12;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Protocol1_3_1To1_2_4 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private static final Map<String, Integer> SPAWNER_ENTITIES = new HashMap<>();

    static {
        final String[] names = {"Creeper", "Skeleton", "Spider", "Giant", "Zombie", "Slime", "Ghast", "PigZombie", "Enderman", "CaveSpider", "Silverfish", "Blaze", "LavaSlime", "EnderDragon"};
        for (int i = 0; i < names.length; i++) {
            SPAWNER_ENTITIES.put(names[i], 50 + i);
        }
        final String[] animals = {"Pig", "Sheep", "Cow", "Chicken", "Squid", "Wolf", "MushroomCow", "SnowMan", "Ozelot", "VillagerGolem"};
        for (int i = 0; i < animals.length; i++) {
            SPAWNER_ENTITIES.put(animals[i], 90 + i);
        }
        SPAWNER_ENTITIES.put("Villager", 120);
    }

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_2, LegacyMappings.ITEMS_1_2)
        .withoutChunks()
        .withClientItems(LegacyTypes.ITEM_1_2, LegacyTypes.ITEM_ARRAY_1_2);

    public Protocol1_3_1To1_2_4() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, false, true);

        registerClientbound(LegacyClientbound.LOGIN, wrapper -> {
            wrapper.passthrough(Types.INT);
            final String levelType = wrapper.read(LegacyTypes.STRING);
            final byte gameMode = wrapper.read(Types.BYTE);
            final byte dimension = wrapper.read(Types.BYTE);
            wrapper.write(LegacyTypes.STRING, "");
            wrapper.write(LegacyTypes.STRING, levelType);
            wrapper.write(Types.INT, (int) gameMode);
            wrapper.write(Types.INT, (int) dimension);
            wrapper.user().get(ClientWorld12.class).dimension(dimension);
        });
        registerClientbound(LegacyClientbound.RESPAWN, wrapper -> wrapper.user().get(ClientWorld12.class).dimension(wrapper.passthrough(Types.INT)));
        registerClientbound(LegacyClientbound.SET_EQUIPPED_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            final Item item = this.helper.items().toClient(wrapper.read(RewindTypes.COMPRESSED_NBT_ITEM));
            wrapper.write(Types.SHORT, item == null ? (short) -1 : (short) item.identifier());
            wrapper.write(Types.SHORT, item == null ? (short) 0 : item.data());
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
            wrapper.write(Types.SHORT, (short) Math.max(0, this.helper.itemId(item)));
            wrapper.read(LegacyTypes.ENTITY_DATA_LIST_NBTLESS);
        });
        registerClientbound(LegacyClientbound.SPAWN_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.write(LegacyTypes.NBTLESS_ITEM, this.helper.items().toClient(wrapper.read(LegacyTypes.NBTLESS_ITEM)));
        });
        registerClientbound(LegacyClientbound.ADD_ENTITY, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte type = wrapper.read(Types.BYTE);
            final int x = wrapper.read(Types.INT);
            final int y = wrapper.read(Types.INT);
            final int z = wrapper.read(Types.INT);
            final int data = wrapper.read(Types.INT);
            if (type == 70) {
                final int block = data & 0xFFFF;
                final byte legacyType = block == 13 ? (byte) 71 : block == 122 ? (byte) 74 : (byte) 70;
                if (data > 0) {
                    wrapper.read(Types.SHORT);
                    wrapper.read(Types.SHORT);
                    wrapper.read(Types.SHORT);
                }
                wrapper.write(Types.BYTE, legacyType);
                wrapper.write(Types.INT, x);
                wrapper.write(Types.INT, y);
                wrapper.write(Types.INT, z);
                wrapper.write(Types.INT, 0);
                return;
            }
            if (!validObject(type)) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.BYTE, type);
            wrapper.write(Types.INT, x);
            wrapper.write(Types.INT, y);
            wrapper.write(Types.INT, z);
            wrapper.write(Types.INT, data);
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
            wrapper.read(Types.SHORT);
            wrapper.read(Types.SHORT);
            wrapper.read(Types.SHORT);
            final List<EntityData> list = wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST_NBTLESS);
            this.helper.entityDataItems(list);
        });
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST_NBTLESS));
        });
        registerClientbound(LegacyClientbound.REMOVE_ENTITIES, wrapper -> {
            final int count = wrapper.read(Types.BYTE) & 0xFF;
            if (count == 0) {
                wrapper.cancel();
                return;
            }
            final int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = wrapper.read(Types.INT);
            }
            for (int i = 0; i < count - 1; i++) {
                final PacketWrapper remove = PacketWrapper.create(LegacyClientbound.REMOVE_ENTITIES, wrapper.user());
                remove.write(Types.INT, ids[i]);
                remove.send(Protocol1_3_1To1_2_4.class);
            }
            wrapper.write(Types.INT, ids[count - 1]);
        });
        registerClientbound(LegacyClientbound.LEVEL_CHUNK, wrapper -> {
            final int chunkX = wrapper.read(Types.INT);
            final int chunkZ = wrapper.read(Types.INT);
            final boolean ground = wrapper.read(Types.BOOLEAN);
            final int primary = wrapper.read(Types.UNSIGNED_SHORT);
            final int add = wrapper.read(Types.UNSIGNED_SHORT);
            final int length = wrapper.read(Types.INT);
            final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
            if (ground && primary == 0) {
                preChunk(wrapper, chunkX, chunkZ, false);
                wrapper.cancel();
                return;
            }
            final byte[] raw = LegacyChunkRemapper.inflate(java.util.Arrays.copyOf(rest, Math.min(length, rest.length)));
            final boolean sky = wrapper.user().get(ClientWorld12.class).dimension() == 0;
            final byte[] converted = raw == null ? null : LegacyChunk12.convert(raw, 0, primary, add, ground, sky, this.helper.blocks());
            if (converted == null) {
                wrapper.cancel();
                return;
            }
            if (ground) {
                preChunk(wrapper, chunkX, chunkZ, true);
            }
            writeChunk(wrapper, chunkX, chunkZ, ground, primary, add, LegacyChunkRemapper.deflate(converted));
        });
        registerClientbound(LegacyClientbound.MAP_BULK_CHUNK, wrapper -> {
            wrapper.cancel();
            final short count = wrapper.read(Types.SHORT);
            final int length = wrapper.read(Types.INT);
            final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
            if (rest.length < length + count * 12) {
                return;
            }
            final byte[] raw = LegacyChunkRemapper.inflate(java.util.Arrays.copyOf(rest, length));
            if (raw == null) {
                return;
            }
            final boolean sky = wrapper.user().get(ClientWorld12.class).dimension() == 0;
            final io.netty.buffer.ByteBuf meta = io.netty.buffer.Unpooled.wrappedBuffer(rest, length, count * 12);
            int offset = 0;
            for (int i = 0; i < count; i++) {
                final int chunkX = meta.readInt();
                final int chunkZ = meta.readInt();
                final int primary = meta.readUnsignedShort();
                final int add = meta.readUnsignedShort();
                final byte[] converted = LegacyChunk12.convert(raw, offset, primary, add, true, sky, this.helper.blocks());
                offset += LegacyChunk12.size(primary, add, true, sky);
                if (converted == null) {
                    return;
                }
                preChunk(wrapper, chunkX, chunkZ, true);
                final PacketWrapper chunk = PacketWrapper.create(LegacyClientbound.LEVEL_CHUNK, wrapper.user());
                writeChunk(chunk, chunkX, chunkZ, true, primary, add, LegacyChunkRemapper.deflate(converted));
                chunk.send(Protocol1_3_1To1_2_4.class);
            }
        });
        registerClientbound(LegacyClientbound.BLOCK_UPDATE, LegacyClientbound.BLOCK_UPDATE, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.INT);
            final int id = wrapper.read(Types.SHORT) & 0xFFFF;
            final int meta = wrapper.read(Types.BYTE) & 15;
            final int mapped = this.helper.blocks().remap(id, meta);
            wrapper.write(Types.UNSIGNED_BYTE, (short) ((mapped >> 4) & 0xFF));
            wrapper.write(Types.BYTE, (byte) (mapped & 15));
        }, true);
        registerClientbound(LegacyClientbound.BLOCK_EVENT, LegacyClientbound.BLOCK_EVENT, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.read(Types.SHORT);
        }, true);
        cancelClientbound(LegacyClientbound.BLOCK_DESTRUCTION);
        cancelClientbound(LegacyClientbound.CUSTOM_SOUND);
        cancelClientbound(LegacyClientbound.COMMAND_SUGGESTIONS);
        registerClientbound(LegacyClientbound.EXPLODE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE);
            wrapper.passthrough(Types.DOUBLE);
            wrapper.passthrough(Types.DOUBLE);
            wrapper.passthrough(Types.FLOAT);
            final int records = wrapper.passthrough(Types.INT);
            for (int i = 0; i < records * 3; i++) {
                wrapper.passthrough(Types.BYTE);
            }
            wrapper.read(Types.FLOAT);
            wrapper.read(Types.FLOAT);
            wrapper.read(Types.FLOAT);
        });
        registerClientbound(LegacyClientbound.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.INT);
            final byte action = wrapper.passthrough(Types.BYTE);
            final CompoundTag tag = wrapper.read(RewindTypes.COMPRESSED_NBT);
            if (action != 1) {
                wrapper.cancel();
                return;
            }
            String name = null;
            if (tag != null) {
                final StringTag id = tag.getStringTag("EntityId");
                if (id != null) {
                    name = id.getValue();
                }
                final CompoundTag spawnData = tag.getCompoundTag("SpawnData");
                if (name == null && spawnData != null && spawnData.getStringTag("id") != null) {
                    name = spawnData.getStringTag("id").getValue();
                }
            }
            wrapper.write(Types.INT, name == null ? 90 : SPAWNER_ENTITIES.getOrDefault(name, 90));
            wrapper.write(Types.INT, 0);
            wrapper.write(Types.INT, 0);
        });
        registerClientbound(LegacyClientbound.PLAYER_ABILITIES, wrapper -> {
            final byte flags = wrapper.read(Types.BYTE);
            wrapper.read(Types.BYTE);
            wrapper.read(Types.BYTE);
            wrapper.write(Types.BOOLEAN, (flags & 1) != 0);
            wrapper.write(Types.BOOLEAN, (flags & 2) != 0);
            wrapper.write(Types.BOOLEAN, (flags & 4) != 0);
            wrapper.write(Types.BOOLEAN, (flags & 8) != 0);
        });
        registerClientbound(LegacyClientbound.OPEN_SCREEN, wrapper -> {
            final short windowId = wrapper.passthrough(Types.UNSIGNED_BYTE);
            final short type = wrapper.passthrough(Types.UNSIGNED_BYTE);
            if (type > 5) {
                wrapper.cancel();
                final PacketWrapper close = PacketWrapper.create(LegacyServerbound.CONTAINER_CLOSE, wrapper.user());
                close.write(Types.BYTE, (byte) windowId);
                close.sendToServer(Protocol1_3_1To1_2_4.class);
            }
        });
        registerClientbound(LegacyClientbound.CUSTOM_PAYLOAD, wrapper -> this.helper.customPayloadToClient(wrapper, (channel, data) -> channel.equals("MC|TrList") ? null : new Object[]{channel, data}));

        registerServerbound(LegacyServerbound.RESPAWN, LegacyServerbound.CLIENT_COMMAND, wrapper -> {
            wrapper.clearPacket();
            wrapper.write(Types.BYTE, (byte) 1);
        });
        registerServerbound(LegacyServerbound.USE_ITEM_ON, LegacyServerbound.USE_ITEM_ON, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.write(RewindTypes.COMPRESSED_NBT_ITEM, this.helper.items().toServer(wrapper.read(LegacyTypes.ITEM_1_2)));
            wrapper.write(Types.BYTE, (byte) 8);
            wrapper.write(Types.BYTE, (byte) 8);
            wrapper.write(Types.BYTE, (byte) 8);
        }, true);
        registerServerbound(LegacyServerbound.PLAYER_ABILITIES, wrapper -> {
            byte flags = 0;
            if (wrapper.read(Types.BOOLEAN)) {
                flags |= 1;
            }
            if (wrapper.read(Types.BOOLEAN)) {
                flags |= 2;
            }
            if (wrapper.read(Types.BOOLEAN)) {
                flags |= 4;
            }
            if (wrapper.read(Types.BOOLEAN)) {
                flags |= 8;
            }
            wrapper.write(Types.BYTE, flags);
            wrapper.write(Types.BYTE, (byte) 12);
            wrapper.write(Types.BYTE, (byte) 25);
        });
        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, this.helper::customPayloadToServer);
    }

    private static boolean validObject(final int type) {
        return type == 1 || (type >= 10 && type <= 12) || type == 50 || type == 51 || (type >= 60 && type <= 65) || type == 72 || type == 73 || type == 75 || type == 90;
    }

    private static void preChunk(final PacketWrapper wrapper, final int chunkX, final int chunkZ, final boolean load) {
        final PacketWrapper pre = PacketWrapper.create(LegacyClientbound.PRE_CHUNK, wrapper.user());
        pre.write(Types.INT, chunkX);
        pre.write(Types.INT, chunkZ);
        pre.write(Types.BOOLEAN, load);
        pre.send(Protocol1_3_1To1_2_4.class);
    }

    private static void writeChunk(final PacketWrapper wrapper, final int chunkX, final int chunkZ, final boolean ground, final int primary, final int add, final byte[] data) {
        wrapper.write(Types.INT, chunkX);
        wrapper.write(Types.INT, chunkZ);
        wrapper.write(Types.BOOLEAN, ground);
        wrapper.write(Types.UNSIGNED_SHORT, primary);
        wrapper.write(Types.UNSIGNED_SHORT, add);
        wrapper.write(Types.INT, data.length);
        wrapper.write(Types.INT, 0);
        wrapper.write(Types.REMAINING_BYTES, data);
    }

    @Override
    public void init(final UserConnection connection) {
        connection.put(new ClientWorld12());
    }

}

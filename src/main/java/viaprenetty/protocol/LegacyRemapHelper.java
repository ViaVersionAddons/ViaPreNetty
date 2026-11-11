package viaprenetty.protocol;

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viarewind.api.type.RewindTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import viaprenetty.api.BlockRemapper;
import viaprenetty.api.LegacyChunkRemapper;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyItemRemapper;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;

import java.util.List;

final class LegacyRemapHelper {

    private final BlockRemapper blocks;
    private final LegacyItemRemapper items;

    LegacyRemapHelper(final BlockRemapper blocks, final LegacyItemRemapper items) {
        this.blocks = blocks;
        this.items = items;
    }

    BlockRemapper blocks() {
        return this.blocks;
    }

    LegacyItemRemapper items() {
        return this.items;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void registerShared(final Protocol protocol, final boolean handleEquipment, final boolean handleWindowItems) {
        protocol.registerClientbound(LegacyClientbound.LEVEL_CHUNK, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) this::chunk);
        protocol.registerClientbound(LegacyClientbound.MAP_BULK_CHUNK, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> Protocol1_7_2To1_6_4.handleBulk(wrapper, this.blocks));
        protocol.registerClientbound(LegacyClientbound.CHUNK_BLOCKS_UPDATE, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> Protocol1_7_2To1_6_4.handleMultiBlock(wrapper, this.blocks));
        protocol.registerClientbound(LegacyClientbound.BLOCK_UPDATE, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) this::blockUpdate);
        protocol.registerClientbound(LegacyClientbound.BLOCK_EVENT, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) this::blockEvent);
        protocol.registerClientbound(LegacyClientbound.LEVEL_EVENT, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) this::levelEvent);
        if (handleEquipment) {
            protocol.registerClientbound(LegacyClientbound.SET_EQUIPPED_ITEM, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> {
                wrapper.passthrough(Types.INT);
                wrapper.passthrough(Types.SHORT);
                this.items.toClient(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
            });
        }
        protocol.registerClientbound(LegacyClientbound.CONTAINER_SET_SLOT, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            this.items.toClient(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
        if (handleWindowItems) {
            protocol.registerClientbound(LegacyClientbound.CONTAINER_SET_CONTENT, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> {
                wrapper.passthrough(Types.BYTE);
                for (Item item : wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM_ARRAY)) {
                    this.items.toClient(item);
                }
            });
        }
        protocol.registerServerbound(LegacyServerbound.USE_ITEM_ON, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            this.items.toServer(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
        protocol.registerServerbound(LegacyServerbound.CONTAINER_CLICK, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.BYTE);
            this.items.toServer(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
        protocol.registerServerbound(LegacyServerbound.SET_CREATIVE_MODE_SLOT, (com.viaversion.viaversion.api.protocol.remapper.PacketHandler) wrapper -> {
            wrapper.passthrough(Types.SHORT);
            this.items.toServer(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
    }

    void chunk(final PacketWrapper wrapper) {
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.BOOLEAN);
        final int primary = wrapper.passthrough(Types.UNSIGNED_SHORT);
        wrapper.passthrough(Types.UNSIGNED_SHORT);
        final int size = wrapper.read(Types.INT);
        final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
        final byte[] compressed = rest.length == size ? rest : java.util.Arrays.copyOf(rest, Math.min(size, rest.length));
        final byte[] remapped = LegacyChunkRemapper.remapSingle(compressed, primary, this.blocks);
        final byte[] out = remapped != null ? remapped : compressed;
        wrapper.write(Types.INT, out.length);
        wrapper.write(Types.REMAINING_BYTES, out);
    }

    void blockUpdate(final PacketWrapper wrapper) {
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.BYTE);
        wrapper.passthrough(Types.INT);
        final int id = wrapper.read(Types.SHORT) & 0xFFFF;
        final int meta = wrapper.read(Types.BYTE) & 15;
        final int mapped = this.blocks.remap(id, meta);
        wrapper.write(Types.SHORT, (short) (mapped >> 4));
        wrapper.write(Types.BYTE, (byte) (mapped & 15));
    }

    void blockEvent(final PacketWrapper wrapper) {
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.SHORT);
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.BYTE);
        wrapper.passthrough(Types.BYTE);
        final int id = wrapper.read(Types.SHORT) & 0xFFFF;
        wrapper.write(Types.SHORT, (short) (this.blocks.remap(id, 0) >> 4));
    }

    void levelEvent(final PacketWrapper wrapper) {
        final int effect = wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.BYTE);
        wrapper.passthrough(Types.INT);
        final int data = wrapper.read(Types.INT);
        if (effect == 2001) {
            final int mapped = this.blocks.remap(data & 4095, (data >> 12) & 15);
            wrapper.write(Types.INT, (mapped >> 4) | ((mapped & 15) << 12));
        } else {
            wrapper.write(Types.INT, data);
        }
    }

    String particle(final String particle) {
        if (particle.startsWith("tilecrack_")) {
            final String[] parts = particle.split("_");
            if (parts.length >= 3) {
                try {
                    final int mapped = this.blocks.remap(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    return "tilecrack_" + (mapped >> 4) + "_" + (mapped & 15);
                } catch (final NumberFormatException ignored) {
                    return particle;
                }
            }
        }
        if (particle.startsWith("iconcrack_")) {
            final String[] parts = particle.split("_");
            if (parts.length >= 2) {
                try {
                    return "iconcrack_" + itemId(Integer.parseInt(parts[1]));
                } catch (final NumberFormatException ignored) {
                    return particle;
                }
            }
        }
        return particle;
    }

    int fallingBlockData(final int data) {
        return Protocol1_7_2To1_6_4.fallingBlockData(data, this.blocks);
    }

    int itemId(final int id) {
        if (id <= 0) {
            return id;
        }
        final Item probe = new DataItem(id, (byte) 1, (short) 0, null);
        this.items.toClient(probe);
        return probe.identifier();
    }

    void entityDataItems(final List<EntityData> list) {
        for (EntityData data : list) {
            if (data.dataType().typeId() == LegacyTypes.LegacyEntityDataTypes.ITEM.typeId()) {
                this.items.toClient(data.value());
            }
        }
    }

    byte[] tradeList(final byte[] data) {
        final ByteBuf in = Unpooled.wrappedBuffer(data);
        final ByteBuf out = Unpooled.buffer();
        try {
            out.writeInt(in.readInt());
            final short count = in.readUnsignedByte();
            out.writeByte(count);
            for (int i = 0; i < count; i++) {
                RewindTypes.COMPRESSED_NBT_ITEM.write(out, this.items.toClient(RewindTypes.COMPRESSED_NBT_ITEM.read(in)));
                RewindTypes.COMPRESSED_NBT_ITEM.write(out, this.items.toClient(RewindTypes.COMPRESSED_NBT_ITEM.read(in)));
                final boolean third = in.readBoolean();
                out.writeBoolean(third);
                if (third) {
                    RewindTypes.COMPRESSED_NBT_ITEM.write(out, this.items.toClient(RewindTypes.COMPRESSED_NBT_ITEM.read(in)));
                }
                out.writeBoolean(in.readBoolean());
            }
            final byte[] result = new byte[out.readableBytes()];
            out.readBytes(result);
            return result;
        } catch (final Exception e) {
            return null;
        } finally {
            in.release();
            out.release();
        }
    }

    void customPayloadToClient(final PacketWrapper wrapper, final java.util.function.BiFunction<String, byte[], Object[]> rewrite) {
        final String channel = wrapper.read(LegacyTypes.STRING);
        final short length = wrapper.read(Types.SHORT);
        final byte[] data = length > 0 ? wrapper.read(Types.REMAINING_BYTES) : new byte[0];
        String outChannel = channel;
        byte[] outData = data;
        if (channel.equals("MC|TrList")) {
            outData = tradeList(data);
            if (outData == null) {
                wrapper.cancel();
                return;
            }
        }
        if (rewrite != null) {
            final Object[] result = rewrite.apply(outChannel, outData);
            if (result == null) {
                wrapper.cancel();
                return;
            }
            outChannel = (String) result[0];
            outData = (byte[]) result[1];
        }
        wrapper.write(LegacyTypes.STRING, outChannel);
        wrapper.write(Types.SHORT, (short) outData.length);
        wrapper.write(Types.REMAINING_BYTES, outData);
    }

    void customPayloadToServer(final PacketWrapper wrapper) {
        final String channel = wrapper.passthrough(LegacyTypes.STRING);
        if (!channel.equals("MC|BEdit") && !channel.equals("MC|BSign")) {
            return;
        }
        wrapper.read(Types.SHORT);
        final Item item = this.items.toServer(wrapper.read(RewindTypes.COMPRESSED_NBT_ITEM));
        final ByteBuf buf = Unpooled.buffer();
        try {
            RewindTypes.COMPRESSED_NBT_ITEM.write(buf, item);
            final byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            wrapper.write(Types.SHORT, (short) data.length);
            wrapper.write(Types.REMAINING_BYTES, data);
        } finally {
            buf.release();
        }
    }

}

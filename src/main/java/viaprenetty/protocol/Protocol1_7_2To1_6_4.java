package viaprenetty.protocol;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viarewind.api.minecraft.entitydata.EntityDataTypes1_7_6_10;
import com.viaversion.viarewind.api.type.RewindTypes;
import com.viaversion.viarewind.protocol.v1_7_6_10to1_7_2_5.packet.ClientboundPackets1_7_2_5;
import com.viaversion.viarewind.protocol.v1_7_6_10to1_7_2_5.packet.ServerboundPackets1_7_2_5;
import viaprenetty.api.BlockRemapper;
import viaprenetty.api.LegacyChunkRemapper;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyItemRemapper;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyText;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;
import viaprenetty.data.LegacySoundMappings;
import viaprenetty.storage.LegacyClient;

import java.util.List;

public final class Protocol1_7_2To1_6_4 extends LegacyProtocol<ClientboundPackets1_7_2_5, LegacyClientbound, ServerboundPackets1_7_2_5, LegacyServerbound> {

    private static final BlockRemapper BLOCKS = LegacyMappings.BLOCKS_1_6;
    private static final LegacyItemRemapper ITEMS = LegacyMappings.ITEMS_1_6;

    public Protocol1_7_2To1_6_4() {
        super(ClientboundPackets1_7_2_5.class, LegacyClientbound.class, ServerboundPackets1_7_2_5.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        registerLogin();
        registerClientboundPlay();
        registerClientboundWorld();
        registerClientboundEntities();
        registerClientboundInventory();
        registerEffects();
        registerServerbound();
    }

    private void registerLogin() {
        registerClientbound(State.LOGIN, 0x00, LegacyClientbound.DISCONNECT.getId(), wrapper -> {
            final String reason = wrapper.read(Types.STRING);
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(LegacyText.json17ToLegacy(reason), 256));
        });
        registerClientbound(State.LOGIN, 0x01, LegacyClientbound.DISCONNECT.getId(), wrapper -> {
            wrapper.clearPacket();
            wrapper.write(LegacyTypes.STRING, "This server runs in online mode. Legacy clients (1.4.7-1.6.4) need an offline-mode server.");
        });
        registerClientbound(State.LOGIN, 0x02, 0x02, PacketWrapper::cancel);
    }

    private void registerClientboundPlay() {
        registerClientbound(ClientboundPackets1_7_2_5.LOGIN, wrapper -> {
            final int entityId = wrapper.read(Types.INT);
            final short gameMode = wrapper.read(Types.UNSIGNED_BYTE);
            final byte dimension = wrapper.read(Types.BYTE);
            final short difficulty = wrapper.read(Types.UNSIGNED_BYTE);
            final short maxPlayers = wrapper.read(Types.UNSIGNED_BYTE);
            final String levelType = wrapper.read(Types.STRING);
            wrapper.write(Types.INT, entityId);
            wrapper.write(LegacyTypes.STRING, levelType(levelType));
            wrapper.write(Types.BYTE, (byte) gameMode);
            wrapper.write(Types.BYTE, dimension);
            wrapper.write(Types.BYTE, (byte) difficulty);
            wrapper.write(Types.BYTE, (byte) 0);
            wrapper.write(Types.BYTE, (byte) Math.min(maxPlayers, 60));
            final LegacyClient client = wrapper.user().get(LegacyClient.class);
            if (client != null) {
                client.setEntityId(entityId);
                client.connection().onPlayStarted();
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.CHAT, wrapper -> {
            final String json = wrapper.read(Types.STRING);
            if (LegacyText.isDeliveryFallback(json)) {
                wrapper.cancel();
                return;
            }
            wrapper.write(LegacyTypes.STRING, LegacyText.json17To16(json));
        });
        registerClientbound(ClientboundPackets1_7_2_5.RESPAWN, wrapper -> {
            wrapper.passthrough(Types.INT);
            final short difficulty = wrapper.read(Types.UNSIGNED_BYTE);
            final short gameMode = wrapper.read(Types.UNSIGNED_BYTE);
            final String levelType = wrapper.read(Types.STRING);
            wrapper.write(Types.BYTE, (byte) difficulty);
            wrapper.write(Types.BYTE, (byte) gameMode);
            wrapper.write(Types.SHORT, (short) 256);
            wrapper.write(LegacyTypes.STRING, levelType(levelType));
        });
        registerClientbound(ClientboundPackets1_7_2_5.PLAYER_POSITION, wrapper -> {
            final double x = wrapper.read(Types.DOUBLE);
            final double y = wrapper.read(Types.DOUBLE);
            final double z = wrapper.read(Types.DOUBLE);
            wrapper.write(Types.DOUBLE, x);
            wrapper.write(Types.DOUBLE, y);
            wrapper.write(Types.DOUBLE, y - 1.62D);
            wrapper.write(Types.DOUBLE, z);
            wrapper.passthrough(Types.FLOAT);
            wrapper.passthrough(Types.FLOAT);
            wrapper.passthrough(Types.BOOLEAN);
        });
        registerClientbound(ClientboundPackets1_7_2_5.SET_CARRIED_ITEM, wrapper -> wrapper.write(Types.SHORT, (short) wrapper.read(Types.BYTE)));
        registerClientbound(ClientboundPackets1_7_2_5.GAME_EVENT, wrapper -> {
            final short reason = wrapper.read(Types.UNSIGNED_BYTE);
            final float value = wrapper.read(Types.FLOAT);
            if (reason > 6) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.BYTE, (byte) reason);
            wrapper.write(Types.BYTE, (byte) value);
        });
        registerClientbound(ClientboundPackets1_7_2_5.PLAYER_INFO, wrapper -> {
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
            wrapper.passthrough(Types.BOOLEAN);
            wrapper.passthrough(Types.SHORT);
        });
        registerClientbound(ClientboundPackets1_7_2_5.COMMAND_SUGGESTIONS, wrapper -> {
            final int count = wrapper.read(Types.VAR_INT);
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    builder.append('\0');
                }
                builder.append(wrapper.read(Types.STRING));
            }
            wrapper.write(LegacyTypes.STRING, builder.toString());
        });
        registerClientbound(ClientboundPackets1_7_2_5.SET_OBJECTIVE, wrapper -> {
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 32));
            wrapper.passthrough(Types.BYTE);
        });
        registerClientbound(ClientboundPackets1_7_2_5.SET_SCORE, wrapper -> {
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
            final byte action = wrapper.passthrough(Types.BYTE);
            if (action != 1) {
                wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
                wrapper.passthrough(Types.INT);
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.SET_DISPLAY_OBJECTIVE, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
        });
        registerClientbound(ClientboundPackets1_7_2_5.SET_PLAYER_TEAM, wrapper -> {
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
            final byte mode = wrapper.passthrough(Types.BYTE);
            if (mode == 0 || mode == 2) {
                wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 32));
                wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
                wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
                wrapper.passthrough(Types.BYTE);
            }
            if (mode == 0 || mode == 3 || mode == 4) {
                final short count = wrapper.passthrough(Types.SHORT);
                for (int i = 0; i < count; i++) {
                    wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
                }
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.CUSTOM_PAYLOAD, wrapper -> {
            final String channel = wrapper.read(Types.STRING);
            final short length = wrapper.read(Types.SHORT);
            if (channel.length() > 20) {
                wrapper.cancel();
                return;
            }
            if (channel.equals("MC|TrList")) {
                final byte[] data = wrapper.read(Types.REMAINING_BYTES);
                final byte[] remapped = remapTradeList(wrapper.user(), data);
                if (remapped == null) {
                    wrapper.cancel();
                    return;
                }
                wrapper.write(LegacyTypes.STRING, channel);
                wrapper.write(Types.SHORT, (short) remapped.length);
                wrapper.write(Types.REMAINING_BYTES, remapped);
                return;
            }
            wrapper.write(LegacyTypes.STRING, channel);
            wrapper.write(Types.SHORT, length);
        });
        registerClientbound(ClientboundPackets1_7_2_5.DISCONNECT, wrapper -> {
            final String reason = wrapper.read(Types.STRING);
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(LegacyText.json17ToLegacy(reason), 256));
        });
        cancelClientbound(ClientboundPackets1_7_2_5.AWARD_STATS);
    }

    private void registerClientboundWorld() {
        registerClientbound(ClientboundPackets1_7_2_5.LEVEL_CHUNK, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BOOLEAN);
            final int primary = wrapper.passthrough(Types.UNSIGNED_SHORT);
            wrapper.passthrough(Types.UNSIGNED_SHORT);
            final int size = wrapper.read(Types.INT);
            final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
            final byte[] compressed = rest.length == size ? rest : java.util.Arrays.copyOf(rest, Math.min(size, rest.length));
            final byte[] remapped = LegacyChunkRemapper.remapSingle(compressed, primary, blocks());
            final byte[] out = remapped != null ? remapped : compressed;
            wrapper.write(Types.INT, out.length);
            wrapper.write(Types.REMAINING_BYTES, out);
        });
        registerClientbound(ClientboundPackets1_7_2_5.MAP_BULK_CHUNK, wrapper -> handleBulk(wrapper, blocks()));
        registerClientbound(ClientboundPackets1_7_2_5.CHUNK_BLOCKS_UPDATE, wrapper -> handleMultiBlock(wrapper, blocks()));
        registerClientbound(ClientboundPackets1_7_2_5.BLOCK_UPDATE, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.write(Types.BYTE, (byte) (short) wrapper.read(Types.UNSIGNED_BYTE));
            wrapper.passthrough(Types.INT);
            final int blockId = wrapper.read(Types.VAR_INT);
            final short meta = wrapper.read(Types.UNSIGNED_BYTE);
            final int mapped = blocks().remap(blockId, meta);
            wrapper.write(Types.SHORT, (short) (mapped >> 4));
            wrapper.write(Types.BYTE, (byte) (mapped & 15));
        });
        registerClientbound(ClientboundPackets1_7_2_5.BLOCK_EVENT, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.INT);
            wrapper.write(Types.BYTE, (byte) (short) wrapper.read(Types.UNSIGNED_BYTE));
            wrapper.write(Types.BYTE, (byte) (short) wrapper.read(Types.UNSIGNED_BYTE));
            final int blockId = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.SHORT, (short) (blocks().remap(blockId, 0) >> 4));
        });
        registerClientbound(ClientboundPackets1_7_2_5.BLOCK_DESTRUCTION, wrapper -> {
            wrapper.write(Types.INT, wrapper.read(Types.VAR_INT));
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
        });
        registerClientbound(ClientboundPackets1_7_2_5.EXPLODE, wrapper -> {
            wrapper.write(Types.DOUBLE, (double) wrapper.read(Types.FLOAT));
            wrapper.write(Types.DOUBLE, (double) wrapper.read(Types.FLOAT));
            wrapper.write(Types.DOUBLE, (double) wrapper.read(Types.FLOAT));
        });
        registerClientbound(ClientboundPackets1_7_2_5.LEVEL_EVENT, wrapper -> {
            final int effect = wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.INT);
            final int data = wrapper.read(Types.INT);
            if (effect == 2001) {
                final int mapped = blocks().remap(data & 4095, (data >> 12) & 15);
                wrapper.write(Types.INT, (mapped >> 4) | ((mapped & 15) << 12));
            } else {
                wrapper.write(Types.INT, data);
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.CUSTOM_SOUND, wrapper -> {
            final String sound = wrapper.read(Types.STRING);
            final String mapped = LegacySoundMappings.to1_6(sound);
            if (mapped == null || mapped.isEmpty()) {
                wrapper.cancel();
                return;
            }
            wrapper.write(LegacyTypes.STRING, mapped);
        });
        registerClientbound(ClientboundPackets1_7_2_5.LEVEL_PARTICLES, wrapper -> {
            final String particle = remapParticle(wrapper.read(Types.STRING), blocks());
            if (particle == null) {
                wrapper.cancel();
                return;
            }
            wrapper.write(LegacyTypes.STRING, particle);
        });
        registerClientbound(ClientboundPackets1_7_2_5.UPDATE_SIGN, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.INT);
            for (int i = 0; i < 4; i++) {
                wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 15));
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.MAP_ITEM_DATA, wrapper -> {
            final int mapId = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.SHORT, (short) 358);
            wrapper.write(Types.SHORT, (short) mapId);
        });
        registerClientbound(ClientboundPackets1_7_2_5.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.INT);
            final short action = wrapper.read(Types.UNSIGNED_BYTE);
            if (action > 4) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.BYTE, (byte) action);
        });
        registerClientbound(ClientboundPackets1_7_2_5.OPEN_SIGN_EDITOR, wrapper -> {
            final int x = wrapper.read(Types.INT);
            final int y = wrapper.read(Types.INT);
            final int z = wrapper.read(Types.INT);
            wrapper.write(Types.BYTE, (byte) 0);
            wrapper.write(Types.INT, x);
            wrapper.write(Types.INT, y);
            wrapper.write(Types.INT, z);
        });
        registerClientbound(ClientboundPackets1_7_2_5.PLAYER_SLEEP, wrapper -> {
            wrapper.passthrough(Types.INT);
            final int x = wrapper.read(Types.INT);
            final short y = wrapper.read(Types.UNSIGNED_BYTE);
            final int z = wrapper.read(Types.INT);
            wrapper.write(Types.BYTE, (byte) 0);
            wrapper.write(Types.INT, x);
            wrapper.write(Types.BYTE, (byte) y);
            wrapper.write(Types.INT, z);
        });
    }

    private void registerClientboundEntities() {
        registerClientbound(ClientboundPackets1_7_2_5.ANIMATE, wrapper -> {
            wrapper.write(Types.INT, wrapper.read(Types.VAR_INT));
            final short animation = wrapper.read(Types.UNSIGNED_BYTE);
            if (animation > 5) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.BYTE, (byte) (animation <= 2 ? animation + 1 : animation + 2));
        });
        registerClientbound(ClientboundPackets1_7_2_5.ADD_PLAYER, wrapper -> {
            wrapper.write(Types.INT, wrapper.read(Types.VAR_INT));
            wrapper.read(Types.STRING);
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 16));
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            final short item = wrapper.read(Types.SHORT);
            wrapper.write(Types.SHORT, (short) remapItemId(item));
            writeEntityData(wrapper, true);
        });
        registerClientbound(ClientboundPackets1_7_2_5.ADD_ENTITY, wrapper -> {
            wrapper.write(Types.INT, wrapper.read(Types.VAR_INT));
            final byte type = wrapper.passthrough(Types.BYTE);
            if (!validObject(type)) {
                wrapper.cancel();
                return;
            }
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            final int data = wrapper.read(Types.INT);
            wrapper.write(Types.INT, type == 70 ? fallingBlockData(data, blocks()) : data);
        });
        registerClientbound(ClientboundPackets1_7_2_5.ADD_MOB, wrapper -> {
            wrapper.write(Types.INT, wrapper.read(Types.VAR_INT));
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
            writeEntityData(wrapper, true);
        });
        registerClientbound(ClientboundPackets1_7_2_5.ADD_PAINTING, wrapper -> {
            wrapper.write(Types.INT, wrapper.read(Types.VAR_INT));
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 13));
        });
        registerClientbound(ClientboundPackets1_7_2_5.ADD_EXPERIENCE_ORB, wrapper -> wrapper.write(Types.INT, wrapper.read(Types.VAR_INT)));
        registerClientbound(ClientboundPackets1_7_2_5.ADD_GLOBAL_ENTITY, wrapper -> wrapper.write(Types.INT, wrapper.read(Types.VAR_INT)));
        registerClientbound(ClientboundPackets1_7_2_5.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            writeEntityData(wrapper, false);
        });
        registerClientbound(ClientboundPackets1_7_2_5.UPDATE_ATTRIBUTES, wrapper -> {
            wrapper.passthrough(Types.INT);
            final int count = wrapper.passthrough(Types.INT);
            for (int i = 0; i < count; i++) {
                wrapper.write(LegacyTypes.STRING, wrapper.read(Types.STRING));
                wrapper.passthrough(Types.DOUBLE);
                final short modifiers = wrapper.passthrough(Types.SHORT);
                for (int j = 0; j < modifiers; j++) {
                    wrapper.passthrough(Types.LONG);
                    wrapper.passthrough(Types.LONG);
                    wrapper.passthrough(Types.DOUBLE);
                    wrapper.passthrough(Types.BYTE);
                }
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.SET_EQUIPPED_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            passthroughItem(wrapper);
        });
    }

    private void registerEffects() {
        registerClientbound(ClientboundPackets1_7_2_5.UPDATE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte effect = wrapper.passthrough(Types.BYTE);
            if ((effect & 0xFF) > 23 || effect == 0) {
                wrapper.cancel();
            }
        });
        registerClientbound(ClientboundPackets1_7_2_5.REMOVE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte effect = wrapper.passthrough(Types.BYTE);
            if ((effect & 0xFF) > 23 || effect == 0) {
                wrapper.cancel();
            }
        });
    }

    private void registerClientboundInventory() {
        registerClientbound(ClientboundPackets1_7_2_5.OPEN_SCREEN, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.write(LegacyTypes.STRING, LegacyTypes.limit(wrapper.read(Types.STRING), 32));
        });
        registerClientbound(ClientboundPackets1_7_2_5.CONTAINER_SET_SLOT, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            passthroughItem(wrapper);
        });
        registerClientbound(ClientboundPackets1_7_2_5.CONTAINER_SET_CONTENT, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            final Item[] items = wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM_ARRAY);
            for (Item item : items) {
                items().toClient(item);
            }
        });
    }

    private static boolean validObject(final int type) {
        return type == 1 || type == 2 || (type >= 10 && type <= 12) || type == 50 || type == 51 || (type >= 60 && type <= 66) || (type >= 70 && type <= 73) || (type >= 75 && type <= 77) || type == 90;
    }

    private void registerServerbound() {
        registerServerbound(LegacyServerbound.CHAT, wrapper -> wrapper.write(Types.STRING, wrapper.read(LegacyTypes.STRING)));
        registerServerbound(LegacyServerbound.INTERACT, wrapper -> {
            wrapper.read(Types.INT);
            final int target = wrapper.passthrough(Types.INT);
            final byte mode = wrapper.passthrough(Types.BYTE);
            if (mode == 0) {
                final PacketWrapper at = PacketWrapper.create(com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8.INTERACT, wrapper.user());
                at.write(Types.VAR_INT, target);
                at.write(Types.VAR_INT, 2);
                at.write(Types.FLOAT, 0F);
                at.write(Types.FLOAT, 0.5F);
                at.write(Types.FLOAT, 0F);
                at.sendToServer(com.viaversion.viarewind.protocol.v1_8to1_7_6_10.Protocol1_8To1_7_6_10.class);
            }
        });
        registerServerbound(LegacyServerbound.USE_ITEM_ON, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BYTE);
            items().toServer(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
        registerServerbound(LegacyServerbound.CONTAINER_CLICK, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.BYTE);
            items().toServer(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
        registerServerbound(LegacyServerbound.SET_CREATIVE_MODE_SLOT, wrapper -> {
            wrapper.passthrough(Types.SHORT);
            items().toServer(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
        });
        registerServerbound(LegacyServerbound.SIGN_UPDATE, wrapper -> {
            final int x = wrapper.read(Types.INT);
            final short y = wrapper.read(Types.SHORT);
            final int z = wrapper.read(Types.INT);
            final String[] lines = new String[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = LegacyTypes.limit(wrapper.read(LegacyTypes.STRING), 15);
            }
            final PacketWrapper sign = PacketWrapper.create(com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8.SIGN_UPDATE, wrapper.user());
            sign.write(Types.BLOCK_POSITION1_8, new com.viaversion.viaversion.api.minecraft.BlockPosition(x, y, z));
            for (String line : lines) {
                sign.write(Types.COMPONENT, new com.viaversion.viaversion.libs.gson.JsonPrimitive(line));
            }
            sign.sendToServer(com.viaversion.viarewind.protocol.v1_8to1_7_6_10.Protocol1_8To1_7_6_10.class);
            wrapper.cancel();
        });
        registerServerbound(LegacyServerbound.COMMAND_SUGGESTION, wrapper -> wrapper.write(Types.STRING, wrapper.read(LegacyTypes.STRING)));
        registerServerbound(LegacyServerbound.CLIENT_INFORMATION, wrapper -> {
            wrapper.write(Types.STRING, wrapper.read(LegacyTypes.STRING));
            final byte view = wrapper.read(Types.BYTE);
            final byte chatFlags = wrapper.read(Types.BYTE);
            final byte difficulty = wrapper.read(Types.BYTE);
            final boolean cape = wrapper.read(Types.BOOLEAN);
            final byte chunks = switch (view) {
                case 0 -> 12;
                case 1 -> 8;
                case 2 -> 4;
                default -> 2;
            };
            wrapper.write(Types.BYTE, chunks);
            wrapper.write(Types.BYTE, (byte) (chatFlags & 3));
            wrapper.write(Types.BOOLEAN, (chatFlags & 8) != 0);
            wrapper.write(Types.BYTE, difficulty);
            wrapper.write(Types.BOOLEAN, cape);
        });
        registerServerbound(LegacyServerbound.CLIENT_COMMAND, wrapper -> {
            final byte action = wrapper.read(Types.BYTE);
            if (action != 1) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.VAR_INT, 0);
        });
        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, wrapper -> {
            final String channel = wrapper.read(LegacyTypes.STRING);
            final short length = wrapper.read(Types.SHORT);
            if (channel.equals("MC|AdvCdm")) {
                wrapper.cancel();
                return;
            }
            if (channel.equals("MC|BEdit") || channel.equals("MC|BSign")) {
                final Item item = items().toServer(wrapper.read(RewindTypes.COMPRESSED_NBT_ITEM));
                final io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
                try {
                    RewindTypes.COMPRESSED_NBT_ITEM.write(buf, item);
                    final byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    wrapper.write(Types.STRING, channel);
                    wrapper.write(Types.SHORT, (short) data.length);
                    wrapper.write(Types.REMAINING_BYTES, data);
                } finally {
                    buf.release();
                }
                return;
            }
            wrapper.write(Types.STRING, channel);
            wrapper.write(Types.SHORT, length);
        });
        cancelServerbound(LegacyServerbound.LOGIN);
        cancelServerbound(LegacyServerbound.CLIENT_PROTOCOL);
        cancelServerbound(LegacyServerbound.RESPAWN);
        cancelServerbound(LegacyServerbound.SHARED_KEY);
        cancelServerbound(LegacyServerbound.SERVER_PING);
        cancelServerbound(LegacyServerbound.DISCONNECT);
    }

    BlockRemapper blocks() {
        return BLOCKS;
    }

    LegacyItemRemapper items() {
        return ITEMS;
    }

    private int remapItemId(final int id) {
        if (id <= 0) {
            return id;
        }
        final Item probe = new com.viaversion.viaversion.api.minecraft.item.DataItem(id, (byte) 1, (short) 0, null);
        items().toClient(probe);
        return probe.identifier();
    }

    private void passthroughItem(final PacketWrapper wrapper) {
        items().toClient(wrapper.passthrough(RewindTypes.COMPRESSED_NBT_ITEM));
    }

    private void writeEntityData(final PacketWrapper wrapper, final boolean spawn) {
        final List<EntityData> list = new java.util.ArrayList<>(wrapper.read(RewindTypes.ENTITY_DATA_LIST1_7));
        if (list.isEmpty() && !spawn) {
            wrapper.cancel();
            return;
        }
        for (EntityData data : list) {
            final LegacyTypes.LegacyEntityDataTypes type = LegacyTypes.LegacyEntityDataTypes.byId(data.dataType().typeId());
            if (type == LegacyTypes.LegacyEntityDataTypes.ITEM) {
                items().toClient(data.value());
            }
            data.setDataTypeUnsafe(type);
        }
        wrapper.write(LegacyTypes.ENTITY_DATA_LIST, LegacyTypes.nonEmpty(list));
    }

    private byte[] remapTradeList(final UserConnection user, final byte[] data) {
        final io.netty.buffer.ByteBuf in = io.netty.buffer.Unpooled.wrappedBuffer(data);
        final io.netty.buffer.ByteBuf out = io.netty.buffer.Unpooled.buffer();
        try {
            out.writeInt(in.readInt());
            final short count = in.readUnsignedByte();
            out.writeByte(count);
            for (int i = 0; i < count; i++) {
                RewindTypes.COMPRESSED_NBT_ITEM.write(out, items().toClient(RewindTypes.COMPRESSED_NBT_ITEM.read(in)));
                RewindTypes.COMPRESSED_NBT_ITEM.write(out, items().toClient(RewindTypes.COMPRESSED_NBT_ITEM.read(in)));
                final boolean third = in.readBoolean();
                out.writeBoolean(third);
                if (third) {
                    RewindTypes.COMPRESSED_NBT_ITEM.write(out, items().toClient(RewindTypes.COMPRESSED_NBT_ITEM.read(in)));
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

    static String levelType(final String type) {
        if (type == null) {
            return "default";
        }
        return switch (type.toLowerCase()) {
            case "flat" -> "flat";
            case "largebiomes" -> "largeBiomes";
            case "default_1_1" -> "default_1_1";
            default -> "default";
        };
    }

    private static final java.util.Set<String> PARTICLES = java.util.Set.of("hugeexplosion", "largeexplode", "fireworksSpark", "bubble", "suspended", "depthsuspend", "townaura", "crit", "magicCrit", "smoke", "mobSpell", "mobSpellAmbient", "spell", "instantSpell", "witchMagic", "note", "portal", "enchantmenttable", "explode", "flame", "lava", "footstep", "splash", "largesmoke", "cloud", "reddust", "snowballpoof", "dripWater", "dripLava", "snowshovel", "slime", "heart", "angryVillager", "happyVillager");

    static String remapParticle(final String particle, final BlockRemapper remapper) {
        if (PARTICLES.contains(particle)) {
            return particle;
        }
        switch (particle) {
            case "wake":
                return "splash";
            case "droplet":
                return "dripWater";
            default:
                break;
        }
        final String[] parts = particle.split("_");
        if (parts[0].equals("blockcrack") || parts[0].equals("blockdust")) {
            if (parts.length < 3) {
                return null;
            }
            try {
                final int mapped = remapper.remap(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                return "tilecrack_" + (mapped >> 4) + "_" + (mapped & 15);
            } catch (final NumberFormatException e) {
                return null;
            }
        }
        if (parts[0].equals("iconcrack") && parts.length >= 2) {
            try {
                final com.viaversion.viaversion.api.minecraft.item.Item probe = new com.viaversion.viaversion.api.minecraft.item.DataItem(Integer.parseInt(parts[1]), (byte) 1, (short) 0, null);
                LegacyMappings.ITEMS_1_6.toClient(probe);
                return "iconcrack_" + probe.identifier();
            } catch (final NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    static int fallingBlockData(final int data, final BlockRemapper remapper) {
        if (data == 0) {
            return 0;
        }
        final int mapped = remapper.remap(data & 0xFFFF, (data >> 16) & 15);
        final int id = mapped >> 4;
        if (id == 0) {
            return 1;
        }
        return id | ((mapped & 15) << 16);
    }

    static void handleBulk(final PacketWrapper wrapper, final BlockRemapper remapper) {
        final short count = wrapper.read(Types.SHORT);
        final int length = wrapper.read(Types.INT);
        final boolean skyLight = wrapper.read(Types.BOOLEAN);
        final byte[] rest = wrapper.read(Types.REMAINING_BYTES);
        final int metaLength = count * 12;
        if (rest.length < length + metaLength) {
            wrapper.cancel();
            return;
        }
        final byte[] compressed = java.util.Arrays.copyOfRange(rest, 0, length);
        final io.netty.buffer.ByteBuf meta = io.netty.buffer.Unpooled.wrappedBuffer(rest, length, metaLength);
        final int[] primary = new int[count];
        final int[] add = new int[count];
        for (int i = 0; i < count; i++) {
            meta.skipBytes(8);
            primary[i] = meta.readUnsignedShort();
            add[i] = meta.readUnsignedShort();
        }
        final byte[] remapped = LegacyChunkRemapper.remapBulk(compressed, primary, add, skyLight, remapper);
        final byte[] out = remapped != null ? remapped : compressed;
        final byte[] combined = new byte[out.length + metaLength];
        System.arraycopy(out, 0, combined, 0, out.length);
        System.arraycopy(rest, length, combined, out.length, metaLength);
        wrapper.write(Types.SHORT, count);
        wrapper.write(Types.INT, out.length);
        wrapper.write(Types.BOOLEAN, skyLight);
        wrapper.write(Types.REMAINING_BYTES, combined);
    }

    static void handleMultiBlock(final PacketWrapper wrapper, final BlockRemapper remapper) {
        wrapper.passthrough(Types.INT);
        wrapper.passthrough(Types.INT);
        final short count = wrapper.passthrough(Types.SHORT);
        wrapper.passthrough(Types.INT);
        for (int i = 0; i < count; i++) {
            final int record = wrapper.read(Types.INT);
            final int id = (record >> 4) & 4095;
            final int meta = record & 15;
            final int mapped = remapper.remap(id, meta);
            wrapper.write(Types.INT, (record & 0xFFFF0000) | ((mapped >> 4) << 4) | (mapped & 15));
        }
    }

    @Override
    public void init(final UserConnection connection) {
    }

}

package viaprenetty.protocol;

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viarewind.api.type.RewindTypes;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;
import viaprenetty.data.LegacySoundMappings;

import java.util.List;

public final class Protocol1_4_2To1_3_1 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_3, LegacyMappings.ITEMS_1_3).withoutBulkSkyFlag().withoutTradeDisabledFlag();

    public Protocol1_4_2To1_3_1() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, true, true);

        registerClientbound(LegacyClientbound.SET_TIME, wrapper -> {
            wrapper.read(Types.LONG);
            final long time = wrapper.read(Types.LONG);
            wrapper.write(Types.LONG, Math.abs(time));
        });
        registerClientbound(LegacyClientbound.SPAWN_ITEM, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.write(LegacyTypes.NBTLESS_ITEM, this.helper.items().toClient(wrapper.read(RewindTypes.COMPRESSED_NBT_ITEM)));
        });
        registerClientbound(LegacyClientbound.ADD_ENTITY, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte type = wrapper.passthrough(Types.BYTE);
            if (type == 66 || type == 71) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.ADD_MOB, wrapper -> {
            wrapper.passthrough(Types.INT);
            final short type = wrapper.passthrough(Types.UNSIGNED_BYTE);
            if (type >= 64 && type <= 66) {
                wrapper.cancel();
                return;
            }
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
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            entityData(wrapper);
        });
        registerClientbound(LegacyClientbound.ADD_PAINTING, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(LegacyTypes.STRING);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.INT);
            final int direction = wrapper.read(Types.INT);
            wrapper.write(Types.INT, direction == 0 ? 2 : direction == 2 ? 0 : direction);
        });
        registerClientbound(LegacyClientbound.LEVEL_EVENT, LegacyClientbound.LEVEL_EVENT, wrapper -> {
            this.helper.levelEvent(wrapper);
            wrapper.read(Types.BOOLEAN);
        }, true);
        registerClientbound(LegacyClientbound.CUSTOM_SOUND, wrapper -> {
            final String sound = LegacySoundMappings.to1_3(wrapper.read(LegacyTypes.STRING));
            if (sound == null) {
                wrapper.cancel();
                return;
            }
            wrapper.write(LegacyTypes.STRING, sound);
        });
        registerClientbound(LegacyClientbound.MAP_ITEM_DATA, wrapper -> {
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.SHORT);
            final short length = wrapper.passthrough(Types.UNSIGNED_BYTE);
            final byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = wrapper.read(Types.BYTE);
            }
            if (length > 0 && data[0] == 1) {
                for (int i = 0; i < (length - 1) / 3; i++) {
                    final int value = data[i * 3 + 1] & 0xFF;
                    data[i * 3 + 1] = (byte) (((value & 15) << 4) | ((value >> 4) & 15));
                }
            }
            for (byte b : data) {
                wrapper.write(Types.BYTE, b);
            }
        });
        registerClientbound(LegacyClientbound.OPEN_SCREEN, wrapper -> {
            final short windowId = wrapper.passthrough(Types.UNSIGNED_BYTE);
            final short type = wrapper.passthrough(Types.UNSIGNED_BYTE);
            if (type > 6) {
                wrapper.cancel();
                final PacketWrapper close = PacketWrapper.create(LegacyServerbound.CONTAINER_CLOSE, wrapper.user());
                close.write(Types.BYTE, (byte) windowId);
                close.sendToServer(Protocol1_4_2To1_3_1.class);
            }
        });
        registerClientbound(LegacyClientbound.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.INT);
            if (wrapper.passthrough(Types.BYTE) != 1) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.GAME_EVENT, wrapper -> {
            if (wrapper.passthrough(Types.BYTE) > 4) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.UPDATE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.INT);
            if (wrapper.passthrough(Types.BYTE) > 19) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.REMOVE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.INT);
            if (wrapper.passthrough(Types.BYTE) > 19) {
                wrapper.cancel();
            }
        });
        registerClientbound(LegacyClientbound.CUSTOM_PAYLOAD, wrapper -> this.helper.customPayloadToClient(wrapper, null));

        registerServerbound(LegacyServerbound.CLIENT_INFORMATION, wrapper -> {
            wrapper.passthrough(LegacyTypes.STRING);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.BYTE);
            wrapper.write(Types.BOOLEAN, true);
        });
        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, this.helper::customPayloadToServer);
    }

    private void entityData(final PacketWrapper wrapper) {
        final List<EntityData> list = wrapper.read(LegacyTypes.ENTITY_DATA_LIST_NBTLESS);
        this.helper.entityDataItems(list);
        wrapper.write(LegacyTypes.ENTITY_DATA_LIST_NBTLESS, list);
    }

}

package viaprenetty.protocol;

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;
import viaprenetty.data.LegacyMappings;

import java.util.List;

public final class Protocol1_5_0To1_4_6 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    private final LegacyRemapHelper helper = new LegacyRemapHelper(LegacyMappings.BLOCKS_1_4, LegacyMappings.ITEMS_1_4);

    public Protocol1_5_0To1_4_6() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        this.helper.registerShared(this, true, true);

        registerClientbound(LegacyClientbound.ADD_ENTITY, wrapper -> {
            wrapper.passthrough(Types.INT);
            final byte type = wrapper.read(Types.BYTE);
            final int x = wrapper.read(Types.INT);
            final int y = wrapper.read(Types.INT);
            final int z = wrapper.read(Types.INT);
            final byte pitch = wrapper.read(Types.BYTE);
            final byte yaw = wrapper.read(Types.BYTE);
            final int data = wrapper.read(Types.INT);
            if (type == 10) {
                if (data > 0) {
                    wrapper.read(Types.SHORT);
                    wrapper.read(Types.SHORT);
                    wrapper.read(Types.SHORT);
                }
                final byte legacyType = (byte) (data == 1 ? 11 : data == 2 ? 12 : 10);
                wrapper.write(Types.BYTE, legacyType);
                wrapper.write(Types.INT, x);
                wrapper.write(Types.INT, y);
                wrapper.write(Types.INT, z);
                wrapper.write(Types.BYTE, pitch);
                wrapper.write(Types.BYTE, yaw);
                wrapper.write(Types.INT, 0);
                return;
            }
            wrapper.write(Types.BYTE, type);
            wrapper.write(Types.INT, x);
            wrapper.write(Types.INT, y);
            wrapper.write(Types.INT, z);
            wrapper.write(Types.BYTE, pitch);
            wrapper.write(Types.BYTE, yaw);
            wrapper.write(Types.INT, type == 70 ? this.helper.fallingBlockData(data) : data);
        });
        registerClientbound(LegacyClientbound.OPEN_SCREEN, wrapper -> {
            final short windowId = wrapper.passthrough(Types.UNSIGNED_BYTE);
            final short type = wrapper.read(Types.UNSIGNED_BYTE);
            if (type == 9 || type > 10) {
                wrapper.cancel();
                try {
                    final PacketWrapper close = PacketWrapper.create(LegacyServerbound.CONTAINER_CLOSE, wrapper.user());
                    close.write(Types.BYTE, (byte) windowId);
                    close.sendToServer(Protocol1_5_0To1_4_6.class);
                } catch (final Exception ignored) {
                }
                return;
            }
            wrapper.write(Types.UNSIGNED_BYTE, type == 10 ? 3 : type);
            wrapper.passthrough(LegacyTypes.STRING);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            wrapper.read(Types.BOOLEAN);
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
            final List<EntityData> data = wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST);
            this.helper.entityDataItems(data);
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
        registerClientbound(LegacyClientbound.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.INT);
            this.helper.entityDataItems(wrapper.passthrough(LegacyTypes.ENTITY_DATA_LIST));
        });
        registerClientbound(LegacyClientbound.CUSTOM_PAYLOAD, wrapper -> this.helper.customPayloadToClient(wrapper, null));
        cancelClientbound(LegacyClientbound.LEVEL_PARTICLES);
        cancelClientbound(LegacyClientbound.SET_OBJECTIVE);
        cancelClientbound(LegacyClientbound.SET_SCORE);
        cancelClientbound(LegacyClientbound.SET_DISPLAY_OBJECTIVE);
        cancelClientbound(LegacyClientbound.SET_PLAYER_TEAM);

        registerServerbound(LegacyServerbound.CUSTOM_PAYLOAD, this.helper::customPayloadToServer);
    }

}

package viaprenetty.api;

import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypeMap;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.exception.CancelException;
import com.viaversion.viaversion.exception.InformativeException;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class LegacyProtocol<CU extends ClientboundPacketType, CM extends ClientboundPacketType, SM extends ServerboundPacketType, SU extends ServerboundPacketType> extends AbstractProtocol<CU, CM, SM, SU> {

    private static final Field INPUT_BUFFER;

    static {
        Field field = null;
        try {
            field = PacketWrapperImpl.class.getDeclaredField("inputBuffer");
            field.setAccessible(true);
        } catch (final ReflectiveOperationException ignored) {
        }
        INPUT_BUFFER = field;
    }

    protected LegacyProtocol(final Class<CU> unmappedClientbound, final Class<CM> mappedClientbound, final Class<SM> mappedServerbound, final Class<SU> unmappedServerbound) {
        super(unmappedClientbound, mappedClientbound, mappedServerbound, unmappedServerbound);
    }

    @Override
    protected PacketTypesProvider<CU, CM, SM, SU> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            Map.of(State.PLAY, PacketTypeMap.ofUnsequenced(this.unmappedClientboundPacketType)),
            Map.of(State.PLAY, PacketTypeMap.ofUnsequenced(this.mappedClientboundPacketType)),
            Map.of(State.PLAY, PacketTypeMap.ofUnsequenced(this.mappedServerboundPacketType)),
            Map.of(State.PLAY, PacketTypeMap.ofUnsequenced(this.unmappedServerboundPacketType))
        );
    }

    @Override
    public void transform(final Direction direction, final State state, final PacketWrapper wrapper) throws InformativeException, CancelException {
        final boolean handled = direction == Direction.CLIENTBOUND
            ? hasRegisteredClientbound(state, wrapper.getId())
            : hasRegisteredServerbound(state, wrapper.getId());
        if (!handled) {
            super.transform(direction, state, wrapper);
            return;
        }
        flatten(wrapper);
        super.transform(direction, state, wrapper);
        if (!wrapper.isCancelled()) {
            flatten(wrapper);
        }
    }

    public static void flatten(final PacketWrapper wrapper) {
        if (!(wrapper instanceof PacketWrapperImpl impl) || impl.areStoredPacketValuesEmpty()) {
            return;
        }
        final ByteBuf tmp = impl.user() != null && impl.user().getChannel() != null
            ? impl.user().getChannel().alloc().directBuffer()
            : Unpooled.directBuffer();
        try {
            final int id = impl.getId();
            impl.writeToBuffer(tmp);
            if (id != -1) {
                Types.VAR_INT.readPrimitive(tmp);
            }
            ByteBuf input = impl.getInputBuffer();
            if (input == null) {
                if (INPUT_BUFFER == null) {
                    return;
                }
                input = Unpooled.buffer(tmp.readableBytes());
                INPUT_BUFFER.set(impl, input);
            }
            impl.clearPacket();
            input.writeBytes(tmp);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } finally {
            tmp.release();
        }
    }

}

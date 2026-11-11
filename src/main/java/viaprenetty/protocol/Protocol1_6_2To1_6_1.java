package viaprenetty.protocol;

import com.viaversion.viaversion.api.type.Types;
import viaprenetty.api.LegacyClientbound;
import viaprenetty.api.LegacyProtocol;
import viaprenetty.api.LegacyServerbound;
import viaprenetty.api.LegacyTypes;

public final class Protocol1_6_2To1_6_1 extends LegacyProtocol<LegacyClientbound, LegacyClientbound, LegacyServerbound, LegacyServerbound> {

    public Protocol1_6_2To1_6_1() {
        super(LegacyClientbound.class, LegacyClientbound.class, LegacyServerbound.class, LegacyServerbound.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();
        registerClientbound(LegacyClientbound.UPDATE_ATTRIBUTES, wrapper -> {
            wrapper.passthrough(Types.INT);
            final int count = wrapper.passthrough(Types.INT);
            for (int i = 0; i < count; i++) {
                wrapper.passthrough(LegacyTypes.STRING);
                wrapper.passthrough(Types.DOUBLE);
                final short modifiers = wrapper.read(Types.SHORT);
                for (int j = 0; j < modifiers; j++) {
                    wrapper.read(Types.LONG);
                    wrapper.read(Types.LONG);
                    wrapper.read(Types.DOUBLE);
                    wrapper.read(Types.BYTE);
                }
            }
        });
        cancelClientbound(LegacyClientbound.OPEN_SIGN_EDITOR);
    }

}

package viaprenetty;

import com.viaversion.viaversion.api.Via;
import com.google.common.collect.Range;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.protocols.base.v1_7.ServerboundBaseProtocol1_7;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.plugin.java.JavaPlugin;
import viaprenetty.net.PaperChannelHook;
import viaprenetty.protocol.Protocol1_5_0To1_4_6;
import viaprenetty.protocol.Protocol1_5_2To1_5_0;
import viaprenetty.protocol.Protocol1_6_1To1_5_2;
import viaprenetty.protocol.Protocol1_6_2To1_6_1;
import viaprenetty.protocol.Protocol1_6_4To1_6_2;
import viaprenetty.protocol.Protocol1_7_2To1_6_4;

public final class ViaPreNettyPlugin extends JavaPlugin {

    private boolean hooked;

    public ViaPreNettyPlugin() {
        LegacyVersions.init();
        if (Via.getManager().isInitialized()) {
            registerProtocols();
        } else {
            Via.getManager().addEnableListener(this::registerProtocols);
        }
    }

    private void registerProtocols() {
        final ProtocolManager manager = Via.getManager().getProtocolManager();
        manager.registerProtocol(new Protocol1_7_2To1_6_4(), LegacyVersions.R1_6_4, ProtocolVersion.v1_7_2);
        manager.registerProtocol(new Protocol1_6_4To1_6_2(), LegacyVersions.R1_6_2, LegacyVersions.R1_6_4);
        manager.registerProtocol(new Protocol1_6_2To1_6_1(), LegacyVersions.R1_6_1, LegacyVersions.R1_6_2);
        manager.registerProtocol(new Protocol1_6_1To1_5_2(), LegacyVersions.R1_5_2, LegacyVersions.R1_6_1);
        manager.registerProtocol(new Protocol1_5_2To1_5_0(), LegacyVersions.R1_5_0, LegacyVersions.R1_5_2);
        manager.registerProtocol(new Protocol1_5_0To1_4_6(), LegacyVersions.R1_4_6, LegacyVersions.R1_5_0);
        manager.registerBaseProtocol(Direction.SERVERBOUND, new ServerboundBaseProtocol1_7(), Range.closed(LegacyVersions.R1_4_6, LegacyVersions.R1_6_4));
        if (manager.getMaxProtocolPathSize() < 64) {
            manager.setMaxProtocolPathSize(64);
        }
        LegacyVersionProvider.install();
    }

    @Override
    public void onEnable() {
        viaprenetty.api.LegacyText.loadServerLanguage(org.bukkit.Bukkit.class.getClassLoader());
        if (!PaperChannelHook.isSupported()) {
            getLogger().severe("This server is not running Paper. Legacy clients (1.4.7-1.6.4) need Paper's channel API.");
            return;
        }
        try {
            PaperChannelHook.install();
            this.hooked = true;
        } catch (final ReflectiveOperationException e) {
            getLogger().severe("Could not hook into the Paper network pipeline: " + e);
        }
    }

    @Override
    public void onDisable() {
        if (this.hooked) {
            PaperChannelHook.uninstall();
        }
    }

}

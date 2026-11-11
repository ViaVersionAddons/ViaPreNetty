package viaprenetty;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import viaprenetty.storage.LegacyClient;

public final class LegacyVersionProvider implements VersionProvider {

    private final VersionProvider parent;

    private LegacyVersionProvider(final VersionProvider parent) {
        this.parent = parent;
    }

    public static void install() {
        final VersionProvider current = Via.getManager().getProviders().get(VersionProvider.class);
        if (current instanceof LegacyVersionProvider) {
            return;
        }
        Via.getManager().getProviders().use(VersionProvider.class, new LegacyVersionProvider(current));
    }

    @Override
    public ProtocolVersion getClientProtocol(final UserConnection connection) {
        final LegacyClient client = connection.get(LegacyClient.class);
        if (client != null) {
            return client.version();
        }
        return this.parent != null ? this.parent.getClientProtocol(connection) : null;
    }

    @Override
    public ProtocolVersion getClosestServerProtocol(final UserConnection connection) throws Exception {
        if (this.parent != null) {
            return this.parent.getClosestServerProtocol(connection);
        }
        return Via.getAPI().getServerVersion().lowestSupportedProtocolVersion();
    }

}

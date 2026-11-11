package viaprenetty.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import viaprenetty.net.LegacyConnection;

public final class LegacyClient implements StorableObject {

    private final ProtocolVersion version;
    private final LegacyConnection connection;
    private int entityId = -1;

    public LegacyClient(final ProtocolVersion version, final LegacyConnection connection) {
        this.version = version;
        this.connection = connection;
    }

    public ProtocolVersion version() {
        return this.version;
    }

    public LegacyConnection connection() {
        return this.connection;
    }

    public int entityId() {
        return this.entityId;
    }

    public void setEntityId(final int entityId) {
        this.entityId = entityId;
    }

}

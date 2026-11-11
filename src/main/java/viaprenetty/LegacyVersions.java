package viaprenetty;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.SubVersionRange;
import com.viaversion.viaversion.api.protocol.version.VersionType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LegacyVersions {

    private static final Map<Integer, ProtocolVersion> BY_ID = new LinkedHashMap<>();

    public static final ProtocolVersion R1_4_6 = register(51, "1.4.6-1.4.7", new SubVersionRange("1.4", 6, 7));
    public static final ProtocolVersion R1_5_0 = register(60, "1.5-1.5.1", new SubVersionRange("1.5", 0, 1));
    public static final ProtocolVersion R1_5_2 = register(61, "1.5.2", null);
    public static final ProtocolVersion R1_6_1 = register(73, "1.6.1", null);
    public static final ProtocolVersion R1_6_2 = register(74, "1.6.2", null);
    public static final ProtocolVersion R1_6_4 = register(78, "1.6.4", null);

    private LegacyVersions() {
    }

    private static ProtocolVersion register(final int id, final String name, final SubVersionRange range) {
        ProtocolVersion existing = null;
        for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
            if (version.getVersionType() == VersionType.RELEASE_INITIAL && version.getVersion() == id) {
                existing = version;
                break;
            }
        }
        final ProtocolVersion version = existing != null ? existing : new ProtocolVersion(VersionType.RELEASE_INITIAL, id, -1, name, range);
        if (existing == null) {
            ProtocolVersion.register(version);
        }
        BY_ID.put(id, version);
        return version;
    }

    public static ProtocolVersion byProtocolId(final int id) {
        return BY_ID.get(id);
    }

    public static boolean isAtLeast(final ProtocolVersion version, final ProtocolVersion other) {
        return version.newerThanOrEqualTo(other);
    }

    public static void init() {
    }

}

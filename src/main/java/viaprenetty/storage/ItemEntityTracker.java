package viaprenetty.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ItemEntityTracker implements StorableObject {

    private final Map<Integer, int[]> pending = new HashMap<>();
    private final Set<Integer> items = new HashSet<>();

    public void pending(final int entityId, final int[] spawn) {
        this.pending.put(entityId, spawn);
        this.items.add(entityId);
    }

    public int[] takePending(final int entityId) {
        return this.pending.remove(entityId);
    }

    public boolean isItem(final int entityId) {
        return this.items.contains(entityId);
    }

    public void remove(final int entityId) {
        this.pending.remove(entityId);
        this.items.remove(entityId);
    }

    public void clear() {
        this.pending.clear();
        this.items.clear();
    }

}

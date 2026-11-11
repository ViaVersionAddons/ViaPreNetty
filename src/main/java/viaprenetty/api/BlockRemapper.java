package viaprenetty.api;

import java.util.Arrays;

public final class BlockRemapper {

    private final int[] table = new int[4096 * 16];
    private final boolean[] touchedIds = new boolean[4096];

    public BlockRemapper() {
        Arrays.fill(this.table, -1);
    }

    public BlockRemapper replace(final int id, final int newId, final int newMeta) {
        for (int meta = 0; meta < 16; meta++) {
            set(id, meta, newId, newMeta);
        }
        return this;
    }

    public BlockRemapper replaceKeepMeta(final int id, final int newId) {
        for (int meta = 0; meta < 16; meta++) {
            set(id, meta, newId, meta);
        }
        return this;
    }

    public BlockRemapper replaceMeta(final int id, final int meta, final int newId, final int newMeta) {
        set(id, meta, newId, newMeta);
        return this;
    }

    public BlockRemapper replaceMetaRange(final int id, final int fromMeta, final int toMeta, final int newId, final int newMeta) {
        for (int meta = fromMeta; meta <= toMeta; meta++) {
            set(id, meta, newId, newMeta);
        }
        return this;
    }

    public BlockRemapper replaceMasked(final int id, final int newId, final int keepMask, final int orValue) {
        for (int meta = 0; meta < 16; meta++) {
            set(id, meta, newId, (meta & keepMask) | orValue);
        }
        return this;
    }

    public BlockRemapper fallback(final java.util.function.IntPredicate valid, final int newId, final int newMeta) {
        for (int id = 0; id < 4096; id++) {
            if (valid.test(id)) {
                continue;
            }
            for (int meta = 0; meta < 16; meta++) {
                if (this.table[(id << 4) | meta] == -1) {
                    set(id, meta, newId, newMeta);
                }
            }
        }
        return this;
    }

    private void set(final int id, final int meta, final int newId, final int newMeta) {
        this.table[(id << 4) | meta] = (newId << 4) | (newMeta & 15);
        this.touchedIds[id] = true;
    }

    public boolean affects(final int id) {
        return id >= 0 && id < 4096 && this.touchedIds[id];
    }

    public int remap(final int raw) {
        if (raw < 0 || raw >= this.table.length) {
            return raw;
        }
        final int mapped = this.table[raw];
        return mapped == -1 ? raw : mapped;
    }

    public int remap(final int id, final int meta) {
        return remap((id << 4) | (meta & 15));
    }

}

package viaprenetty.api;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

public final class LegacyItemRemapper {

    private static final int[] LEGACY_ENCHANTMENTS = {0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 32, 33, 34, 35, 48, 49, 50, 51};

    private final String tagName;
    private final String versionName;
    private final List<Entry> entries = new ArrayList<>();
    private final boolean[] touched = new boolean[32768];
    private IntPredicate validItems = id -> true;
    private IntPredicate validEnchantments = id -> true;
    private int maxEffect = Integer.MAX_VALUE;

    public LegacyItemRemapper(final String tagName, final String versionName) {
        this.tagName = "VPN|" + tagName;
        this.versionName = versionName;
    }

    public LegacyItemRemapper validItems(final IntPredicate predicate) {
        this.validItems = predicate;
        return this;
    }

    public LegacyItemRemapper legacyEnchantments() {
        this.validEnchantments = id -> {
            for (int allowed : LEGACY_ENCHANTMENTS) {
                if (allowed == id) {
                    return true;
                }
            }
            return false;
        };
        return this;
    }

    public LegacyItemRemapper maxEffect(final int maxEffect) {
        this.maxEffect = maxEffect;
        return this;
    }

    public LegacyItemRemapper map(final int id, final int newId, final String name) {
        return add(id, -1, newId, -1, name);
    }

    public LegacyItemRemapper map(final int id, final int newId, final int newMeta, final String name) {
        return add(id, -1, newId, newMeta, name);
    }

    public LegacyItemRemapper mapMeta(final int id, final int meta, final int newId, final int newMeta, final String name) {
        return add(id, meta, newId, newMeta, name);
    }

    public LegacyItemRemapper mapMetaRange(final int id, final int fromMeta, final int toMeta, final int newId, final int newMeta, final String name) {
        for (int meta = fromMeta; meta <= toMeta; meta++) {
            add(id, meta, newId, newMeta, name);
        }
        return this;
    }

    private LegacyItemRemapper add(final int id, final int meta, final int newId, final int newMeta, final String name) {
        this.entries.add(new Entry(id, meta, newId, newMeta, name));
        if (id >= 0 && id < this.touched.length) {
            this.touched[id] = true;
        }
        return this;
    }

    public Item toClient(final Item item) {
        if (item == null) {
            return null;
        }
        final int id = item.identifier();
        boolean remapped = false;
        if (id >= 0 && id < this.touched.length && this.touched[id]) {
            for (Entry entry : this.entries) {
                if (entry.id == id && (entry.meta == -1 || entry.meta == item.data())) {
                    rename(item, backup(item), entry.name);
                    item.setIdentifier(entry.newId);
                    if (entry.newMeta != -1) {
                        item.setData((short) entry.newMeta);
                    }
                    remapped = true;
                    break;
                }
            }
        }
        if (!remapped && !this.validItems.test(item.identifier())) {
            rename(item, backup(item), "Unknown Item");
            item.setIdentifier(1);
            item.setData((short) 0);
        }
        final CompoundTag tag = item.tag();
        if (tag != null) {
            filterList(item, tag, "ench", "id", this.validEnchantments);
            filterList(item, tag, "StoredEnchantments", "id", this.validEnchantments);
            filterList(item, tag, "CustomPotionEffects", "Id", effect -> (effect & 0xFF) >= 1 && (effect & 0xFF) <= this.maxEffect);
        }
        return item;
    }

    public Item toServer(final Item item) {
        if (item == null) {
            return null;
        }
        final CompoundTag tag = item.tag();
        if (tag == null) {
            return item;
        }
        final CompoundTag stored = tag.getCompoundTag(this.tagName);
        if (stored == null) {
            return item;
        }
        tag.remove(this.tagName);
        final NumberTag storedId = stored.getNumberTag("Id");
        final NumberTag storedMeta = stored.getNumberTag("Meta");
        if (storedId != null) {
            item.setIdentifier(storedId.asInt());
        }
        if (storedMeta != null) {
            item.setData(storedMeta.asShort());
        }
        final CompoundTag lists = stored.getCompoundTag("Lists");
        if (lists != null) {
            for (String key : lists.keySet()) {
                final Tag original = lists.get(key);
                if (original != null) {
                    tag.put(key, original);
                }
            }
        }
        final CompoundTag display = tag.getCompoundTag("display");
        if (display != null) {
            if (stored.contains("RemoveLastLore")) {
                final ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
                if (lore != null && lore.size() > 0) {
                    lore.remove(lore.get(lore.size() - 1));
                }
            }
            if (stored.contains("RemoveLore")) {
                display.remove("Lore");
            }
            if (stored.contains("RemoveName")) {
                display.remove("Name");
            }
            if (stored.contains("RemoveDisplay")) {
                tag.remove("display");
            }
        }
        if (stored.contains("RemoveTag")) {
            item.setTag(null);
        }
        return item;
    }

    private CompoundTag backup(final Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) {
            tag = new CompoundTag();
            item.setTag(tag);
            final CompoundTag stored = new CompoundTag();
            stored.putBoolean("RemoveTag", true);
            tag.put(this.tagName, stored);
        }
        CompoundTag stored = tag.getCompoundTag(this.tagName);
        if (stored == null) {
            stored = new CompoundTag();
            tag.put(this.tagName, stored);
        }
        if (!stored.contains("Id")) {
            stored.putInt("Id", item.identifier());
            stored.putShort("Meta", item.data());
        }
        return stored;
    }

    private void rename(final Item item, final CompoundTag stored, final String name) {
        final CompoundTag tag = item.tag();
        CompoundTag display = tag.getCompoundTag("display");
        if (display == null) {
            display = new CompoundTag();
            tag.put("display", display);
            stored.putBoolean("RemoveDisplay", true);
        }
        if (display.contains("Name")) {
            ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
            if (lore == null) {
                lore = new ListTag<>(StringTag.class);
                display.put("Lore", lore);
                stored.putBoolean("RemoveLore", true);
            }
            lore.add(new StringTag("§7" + this.versionName + ": " + name));
            stored.putBoolean("RemoveLastLore", true);
        } else {
            display.putString("Name", "§r" + name);
            stored.putBoolean("RemoveName", true);
        }
    }

    private void filterList(final Item item, final CompoundTag tag, final String key, final String idKey, final IntPredicate valid) {
        final Tag raw = tag.get(key);
        if (!(raw instanceof ListTag<?> list)) {
            return;
        }
        boolean invalid = false;
        for (Tag entry : list) {
            if (!(entry instanceof CompoundTag compound) || !(compound.get(idKey) instanceof NumberTag number) || !valid.test(number.asInt())) {
                invalid = true;
                break;
            }
        }
        if (!invalid) {
            return;
        }
        final CompoundTag stored = backup(item);
        CompoundTag lists = stored.getCompoundTag("Lists");
        if (lists == null) {
            lists = new CompoundTag();
            stored.put("Lists", lists);
        }
        lists.put(key, list.copy());
        final ListTag<CompoundTag> filtered = new ListTag<>(CompoundTag.class);
        for (Tag entry : list) {
            if (entry instanceof CompoundTag compound && compound.get(idKey) instanceof NumberTag number && valid.test(number.asInt())) {
                filtered.add(compound);
            }
        }
        if (filtered.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, filtered);
        }
    }

    private record Entry(int id, int meta, int newId, int newMeta, String name) {
    }

}

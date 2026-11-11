package viaprenetty.data;

import viaprenetty.api.BlockRemapper;
import viaprenetty.api.LegacyItemRemapper;

public final class LegacyMappings {

    public static final BlockRemapper BLOCKS_1_6 = new BlockRemapper()
        .replace(95, 20, 0)
        .replace(160, 102, 0)
        .replaceMasked(161, 18, 0xC, 0)
        .replaceMasked(162, 17, 0xC, 0)
        .replaceKeepMeta(163, 53)
        .replaceKeepMeta(164, 53)
        .replace(174, 79, 0)
        .replaceMetaRange(175, 0, 7, 31, 1)
        .replaceMetaRange(175, 8, 15, 0, 0)
        .replaceMetaRange(38, 1, 15, 38, 0)
        .replaceMetaRange(3, 1, 15, 3, 0)
        .replaceMetaRange(12, 1, 15, 12, 0)
        .replaceMetaRange(5, 4, 15, 5, 0)
        .replaceMetaRange(6, 4, 7, 6, 0)
        .replaceMetaRange(6, 12, 15, 6, 8)
        .replaceMetaRange(126, 4, 7, 126, 0)
        .replaceMetaRange(126, 12, 15, 126, 8)
        .replaceMetaRange(125, 4, 15, 125, 0)
        .replaceMetaRange(97, 3, 15, 97, 2)
        .fallback(LegacyMappings::validBlock16, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_6 = new LegacyItemRemapper("1_6", "1.7")
        .map(95, 20, 0, "Stained Glass")
        .map(160, 102, 0, "Stained Glass Pane")
        .map(161, 18, 0, "Acacia/Dark Oak Leaves")
        .map(162, 17, 0, "Acacia/Dark Oak Wood")
        .map(163, 53, 0, "Acacia Stairs")
        .map(164, 53, 0, "Dark Oak Stairs")
        .map(174, 79, 0, "Packed Ice")
        .map(175, 31, 1, "Large Flower")
        .mapMetaRange(38, 1, 8, 38, 0, "Flower")
        .mapMetaRange(3, 1, 2, 3, 0, "Podzol")
        .mapMeta(12, 1, 12, 0, "Red Sand")
        .mapMeta(5, 4, 5, 0, "Acacia Planks")
        .mapMeta(5, 5, 5, 0, "Dark Oak Planks")
        .mapMeta(6, 4, 6, 0, "Acacia Sapling")
        .mapMeta(6, 5, 6, 0, "Dark Oak Sapling")
        .mapMeta(126, 4, 126, 0, "Acacia Slab")
        .mapMeta(126, 5, 126, 0, "Dark Oak Slab")
        .mapMetaRange(97, 3, 5, 97, 2, "Monster Egg")
        .mapMetaRange(349, 1, 3, 349, 0, "Fish")
        .mapMeta(350, 1, 350, 0, "Cooked Salmon")
        .map(422, 328, 0, "Minecart with Command Block")
        .validItems(id -> validBlock16(id) || (id >= 256 && id <= 408) || (id >= 417 && id <= 421) || (id >= 2256 && id <= 2267))
        .legacyEnchantments()
        .maxEffect(23);

    public static final BlockRemapper BLOCKS_1_5 = new BlockRemapper()
        .replaceKeepMeta(159, 35)
        .replace(170, 35, 4)
        .replace(171, 0, 0)
        .replace(172, 35, 12)
        .replace(173, 35, 15)
        .fallback(id -> id >= 0 && id <= 158, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_5 = new LegacyItemRemapper("1_5", "1.6")
        .map(159, 35, -1, "Stained Clay")
        .map(170, 35, 4, "Hay Bale")
        .map(171, 70, 0, "Carpet")
        .map(172, 35, 12, "Hardened Clay")
        .map(173, 35, 15, "Block of Coal")
        .map(417, 265, 0, "Iron Horse Armor")
        .map(418, 266, 0, "Gold Horse Armor")
        .map(419, 264, 0, "Diamond Horse Armor")
        .map(420, 287, 0, "Lead")
        .map(421, 339, 0, "Name Tag")
        .mapMeta(383, 100, 383, 92, "Horse Spawn Egg")
        .validItems(id -> (id >= 0 && id <= 158) || (id >= 256 && id <= 408) || (id >= 2256 && id <= 2267))
        .maxEffect(20);

    public static final BlockRemapper BLOCKS_1_4 = new BlockRemapper()
        .replaceMeta(43, 7, 43, 0)
        .replaceMeta(43, 15, 43, 0)
        .replaceMeta(44, 7, 44, 0)
        .replaceMeta(44, 15, 44, 8)
        .replaceKeepMeta(146, 54)
        .replace(147, 70, 0)
        .replace(148, 70, 0)
        .replaceKeepMeta(149, 93)
        .replaceKeepMeta(150, 94)
        .replace(151, 44, 0)
        .replace(152, 35, 14)
        .replace(153, 87, 0)
        .replace(154, 118, 0)
        .replace(155, 80, 0)
        .replaceKeepMeta(156, 109)
        .replaceKeepMeta(157, 27)
        .replaceKeepMeta(158, 23)
        .fallback(id -> id >= 0 && id <= 145, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_4 = new LegacyItemRemapper("1_4", "1.5")
        .mapMeta(43, 7, 43, 0, "Quartz Double Slab")
        .mapMeta(44, 7, 44, 0, "Quartz Slab")
        .map(146, 54, 0, "Trapped Chest")
        .map(147, 70, 0, "Weighted Pressure Plate (Light)")
        .map(148, 70, 0, "Weighted Pressure Plate (Heavy)")
        .map(151, 44, 0, "Daylight Sensor")
        .map(152, 35, 14, "Block of Redstone")
        .map(153, 87, 0, "Nether Quartz Ore")
        .map(154, 380, 0, "Hopper")
        .map(155, 80, 0, "Block of Quartz")
        .map(156, 109, 0, "Quartz Stairs")
        .map(157, 27, 0, "Activator Rail")
        .map(158, 23, 0, "Dropper")
        .map(404, 356, 0, "Redstone Comparator")
        .map(405, 336, 0, "Nether Brick")
        .map(406, 318, 0, "Nether Quartz")
        .map(407, 328, 0, "Minecart with TNT")
        .map(408, 328, 0, "Minecart with Hopper")
        .validItems(id -> (id >= 0 && id <= 145) || (id >= 256 && id <= 403) || (id >= 2256 && id <= 2267))
        .maxEffect(20);

    private LegacyMappings() {
    }

    private static boolean validBlock16(final int id) {
        return (id >= 0 && id <= 159) || (id >= 170 && id <= 173);
    }

}

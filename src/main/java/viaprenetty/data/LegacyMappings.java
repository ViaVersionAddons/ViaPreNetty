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

    public static final BlockRemapper BLOCKS_1_4_4 = new BlockRemapper()
        .replaceMeta(43, 6, 112, 0)
        .replaceMeta(44, 6, 44, 0)
        .replaceMeta(44, 14, 44, 8)
        .fallback(id -> id >= 0 && id <= 145, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_4_4 = new LegacyItemRemapper("1_4_4", "1.4.6")
        .mapMeta(43, 6, 112, 0, "Nether Brick Double Slab")
        .mapMeta(44, 6, 44, 0, "Nether Brick Slab")
        .map(401, 289, 0, "Firework Rocket")
        .map(402, 351, 8, "Firework Star")
        .map(403, 340, 0, "Enchanted Book")
        .validItems(id -> (id >= 0 && id <= 145) || (id >= 256 && id <= 400) || (id >= 2256 && id <= 2267))
        .maxEffect(20);

    public static final BlockRemapper BLOCKS_1_4_2 = new BlockRemapper()
        .fallback(id -> id >= 0 && id <= 145, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_4_2 = new LegacyItemRemapper("1_4_2", "1.4.4")
        .map(2267, 2256, 0, "Music Disc (wait)")
        .validItems(id -> (id >= 0 && id <= 145) || (id >= 256 && id <= 400) || (id >= 2256 && id <= 2266))
        .maxEffect(20);

    public static final BlockRemapper BLOCKS_1_3 = new BlockRemapper()
        .replace(137, 1, 0)
        .replace(138, 20, 0)
        .replaceMeta(139, 0, 4, 0)
        .replaceMetaRange(139, 1, 15, 48, 0)
        .replace(140, 0, 0)
        .replaceKeepMeta(141, 59)
        .replaceKeepMeta(142, 59)
        .replaceKeepMeta(143, 77)
        .replace(144, 0, 0)
        .replace(145, 42, 0)
        .fallback(id -> id >= 0 && id <= 136, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_3 = new LegacyItemRemapper("1_3", "1.4.2")
        .map(137, 1, 0, "Command Block")
        .map(138, 20, 0, "Beacon")
        .mapMeta(139, 0, 4, 0, "Cobblestone Wall")
        .mapMeta(139, 1, 48, 0, "Mossy Cobblestone Wall")
        .map(143, 77, 0, "Wooden Button")
        .map(144, 352, 0, "Skull")
        .map(145, 42, 0, "Anvil")
        .map(389, 321, 0, "Item Frame")
        .map(390, 336, 0, "Flower Pot")
        .map(391, 260, 0, "Carrot")
        .map(392, 260, 0, "Potato")
        .map(393, 297, 0, "Baked Potato")
        .map(394, 260, 0, "Poisonous Potato")
        .map(395, 358, 0, "Empty Map")
        .map(396, 322, 0, "Golden Carrot")
        .map(397, 352, 0, "Skull")
        .map(398, 346, 0, "Carrot on a Stick")
        .map(399, 264, 0, "Nether Star")
        .map(400, 357, 0, "Pumpkin Pie")
        .mapMeta(383, 65, 383, 90, "Spawn Bat")
        .mapMeta(383, 66, 383, 90, "Spawn Witch")
        .validItems(id -> (id >= 0 && id <= 136) || (id >= 256 && id <= 388) || (id >= 2256 && id <= 2266))
        .maxEffect(19);

    public static final BlockRemapper BLOCKS_1_2 = new BlockRemapper()
        .replaceMasked(17, 17, 0x3, 0)
        .replace(125, 43, 2)
        .replace(126, 44, 2)
        .replace(127, 0, 0)
        .replaceKeepMeta(128, 53)
        .replace(129, 1, 0)
        .replaceKeepMeta(130, 54)
        .replace(131, 0, 0)
        .replace(132, 0, 0)
        .replace(133, 42, 0)
        .replaceKeepMeta(134, 53)
        .replaceKeepMeta(135, 53)
        .replaceKeepMeta(136, 53)
        .fallback(id -> id >= 0 && id <= 124, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_2 = new LegacyItemRemapper("1_2", "1.3")
        .map(125, 44, 2, "Wooden Double Slab")
        .map(126, 44, 2, "Wooden Slab")
        .map(127, 351, 3, "Cocoa")
        .map(128, 53, 0, "Sandstone Stairs")
        .map(129, 1, 0, "Emerald Ore")
        .map(130, 54, 0, "Ender Chest")
        .map(131, 69, 0, "Tripwire Hook")
        .map(132, 287, 0, "Tripwire")
        .map(133, 42, 0, "Block of Emerald")
        .map(134, 53, 0, "Spruce Stairs")
        .map(135, 53, 0, "Birch Stairs")
        .map(136, 53, 0, "Jungle Stairs")
        .mapMeta(322, 1, 322, 0, "Enchanted Golden Apple")
        .map(386, 340, 0, "Book and Quill")
        .map(387, 340, 0, "Written Book")
        .map(388, 264, 0, "Emerald")
        .validItems(id -> (id >= 0 && id <= 124) || (id >= 256 && id <= 385) || (id >= 2256 && id <= 2266))
        .maxEffect(19);

    public static final BlockRemapper BLOCKS_1_2_1 = new BlockRemapper()
        .replaceMetaRange(5, 1, 15, 5, 0)
        .replaceMetaRange(24, 1, 15, 24, 0)
        .fallback(id -> id >= 0 && id <= 124, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_2_1 = new LegacyItemRemapper("1_2_1", "1.2.4")
        .mapMeta(5, 1, 5, 0, "Spruce Planks")
        .mapMeta(5, 2, 5, 0, "Birch Planks")
        .mapMeta(5, 3, 5, 0, "Jungle Planks")
        .mapMeta(24, 1, 24, 0, "Chiseled Sandstone")
        .mapMeta(24, 2, 24, 0, "Smooth Sandstone")
        .validItems(id -> (id >= 0 && id <= 124) || (id >= 256 && id <= 385) || (id >= 2256 && id <= 2266))
        .maxEffect(19);

    public static final BlockRemapper BLOCKS_1_1 = new BlockRemapper()
        .replaceMeta(6, 3, 6, 0)
        .replaceMeta(17, 3, 17, 0)
        .replaceMeta(18, 3, 18, 0)
        .replaceMeta(18, 7, 18, 4)
        .replaceMeta(18, 11, 18, 8)
        .replaceMeta(18, 15, 18, 12)
        .replaceMeta(98, 3, 98, 0)
        .replace(123, 89, 0)
        .replace(124, 89, 0)
        .fallback(id -> id >= 0 && id <= 122, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_1 = new LegacyItemRemapper("1_1", "1.2")
        .mapMeta(6, 3, 6, 0, "Jungle Sapling")
        .mapMeta(17, 3, 17, 0, "Jungle Wood")
        .mapMeta(18, 3, 18, 0, "Jungle Leaves")
        .mapMeta(98, 3, 98, 0, "Chiseled Stone Bricks")
        .map(123, 89, 0, "Redstone Lamp")
        .map(124, 89, 0, "Redstone Lamp")
        .map(384, 373, 0, "Bottle o' Enchanting")
        .map(385, 341, 0, "Fire Charge")
        .mapMeta(383, 98, 383, 90, "Spawn Ocelot")
        .validItems(id -> (id >= 0 && id <= 122) || (id >= 256 && id <= 383) || (id >= 2256 && id <= 2266))
        .maxEffect(19);

    public static final BlockRemapper BLOCKS_1_0 = new BlockRemapper()
        .fallback(id -> id >= 0 && id <= 122, 1, 0);

    public static final LegacyItemRemapper ITEMS_1_0 = new LegacyItemRemapper("1_0", "1.1")
        .map(383, 344, 0, "Spawn Egg")
        .validItems(id -> (id >= 0 && id <= 122) || (id >= 256 && id <= 382) || (id >= 2256 && id <= 2266))
        .validEnchantments(id -> (id >= 0 && id <= 7) || (id >= 16 && id <= 21) || (id >= 32 && id <= 35))
        .maxEffect(19);

    private LegacyMappings() {
    }

    private static boolean validBlock16(final int id) {
        return (id >= 0 && id <= 159) || (id >= 170 && id <= 173);
    }

}

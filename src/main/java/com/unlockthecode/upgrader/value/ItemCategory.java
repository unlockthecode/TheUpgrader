package com.unlockthecode.upgrader.value;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Categories for organizing items in the catalog.
 * Loaded from config.yml - order matters for tab order.
 */
public final class ItemCategory {

    private static final Logger LOGGER = Logger.getLogger("TheUpgrader");

    private final String id;
    private final String label;
    private final Material icon;
    private final boolean isAll;
    private final Set<Material> explicitItems; // null = auto-classify using built-in rules

    private ItemCategory(String id, String label, Material icon, boolean isAll, Set<Material> explicitItems) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.isAll = isAll;
        this.explicitItems = explicitItems;
    }

    public String id() { return id; }
    public String label() { return label; }
    public Material icon() { return icon; }
    public boolean isAll() { return isAll; }

    public boolean contains(Material material) {
        if (material == null || material == Material.AIR) return false;
        if (isAll) return true;
        if (explicitItems != null) return explicitItems.contains(material);
        return classifyByBuiltinRules(material);
    }

    public static ItemCategory fromConfig(Map<?, ?> raw) {
        String id = raw.get("id") != null ? String.valueOf(raw.get("id")) : "";
        String label = raw.get("name") != null ? String.valueOf(raw.get("name")) : "";
        String iconStr = raw.get("icon") != null ? String.valueOf(raw.get("icon")) : "";

        if (id.isBlank() || label.isBlank() || iconStr.isBlank()) {
            return null;
        }

        Material icon = Material.matchMaterial(iconStr);
        if (icon == null) {
            LOGGER.warning(String.format("Unknown material for category icon: %s", iconStr));
            return null;
        }

        boolean isAll = "all".equalsIgnoreCase(id);
        Set<Material> explicitItems = null;

        Object itemsObj = raw.get("items");
        if (itemsObj instanceof List<?> itemsList) {
            explicitItems = new HashSet<>();
            for (Object itemObj : itemsList) {
                String matName = String.valueOf(itemObj).trim();
                Material m = Material.matchMaterial(matName);
                if (m != null) {
                    explicitItems.add(m);
                } else {
                    LOGGER.warning(String.format("Unknown material in category '%s' items list: %s", id, matName));
                }
            }
        }

        return new ItemCategory(id, label, icon, isAll, explicitItems);
    }

    public static List<ItemCategory> defaults() {
        List<ItemCategory> list = new ArrayList<>();
        list.add(new ItemCategory("all", "All Items", Material.CHEST, true, null));
        list.add(new ItemCategory("blocks", "Blocks", Material.GRASS_BLOCK, false, null));
        list.add(new ItemCategory("minerals", "Ores & Ingots", Material.GOLD_INGOT, false, null));
        list.add(new ItemCategory("tools", "Tools & Utility", Material.IRON_PICKAXE, false, null));
        list.add(new ItemCategory("combat", "Weapons & Armor", Material.DIAMOND_SWORD, false, null));
        list.add(new ItemCategory("food", "Food", Material.COOKED_BEEF, false, null));
        list.add(new ItemCategory("farming", "Farming & Nature", Material.WHEAT, false, null));
        list.add(new ItemCategory("redstone", "Redstone & Rails", Material.REDSTONE, false, null));
        list.add(new ItemCategory("misc", "Everything Else", Material.PAPER, false, null));
        return list;
    }

    private boolean classifyByBuiltinRules(Material material) {
        // Food first (most specific)
        if (isFood(material)) return id.equals("food");

        // Combat (weapons/armor)
        if (isCombat(material)) return id.equals("combat");

        // Tools
        if (isTool(material)) return id.equals("tools");

        // Minerals (ores, ingots, nuggets, raw materials, gems)
        if (isMineral(material)) return id.equals("minerals");

        // Redstone-related
        if (isRedstone(material)) return id.equals("redstone");

        // Farming & nature
        if (isFarming(material)) return id.equals("farming");

        // Blocks
        if (material.isBlock() && !material.isAir()) return id.equals("blocks");

        // Everything else
        return id.equals("misc");
    }

    private static boolean isFood(Material material) {
        String name = material.name();
        if (material.isEdible()) return true;
        return name.equals("CAKE") ||
               name.equals("PUMPKIN_PIE") ||
               name.equals("COOKIE") ||
               name.equals("BREAD") ||
               name.equals("GOLDEN_APPLE") ||
               name.equals("ENCHANTED_GOLDEN_APPLE") ||
               name.equals("SUSPICIOUS_STEW") ||
               name.equals("MUSHROOM_STEW") ||
               name.equals("BEETROOT_SOUP") ||
               name.equals("RABBIT_STEW") ||
               name.equals("MILK_BUCKET") ||
               name.equals("POTATO") ||
               name.equals("CARROT") ||
               name.equals("BEETROOT") ||
               name.equals("POISONOUS_POTATO") ||
               name.equals("SPIDER_EYE") ||
               name.equals("FERMENTED_SPIDER_EYE") ||
               name.equals("MAGMA_CREAM") ||
               name.equals("GHAST_TEAR") ||
               name.equals("SUGAR") ||
               name.equals("MELON_SLICE") ||
               name.equals("APPLE") ||
               name.equals("GOLDEN_CARROT") ||
               name.equals("PECTIN") ||
               name.equals("HONEY_BOTTLE") ||
               name.equals("HONEYCOMB") ||
               name.equals("BEE_NEST") ||
               name.equals("BEEHIVE");
    }

    private static boolean isCombat(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") ||
               name.endsWith("_SPEAR") ||
               name.endsWith("_AXE") ||
               name.equals("BOW") ||
               name.equals("CROSSBOW") ||
               name.equals("TRIDENT") ||
               name.equals("MACE") ||
               name.equals("SHIELD") ||
               name.endsWith("_HELMET") ||
               name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") ||
               name.endsWith("_BOOTS") ||
               name.endsWith("_ARMOR") ||
               name.equals("ELYTRA") ||
               name.equals("TURTLE_HELMET") ||
               name.equals("NETHERITE_UPGRADE_SMITHING_TEMPLATE") ||
               name.endsWith("_HORSE_ARMOR") ||
               name.endsWith("_SADDLE");
    }

    private static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") ||
               name.endsWith("_SHOVEL") ||
               name.endsWith("_AXE") ||
               name.endsWith("_HOE") ||
               name.equals("SHEARS") ||
               name.equals("FLINT_AND_STEEL") ||
               name.equals("FISHING_ROD") ||
               name.equals("COMPASS") ||
               name.equals("RECOVERY_COMPASS") ||
               name.equals("CLOCK") ||
               name.equals("SPYGLASS") ||
               name.equals("BRUSH") ||
               name.equals("LEAD") ||
               name.equals("NAME_TAG") ||
               name.equals("SADDLE") ||
               name.equals("CARROT_ON_A_STICK") ||
               name.equals("WARPED_FUNGUS_ON_A_STICK");
    }

    private static boolean isMineral(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") ||
               name.endsWith("_INGOT") ||
               name.endsWith("_NUGGET") ||
               name.startsWith("RAW_") ||
               name.equals("COAL") ||
               name.equals("CHARCOAL") ||
               name.equals("DIAMOND") ||
               name.equals("EMERALD") ||
               name.equals("LAPIS_LAZULI") ||
               name.equals("QUARTZ") ||
               name.equals("AMETHYST_CLUSTER") ||
               name.equals("AMETHYST_SHARD") ||
               name.equals("NETHERITE_SCRAP") ||
               name.equals("NETHERITE_INGOT") ||
               name.equals("GLOWSTONE_DUST") ||
               name.equals("ECHO_SHARD") ||
               name.equals("PRISMARINE_SHARD") ||
               name.equals("PRISMARINE_CRYSTALS") ||
               name.equals("PRISMARINE_BRICKS") ||
               name.equals("PRISMARINE_TILES") ||
               name.equals("DARK_PRISMARINE");
    }

    private static boolean isRedstone(Material material) {
        String name = material.name();
        return name.equals("REDSTONE") ||
               name.equals("REDSTONE_BLOCK") ||
               name.equals("REDSTONE_TORCH") ||
               name.equals("REDSTONE_WALL_TORCH") ||
               name.equals("REDSTONE_LAMP") ||
               name.equals("OBSERVER") ||
               name.equals("DISPENSER") ||
               name.equals("DROPPER") ||
               name.equals("HOPPER") ||
               name.equals("HOPPER_MINECART") ||
               name.equals("POWERED_RAIL") ||
               name.equals("ACTIVATOR_RAIL") ||
               name.equals("DETECTOR_RAIL") ||
               name.equals("RAIL") ||
               name.equals("DAYLIGHT_DETECTOR") ||
               name.equals("TRIPWIRE_HOOK") ||
               name.equals("TARGET_BLOCK") ||
               name.equals("PISTON") ||
               name.equals("STICKY_PISTON") ||
               name.equals("PISTON_HEAD") ||
               name.equals("STICKY_PISTON_HEAD") ||
               name.equals("COMPARATOR") ||
               name.equals("MINECART") ||
               name.equals("CHEST_MINECART") ||
               name.equals("FURNACE_MINECART") ||
               name.equals("TNT") ||
               name.equals("TNT_MINECART");
    }

    private static boolean isFarming(Material material) {
        String name = material.name();
        return name.endsWith("_SEEDS") ||
               name.equals("WHEAT") ||
               name.equals("SUGAR_CANE") ||
               name.equals("CACTUS") ||
               name.equals("BAMBOO") ||
               name.equals("KELP") ||
               name.equals("PITCHER_POD") ||
               name.equals("COCOA_BEANS") ||
               name.equals("RED_MUSHROOM") ||
               name.equals("BROWN_MUSHROOM") ||
               name.equals("NETHER_WART") ||
               name.equals("CHORUS_FRUIT") ||
               name.equals("MELON") ||
               name.equals("PUMPKIN") ||
               name.equals("MELON_STEM") ||
               name.equals("PUMPKIN_STEM") ||
               name.equals("BEETROOT") ||
               name.equals("CARROT") ||
               name.equals("POTATO") ||
               name.equals("SWEET_BERRY_BUSH") ||
               name.equals("GLOW_BERRIES") ||
               name.equals("AZALEA") ||
               name.equals("FLOWERING_AZALEA") ||
               name.equals("ROSE_BUSH") ||
               name.equals("PEONY") ||
               name.equals("LILAC") ||
               name.equals("HYDRANGEA") ||
               name.equals("SUNFLOWER") ||
               name.equals("LILY_OF_THE_VALLEY") ||
               name.equals("ORANGE_TULIP") ||
               name.equals("PINK_TULIP") ||
               name.equals("RED_TULIP") ||
               name.equals("WHITE_TULIP") ||
               name.equals("BLUE_ORCHID") ||
               name.equals("ALLIUM") ||
               name.equals("BLUESTAR") ||
               name.equals("DAISY") ||
               name.equals("POPPY") ||
               name.equals("OXEYE_DAISY") ||
               name.equals("TULIP");
    }
}
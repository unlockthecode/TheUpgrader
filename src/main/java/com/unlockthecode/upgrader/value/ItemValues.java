package com.unlockthecode.upgrader.value;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.config.UpgraderConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemValues {

    private static final Logger LOGGER = Logger.getLogger("TheUpgrader");

    private final UpgraderConfig config;

    // Hardcoded base values as fallback when config has no custom base-values
    private static final Map<Material, Double> DEFAULT_BASE_VALUES = new HashMap<>();
    private static final Set<Material> BUILTIN_BLACKLIST = new HashSet<>();

    // Computed values cache
    private volatile Map<Material, Double> computedValues = null;
    private volatile List<Material> cachedCatalog = null;
    private final Map<ItemCategory, List<Material>> categoryCatalogs = new HashMap<>();

    // The active instance, so the static invalidateCache() can reach it
    private static volatile ItemValues activeInstance;

    static {
        // Initialize default base values (fallback when config has no custom base-values)
        // Rebalanced to match config.yml server economy prices
        putDefault(0.5, Material.DIRT, Material.COBBLESTONE, Material.COBBLED_DEEPSLATE, Material.SAND, Material.RED_SAND,
                Material.GRAVEL, Material.NETHERRACK, Material.END_STONE);
        putDefault(1.0, Material.STONE, Material.DEEPSLATE, Material.FLINT, Material.BAMBOO, Material.CACTUS,
                Material.SUGAR_CANE, Material.SOUL_SAND, Material.SOUL_SOIL,
                Material.CLAY_BALL, Material.MOSS_BLOCK, Material.ROOTED_DIRT, Material.MUD, Material.PACKED_MUD);
        putDefault(0.1, Material.SHORT_GRASS, Material.FERN, Material.DEAD_BUSH, Material.SEAGRASS, Material.TALL_SEAGRASS);
        putDefault(0.5, Material.SNOWBALL, Material.WHEAT_SEEDS);

        putDefault(2.0, Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
                Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.CRIMSON_STEM, Material.WARPED_STEM, Material.BAMBOO_BLOCK);
        putDefault(2.0, Material.WHEAT, Material.POTATO, Material.CARROT, Material.BEETROOT, Material.MELON_SLICE,
                Material.SWEET_BERRIES, Material.KELP);
        putDefault(3.0, Material.APPLE, Material.EGG, Material.GLOW_BERRIES, Material.CHORUS_FRUIT);
        putDefault(2.0, Material.FEATHER);
        putDefault(3.0, Material.STRING, Material.BONE, Material.SPIDER_EYE);
        putDefault(1.0, Material.ROTTEN_FLESH);
        putDefault(2.0, Material.INK_SAC);
        putDefault(4.0, Material.PHANTOM_MEMBRANE);

        putDefault(4.0, Material.PORKCHOP, Material.BEEF, Material.MUTTON);
        putDefault(3.0, Material.CHICKEN, Material.COD, Material.SALMON, Material.RABBIT);
        putDefault(2.0, Material.TROPICAL_FISH);
        putDefault(4.0, Material.PUFFERFISH);
        putDefault(5.0, Material.COAL);
        putDefault(4.0, Material.CHARCOAL);
        putDefault(6.0, Material.REDSTONE, Material.GLOWSTONE_DUST);
        putDefault(8.0, Material.QUARTZ);
        putDefault(4.0, Material.ICE, Material.FROSTED_ICE);
        putDefault(12.0, Material.LAPIS_LAZULI);
        putDefault(5.0, Material.LEATHER);
        putDefault(8.0, Material.SLIME_BALL, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS);
        putDefault(2.0, Material.RABBIT_HIDE);
        putDefault(12.0, Material.TURTLE_SCUTE);
        putDefault(8.0, Material.COPPER_INGOT);
        putDefault(6.0, Material.RAW_COPPER);
        putDefault(10.0, Material.AMETHYST_SHARD, Material.RAW_IRON);
        putDefault(15.0, Material.RAW_GOLD);

        putDefault(8.0, Material.HANGING_ROOTS);
        putDefault(12.0, Material.GUNPOWDER, Material.GLOW_INK_SAC);
        putDefault(15.0, Material.MAGMA_CREAM);
        putDefault(18.0, Material.BLAZE_POWDER);
        putDefault(10.0, Material.PACKED_ICE);
        putDefault(20.0, Material.BLUE_ICE);
        putDefault(8.0, Material.BIG_DRIPLEAF, Material.SMALL_DRIPLEAF);
        putDefault(15.0, Material.HONEYCOMB);
        putDefault(5.0, Material.MOSS_CARPET);
        putDefault(18.0, Material.IRON_INGOT);
        putDefault(8.0, Material.MANGROVE_ROOTS, Material.MUDDY_MANGROVE_ROOTS);
        putDefault(15.0, Material.OBSIDIAN);

        putDefault(25.0, Material.ENDER_PEARL, Material.RABBIT_FOOT);
        putDefault(30.0, Material.BLAZE_ROD);
        putDefault(35.0, Material.NAUTILUS_SHELL);
        putDefault(60.0, Material.HEART_OF_THE_SEA);
        putDefault(35.0, Material.GOLD_INGOT);
        putDefault(40.0, Material.WIND_CHARGE);
        putDefault(50.0, Material.GHAST_TEAR);
        putDefault(40.0, Material.SHULKER_SHELL);
        putDefault(80.0, Material.EMERALD);

        putDefault(100.0, Material.BREEZE_ROD, Material.SADDLE, Material.NAME_TAG);
        putDefault(150.0, Material.ECHO_SHARD);
        putDefault(120.0, Material.SPONGE);
        putDefault(180.0, Material.DIAMOND);
        putDefault(300.0, Material.HEAVY_CORE);
        putDefault(500.0, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        putDefault(700.0, Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        putDefault(800.0, Material.WITHER_SKELETON_SKULL, Material.TRIDENT);
        putDefault(1200.0, Material.TOTEM_OF_UNDYING);

        putDefault(3000.0, Material.ENCHANTED_GOLDEN_APPLE, Material.NETHERITE_INGOT);
        putDefault(4000.0, Material.NETHER_STAR);
        putDefault(5000.0, Material.ELYTRA);

        putDefault(50000.0, Material.DRAGON_EGG);

        // 1.20/1.21+ newer materials
        putDefault(100.0, Material.BREEZE_ROD);
        putDefault(40.0, Material.WIND_CHARGE);
        putDefault(300.0, Material.HEAVY_CORE);
        putDefault(10.0, Material.PACKED_ICE);
        putDefault(20.0, Material.BLUE_ICE);
        putDefault(4.0, Material.ICE, Material.FROSTED_ICE);
        putDefault(8.0, Material.HANGING_ROOTS);
        putDefault(5.0, Material.MOSS_CARPET);
        putDefault(8.0, Material.BIG_DRIPLEAF, Material.SMALL_DRIPLEAF);
        putDefault(8.0, Material.MANGROVE_ROOTS, Material.MUDDY_MANGROVE_ROOTS);
        putDefault(2.0, Material.IRON_NUGGET);
        putDefault(4.0, Material.GOLD_NUGGET);
        putDefault(3000.0, Material.NETHERITE_INGOT);
        putDefault(500.0, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        // Built-in blacklist
        BUILTIN_BLACKLIST.addAll(Arrays.asList(
                Material.BEDROCK, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_BLOCK,
                Material.STRUCTURE_VOID, Material.JIGSAW, Material.COMMAND_BLOCK,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.COMMAND_BLOCK_MINECART, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK,
                Material.SPAWNER, Material.BUDDING_AMETHYST, Material.END_PORTAL_FRAME,
                Material.REINFORCED_DEEPSLATE, Material.PETRIFIED_OAK_SLAB, Material.FARMLAND,
                Material.DIRT_PATH, Material.INFESTED_STONE, Material.INFESTED_COBBLESTONE,
                Material.INFESTED_DEEPSLATE, Material.INFESTED_STONE_BRICKS,
                Material.INFESTED_MOSSY_STONE_BRICKS, Material.INFESTED_CRACKED_STONE_BRICKS,
                Material.INFESTED_CHISELED_STONE_BRICKS, Material.TRIAL_SPAWNER, Material.VAULT,
                Material.SUSPICIOUS_SAND, Material.SUSPICIOUS_GRAVEL, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.POTION, Material.SPLASH_POTION,
                Material.LINGERING_POTION, Material.TIPPED_ARROW, Material.ENCHANTED_BOOK,
                Material.WRITTEN_BOOK, Material.SUSPICIOUS_STEW, Material.FILLED_MAP
        ));
    }

    private static void putDefault(double value, Material... materials) {
        for (Material m : materials) {
            DEFAULT_BASE_VALUES.put(m, value);
        }
    }

    public ItemValues(UpgraderPlugin plugin) {
        this.config = plugin.getUpgraderConfig();
        activeInstance = this;
    }

    public static void invalidateCache() {
        // Called when config changes
        ItemValues active = activeInstance;
        if (active != null) {
            active.computedValues = null;
            active.cachedCatalog = null;
            active.categoryCatalogs.clear();
        }
    }

    public boolean isBlacklisted(Material material) {
        return isBuiltInBlacklisted(material) || config.getBlacklist().contains(material.name().toLowerCase());
    }

    public boolean isBuiltInBlacklisted(Material material) {
        return material == Material.AIR || BUILTIN_BLACKLIST.contains(material);
    }

    public double unitValue(Material material) {
        if (material == Material.AIR) return 0.0;

        Map<Material, Double> table = getValueTable();
        Double value = table.get(material);
        if (value != null) {
            return value * config.globalValueMultiplier * itemMultiplier(material);
        }
        return fallbackValue() * config.globalValueMultiplier * itemMultiplier(material);
    }

    public double stackValue(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return 0.0;
        }
        double value = unitValue(stack.getType()) * stack.getAmount();

        // Durability factor (modern Damageable API)
        if (stack.getType().getMaxDurability() > 0) {
            var meta = stack.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int damage = damageable.getDamage();
                if (damage > 0) {
                    double remaining = 1.0 - (double) damage / stack.getType().getMaxDurability();
                    value *= Math.max(0.05, remaining);
                }
            }
        }

        return value;
    }

    /**
     * All upgradable items sorted by value. The returned list is cached and must
     * not be modified by callers.
     */
    public List<Material> catalog() {
        List<Material> local = cachedCatalog;
        if (local != null) return local;

        synchronized (this) {
            if (cachedCatalog != null) return cachedCatalog;
            List<Material> items = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isItem() && !isBlacklisted(material)) {
                    // Defensive check: some materials may claim to be items but
                    // still fail to wrap in an ItemStack (e.g. due to server quirks)
                    try {
                        ItemStack testStack = new ItemStack(material);
                        items.add(material);
                    } catch (IllegalArgumentException ignored) {
                        // Skip materials that can't be made into ItemStacks
                    }
                }
            }
            // Sort alphabetically by material name (groups similar items together)
            items.sort(Comparator.comparing(Material::name));
            return cachedCatalog = items;
        }
    }

    /**
     * Get catalog items for a specific category. The returned list is cached per
     * category and must not be modified by callers.
     */
    public List<Material> catalog(ItemCategory category) {
        // Check if this is the "all" category
        if (category != null && category.id().equals("all")) {
            return catalog(); // Delegate to the main catalog for ALL
        }

        List<Material> local = categoryCatalogs.get(category);
        if (local != null) return local;

        // Get base catalog outside synchronized block to avoid potential lock ordering issues
        List<Material> baseCatalog = catalog();
        if (baseCatalog == null) return new ArrayList<>();
        List<Material> items = new ArrayList<>();
        for (Material material : baseCatalog) {
            if (category.contains(material)) {
                items.add(material);
            }
        }
        // Sort alphabetically by material name (groups similar items together)
        items.sort(Comparator.comparing(Material::name));

        synchronized (this) {
            // Double-check in case another thread initialized this category while we were sorting
            local = categoryCatalogs.get(category);
            if (local != null) return local;

            categoryCatalogs.put(category, items);
            return items;
        }
    }

    private Map<Material, Double> getValueTable() {
        Map<Material, Double> local = computedValues;
        if (local != null) return local;

        synchronized (this) {
            local = computedValues;
            if (local != null) return local;
            computedValues = local = compute();
            return local;
        }
    }

    private Map<Material, Double> compute() {
        long started = System.nanoTime();
        Map<Material, Double> table = new HashMap<>();
        Set<Material> overridden = new HashSet<>();
        int anchors = 0;

        // Apply config overrides and base values
        Map<Material, Double> baseValues = getBaseValuesMap();
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;

            Double override = configOverride(material);
            if (override != null) {
                table.put(material, override);
                overridden.add(material);
                continue;
            }

            Double base = baseValues.get(material);
            if (base == null) continue;

            table.put(material, base);
            anchors++;
        }

        // Normalize recipes (only if use-recipe-pricing is enabled)
        List<PricedRecipe> recipes = config.useRecipePricing ? normalize() : new ArrayList<>();
        long recipesDone = System.nanoTime();

        // Relax prices
        Set<Material> fixed = new HashSet<>(overridden);
        relax(recipes, table, fixed);

        int raisedByConversion = 0;
        for (int round = 0; round < 24; round++) {
            int n = conversionFloors(recipes, table, fixed);
            raisedByConversion += n;
            if (n == 0) break;
            relax(recipes, table, fixed);
        }

        long end = System.nanoTime();
        int finalOverridden = overridden.size();
        int finalAnchors = anchors;
        int finalRecipes = recipes.size();
        int finalRaised = raisedByConversion;
        long totalMs = (end - started) / 1_000_000;
        long recipesMs = (recipesDone - started) / 1_000_000;
        LOGGER.log(Level.INFO, () -> String.format(
                "Priced %d items in %d ms (%d ms reading %d recipes): %d config overrides, %d anchors, %d conversion floors",
                table.size(), totalMs, recipesMs, finalRecipes,
                finalOverridden, finalAnchors, finalRaised));

        return table;
    }

    private void relax(List<PricedRecipe> recipes, Map<Material, Double> table, Set<Material> fixed) {
        for (int pass = 0; pass < 16; pass++) {
            boolean changed = false;
            for (PricedRecipe recipe : recipes) {
                if (fixed.contains(recipe.result)) continue;
                double cost = recipe.cost(table);
                if (Double.isInfinite(cost)) continue;
                cost = Math.max(1.0, cost);
                Double current = table.get(recipe.result);
                if (current != null && !(cost < current - 1e-6)) continue;
                table.put(recipe.result, cost);
                changed = true;
            }
            if (!changed) break;
        }
    }

    private int conversionFloors(List<PricedRecipe> recipes, Map<Material, Double> table, Set<Material> fixed) {
        Map<Material, Double> floors = new HashMap<>();
        for (PricedRecipe recipe : recipes) {
            Double result = table.get(recipe.result);
            if (result == null) continue;

            List<List<Material>> groups = recipe.ingredientGroups;
            double[] groupCost = new double[groups.size()];
            for (int i = 0; i < groups.size(); i++) {
                double cheapest = Double.POSITIVE_INFINITY;
                for (Material item : groups.get(i)) {
                    cheapest = Math.min(cheapest, rawValue(table, item));
                }
                groupCost[i] = cheapest;
            }

            double worth = result * recipe.count;
            Set<Material> seen = new HashSet<>();
            for (List<Material> group : groups) {
                for (Material item : group) {
                    if (table.containsKey(item) || !seen.add(item)) continue;
                    int slots = 0;
                    double rest = 0.0;
                    for (int i = 0; i < groups.size(); i++) {
                        if (groups.get(i).contains(item)) {
                            slots++;
                            continue;
                        }
                        rest += groupCost[i];
                    }
                    double share = (worth - rest) / (double) slots;
                    if (!(share > rarityFallback(item) + 1e-6)) continue;
                    floors.merge(item, share, Math::max);
                }
            }
        }

        for (Map.Entry<Material, Double> entry : floors.entrySet()) {
            table.put(entry.getKey(), entry.getValue());
            fixed.add(entry.getKey());
        }
        return floors.size();
    }

    private Double rawValue(Map<Material, Double> table, Material material) {
        Double value = table.get(material);
        return value != null ? value : rarityFallback(material);
    }

    private Double configOverride(Material material) {
        if (config.getValueOverrides().isEmpty()) return null;
        return config.getValueOverrides().get(material.name().toLowerCase());
    }

    private double itemMultiplier(Material material) {
        String id = material.name().toLowerCase();
        if (id.isEmpty()) return 1.0;
        return config.getValueMultipliers().getOrDefault(id, 1.0);
    }

    private double fallbackValue() {
        return config.fallbackValue;
    }

    private double rarityFallback(Material _material) {
        // Bukkit doesn't have rarity, use a default
        return config.fallbackValue;
    }

    /**
     * Get the base values map to use for computation.
     * Uses config base-values if available, otherwise falls back to hardcoded defaults.
     */
    private Map<Material, Double> getBaseValuesMap() {
        if (config.hasCustomBaseValues()) {
            Map<String, Double> configBase = config.getBaseValues();
            Map<Material, Double> result = new HashMap<>();
            for (Map.Entry<String, Double> entry : configBase.entrySet()) {
                Material material = Material.matchMaterial(entry.getKey().toUpperCase());
                if (material != null) {
                    result.put(material, entry.getValue());
                }
            }
            return result;
        }
        return DEFAULT_BASE_VALUES;
    }

    // Extract materials that match a RecipeChoice
    private List<Material> extractMaterials(RecipeChoice choice, List<Material> allItems) {
        List<Material> materials = new ArrayList<>();
        if (choice == null) return materials;

        // Fast paths: nearly all recipes use MaterialChoice or ExactChoice, so we
        // can skip the O(allItems) scan (and its ItemStack allocations) entirely.
        if (choice instanceof RecipeChoice.ExactChoice exact) {
            Material material = exact.getItemStack().getType();
            if (material.isItem()) {
                materials.add(material);
            }
            return materials;
        }
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            try {
                for (Material material : materialChoice.getChoices()) {
                    if (material.isItem() && !materials.contains(material)) {
                        materials.add(material);
                    }
                }
                if (!materials.isEmpty()) {
                    return materials;
                }
            } catch (RuntimeException ignored) {
                // Predicate-backed choice without a material list: fall through to the scan
            }
        }

        for (Material m : allItems) {
            if (!m.isItem()) continue;
            try {
                if (choice.test(new ItemStack(m))) {
                    materials.add(m);
                }
            } catch (IllegalArgumentException ignored) {
                // Defensive: a material that can't be wrapped in an ItemStack
            }
        }
        return materials;
    }

    private List<PricedRecipe> normalize() {
        List<PricedRecipe> normalized = new ArrayList<>();
        // Only materials that are actual items — creating an ItemStack from a
        // block-only material (wall signs, wall torches, …) throws on modern Paper.
        List<Material> allItems = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem()) {
                allItems.add(material);
            }
        }

        // Convert iterator to list
        List<Recipe> recipes = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            recipes.add(iterator.next());
        }

        for (Recipe recipe : recipes) {
            if (recipe instanceof ShapedRecipe) {
                processShapedRecipe((ShapedRecipe) recipe, normalized, allItems);
            } else if (recipe instanceof ShapelessRecipe) {
                processShapelessRecipe((ShapelessRecipe) recipe, normalized, allItems);
            } else if (recipe instanceof SmithingRecipe) {
                processSmithingRecipe((SmithingRecipe) recipe, normalized, allItems);
            } else if (recipe instanceof FurnaceRecipe || recipe instanceof BlastingRecipe ||
                    recipe instanceof SmokingRecipe || recipe instanceof CampfireRecipe) {
                processSmeltingRecipe(recipe, normalized, allItems);
            } else if (recipe instanceof StonecuttingRecipe) {
                processStonecuttingRecipe((StonecuttingRecipe) recipe, normalized, allItems);
            }
        }
        return normalized;
    }

    private void processShapedRecipe(ShapedRecipe recipe, List<PricedRecipe> normalized, List<Material> allItems) {
        ItemStack result = recipe.getResult();
        if (result == null || result.getAmount() <= 0) return;

        List<List<Material>> groups = new ArrayList<>();
        String[] shape = recipe.getShape();
        Map<Character, RecipeChoice> choices = recipe.getChoiceMap();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length(); col++) {
                char key = shape[row].charAt(col);
                if (key == ' ') continue;
                RecipeChoice choice = choices.get(key);
                if (choice == null) continue;

                List<Material> group = extractMaterials(choice, allItems);
                if (!group.isEmpty()) {
                    groups.add(group);
                }
            }
        }

        if (groups.isEmpty()) return;

        int count = result.getAmount();
        List<List<Material>> kept = new ArrayList<>();
        for (List<Material> group : groups) {
            if (group.size() == 1 && group.get(0) == result.getType()) {
                count--;
                continue;
            }
            kept.add(group);
        }

        if (count > 0 && !kept.isEmpty()) {
            normalized.add(new PricedRecipe(result.getType(), count, kept));
        }
    }

    private void processShapelessRecipe(ShapelessRecipe recipe, List<PricedRecipe> normalized, List<Material> allItems) {
        ItemStack result = recipe.getResult();
        if (result == null || result.getAmount() <= 0) return;

        List<List<Material>> groups = new ArrayList<>();
        for (RecipeChoice choice : recipe.getChoiceList()) {
            List<Material> group = extractMaterials(choice, allItems);
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }

        if (groups.isEmpty()) return;

        int count = result.getAmount();
        List<List<Material>> kept = new ArrayList<>();
        for (List<Material> group : groups) {
            if (group.size() == 1 && group.get(0) == result.getType()) {
                count--;
                continue;
            }
            kept.add(group);
        }

        if (count > 0 && !kept.isEmpty()) {
            normalized.add(new PricedRecipe(result.getType(), count, kept));
        }
    }

    private void processSmithingRecipe(SmithingRecipe recipe, List<PricedRecipe> normalized, List<Material> allItems) {
        ItemStack result = recipe.getResult();
        if (result == null || result.getAmount() <= 0) return;

        List<Material> baseGroup = extractMaterials(recipe.getBase(), allItems);
        List<Material> additionGroup = extractMaterials(recipe.getAddition(), allItems);

        if (baseGroup.isEmpty() || additionGroup.isEmpty()) return;

        List<List<Material>> groups = new ArrayList<>();
        if (recipe instanceof SmithingTransformRecipe transform) {
            List<Material> templateGroup = extractMaterials(transform.getTemplate(), allItems);
            if (!templateGroup.isEmpty()) {
                groups.add(templateGroup);
            }
        }
        groups.add(baseGroup);
        groups.add(additionGroup);
        normalized.add(new PricedRecipe(result.getType(), result.getAmount(), groups));
    }

    private void processSmeltingRecipe(Recipe recipe, List<PricedRecipe> normalized, List<Material> allItems) {
        ItemStack result = null;
        RecipeChoice inputChoice = null;

        if (recipe instanceof FurnaceRecipe) {
            FurnaceRecipe fr = (FurnaceRecipe) recipe;
            result = fr.getResult();
            inputChoice = fr.getInputChoice();
        } else if (recipe instanceof BlastingRecipe) {
            BlastingRecipe br = (BlastingRecipe) recipe;
            result = br.getResult();
            inputChoice = br.getInputChoice();
        } else if (recipe instanceof SmokingRecipe) {
            SmokingRecipe sr = (SmokingRecipe) recipe;
            result = sr.getResult();
            inputChoice = sr.getInputChoice();
        } else if (recipe instanceof CampfireRecipe) {
            CampfireRecipe cr = (CampfireRecipe) recipe;
            result = cr.getResult();
            inputChoice = cr.getInputChoice();
        }

        if (result == null || result.getAmount() <= 0 || inputChoice == null) return;

        List<Material> inputGroup = extractMaterials(inputChoice, allItems);
        if (!inputGroup.isEmpty()) {
            normalized.add(new PricedRecipe(result.getType(), result.getAmount(), List.of(inputGroup)));
        }
    }

    private void processStonecuttingRecipe(StonecuttingRecipe recipe, List<PricedRecipe> normalized, List<Material> allItems) {
        ItemStack result = recipe.getResult();
        if (result == null || result.getAmount() <= 0) return;

        RecipeChoice inputChoice = recipe.getInputChoice();
        List<Material> inputGroup = extractMaterials(inputChoice, allItems);
        if (!inputGroup.isEmpty()) {
            normalized.add(new PricedRecipe(result.getType(), result.getAmount(), List.of(inputGroup)));
        }
    }

    public static class PricedRecipe {
        public final Material result;
        public final int count;
        public final List<List<Material>> ingredientGroups;

        public PricedRecipe(Material result, int count, List<List<Material>> ingredientGroups) {
            this.result = result;
            this.count = count;
            this.ingredientGroups = ingredientGroups;
        }

        public double cost(Map<Material, Double> table) {
            double sum = 0.0;
            for (List<Material> group : ingredientGroups) {
                double cheapest = Double.POSITIVE_INFINITY;
                for (Material item : group) {
                    Double value = table.get(item);
                    if (value == null || !(value < cheapest)) continue;
                    cheapest = value;
                }
                if (Double.isInfinite(cheapest)) {
                    return Double.POSITIVE_INFINITY;
                }
                sum += cheapest;
            }
            return sum <= 0.0 ? Double.POSITIVE_INFINITY : sum / (double) count;
        }
    }
}
package com.unlockthecode.upgrader.config;

import com.unlockthecode.upgrader.value.ItemCategory;
import com.unlockthecode.upgrader.value.ItemValues;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

public class UpgraderConfig {

    private final JavaPlugin plugin;
    private static final Logger logger = Logger.getLogger(UpgraderConfig.class.getName());

    // --- Gameplay scalars (public fields, existing style) ---
    public double houseEdge = 0.9;
    public double minChance = 0.001;
    public double maxChance = 0.9;
    public int spinTicks = 120;
    public int maxTargetCount = 64;
    public int attemptLimit = 0;
    public int attemptWindowSeconds = 0;
    public double globalValueMultiplier = 1.0;

    // --- Gameplay maps/sets ---
    public Map<String, Double> valueOverrides = new HashMap<>();
    public Map<String, Double> valueMultipliers = new HashMap<>();
    public Set<String> blacklist = new HashSet<>();

    // --- NEW: Item values ---
    public boolean useRecipePricing = true;
    public double fallbackValue = 3.0;
    private final Map<String, Double> baseValues = new HashMap<>();
    private boolean hasCustomBaseValues = false;

    // --- NEW: Categories ---
    private List<ItemCategory> categories = new ArrayList<>();

    // --- NEW: GUI layout ---
    private int slotInfo = 4;
    private int slotWager = 10;
    private int slotArrow = 13;
    private int slotTarget = 16;
    private int slotStats = 22; // Stats button
    private int slotMinus32 = 28;
    private int slotMinus16 = 29;
    private int slotMinus1 = 30;
    private int slotCount = 31;
    private int slotPlus1 = 32;
    private int slotPlus16 = 33;
    private int slotPlus32 = 34;
    private int slotCatalog = 39;
    private int slotUpgrade = 40;
    private int slotClose = 41;
    private int[] categorySlots = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private int catalogPrev = 45;
    private int catalogIndicator = 48;
    private int catalogBack = 50;
    private int catalogNext = 53;
    private int[] spinSlots = {11, 12, 13, 14, 15};

    // --- NEW: Stats GUI layout ---
    private int statsPlayerStats = 19;
    private int statsLeaderboards = 21;
    private int statsAchievements = 23;
    private int statsServerStats = 25;
    private int statsBack = 45;
    private int statsClose = 53;
    private int[] statsContentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private int statsLeaderboardPrev = 45;
    private int statsLeaderboardBack = 48;
    private int statsLeaderboardIndicator = 49;
    private int statsLeaderboardNext = 53;
    private int statsAchievementBack = 45;
    private int statsAchievementClose = 53;
    private int statsServerStatsBack = 45;
    private int statsServerStatsClose = 53;

    // --- NEW: Presets ---
    private List<Preset> presets = new ArrayList<>();

    public UpgraderConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        // Gameplay scalars
        houseEdge = config.getDouble("house-edge", 0.9);
        minChance = config.getDouble("min-chance", 0.001);
        maxChance = config.getDouble("max-chance", 0.9);
        spinTicks = config.getInt("spin-ticks", 120);
        maxTargetCount = config.getInt("max-target-count", 64);
        attemptLimit = config.getInt("attempt-limit", 0);
        attemptWindowSeconds = config.getInt("attempt-window-seconds", 0);
        globalValueMultiplier = config.getDouble("global-value-multiplier", 1.0);

        // Item values
        useRecipePricing = config.getBoolean("use-recipe-pricing", true);
        fallbackValue = config.getDouble("fallback-value", 3.0);

        baseValues.clear();
        ConfigurationSection baseSection = config.getConfigurationSection("base-values");
        if (baseSection != null) {
            hasCustomBaseValues = true;
            for (String key : baseSection.getKeys(false)) {
                Double val = baseSection.getDouble(key);
                if (val > 0) {
                    baseValues.put(key.toLowerCase(), val);
                } else {
                    logger.warning(() -> String.format("Invalid base-value for '%s': %s (must be > 0)", key, val));
                }
            }
        } else {
            hasCustomBaseValues = false;
        }

        // Value overrides
        valueOverrides.clear();
        ConfigurationSection overridesSection = config.getConfigurationSection("value-overrides");
        if (overridesSection != null) {
            for (String key : overridesSection.getKeys(false)) {
                valueOverrides.put(key.toLowerCase(), overridesSection.getDouble(key));
            }
        }

        // Value multipliers
        valueMultipliers.clear();
        ConfigurationSection multipliersSection = config.getConfigurationSection("value-multipliers");
        if (multipliersSection != null) {
            for (String key : multipliersSection.getKeys(false)) {
                valueMultipliers.put(key.toLowerCase(), multipliersSection.getDouble(key));
            }
        }

        // Blacklist
        blacklist.clear();
        if (config.isList("blacklist")) {
            for (String item : config.getStringList("blacklist")) {
                if (!item.isBlank()) {
                    blacklist.add(item.toLowerCase());
                }
            }
        }

        // Categories
        categories.clear();
        if (config.isList("categories")) {
            List<Map<?, ?>> rawCats = config.getMapList("categories");
            for (Map<?, ?> raw : rawCats) {
                ItemCategory cat = ItemCategory.fromConfig(raw);
                if (cat != null) {
                    categories.add(cat);
                }
            }
        }
        if (categories.isEmpty()) {
            categories = ItemCategory.defaults();
        }

        // GUI layout
        ConfigurationSection gui = config.getConfigurationSection("gui");
        if (gui != null) {
            ConfigurationSection slots = gui.getConfigurationSection("slots");
            if (slots != null) {
                slotInfo = clampSlot(slots.getInt("info", 4));
                slotWager = clampSlot(slots.getInt("wager", 10));
                slotArrow = clampSlot(slots.getInt("arrow", 13));
                slotTarget = clampSlot(slots.getInt("target", 16));
                slotStats = clampSlot(slots.getInt("stats", 22));
                slotMinus32 = clampSlot(slots.getInt("count-minus32", 28));
                slotMinus16 = clampSlot(slots.getInt("count-minus16", 29));
                slotMinus1 = clampSlot(slots.getInt("count-minus1", 30));
                slotCount = clampSlot(slots.getInt("count", 31));
                slotPlus1 = clampSlot(slots.getInt("count-plus1", 32));
                slotPlus16 = clampSlot(slots.getInt("count-plus16", 33));
                slotPlus32 = clampSlot(slots.getInt("count-plus32", 34));
                slotCatalog = clampSlot(slots.getInt("catalog", 39));
                slotUpgrade = clampSlot(slots.getInt("upgrade", 40));
                slotClose = clampSlot(slots.getInt("close", 41));
            }

            List<Integer> catSlots = gui.getIntegerList("category-slots");
            if (!catSlots.isEmpty()) {
                int[] arr = new int[catSlots.size()];
                for (int i = 0; i < catSlots.size(); i++) {
                    arr[i] = clampSlot(catSlots.get(i));
                }
                categorySlots = arr;
            }

            ConfigurationSection catGui = gui.getConfigurationSection("catalog");
            if (catGui != null) {
                catalogPrev = clampSlot(catGui.getInt("prev", 45));
                catalogIndicator = clampSlot(catGui.getInt("page-indicator", 49));
                catalogBack = clampSlot(catGui.getInt("back", 51));
                catalogNext = clampSlot(catGui.getInt("next", 53));
            }

            List<Integer> spinList = gui.getIntegerList("spin-slots");
            if (!spinList.isEmpty()) {
                int[] arr = new int[spinList.size()];
                for (int i = 0; i < spinList.size(); i++) {
                    arr[i] = clampSlot(spinList.get(i));
                }
                spinSlots = arr;
            }
        }

        // Stats GUI layout - top-level section (falls back to nested under gui for older configs)
        ConfigurationSection statsGui = config.getConfigurationSection("stats-gui");
        if (statsGui == null && gui != null) {
            statsGui = gui.getConfigurationSection("stats-gui");
        }
        if (statsGui != null) {
            statsPlayerStats = clampSlot(statsGui.getInt("player-stats", 10));
            statsLeaderboards = clampSlot(statsGui.getInt("leaderboards", 12));
            statsAchievements = clampSlot(statsGui.getInt("achievements", 14));
            statsServerStats = clampSlot(statsGui.getInt("server-stats", 16));
            statsBack = clampSlot(statsGui.getInt("back", 45));
            statsClose = clampSlot(statsGui.getInt("close", 53));

            List<Integer> contentSlots = statsGui.getIntegerList("content-slots");
            if (!contentSlots.isEmpty()) {
                int[] arr = new int[contentSlots.size()];
                for (int i = 0; i < contentSlots.size(); i++) {
                    arr[i] = clampSlot(contentSlots.get(i));
                }
                statsContentSlots = arr;
            }

            statsLeaderboardPrev = clampSlot(statsGui.getInt("leaderboard-prev", 45));
            statsLeaderboardBack = clampSlot(statsGui.getInt("leaderboard-back", 48));
            statsLeaderboardIndicator = clampSlot(statsGui.getInt("leaderboard-indicator", 49));
            statsLeaderboardNext = clampSlot(statsGui.getInt("leaderboard-next", 53));

            statsAchievementBack = clampSlot(statsGui.getInt("achievement-back", 45));
            statsAchievementClose = clampSlot(statsGui.getInt("achievement-close", 53));

            statsServerStatsBack = clampSlot(statsGui.getInt("server-stats-back", 45));
            statsServerStatsClose = clampSlot(statsGui.getInt("server-stats-close", 53));
        }

        // Presets
        presets.clear();
        if (config.isList("presets")) {
            List<Map<?, ?>> rawPresets = config.getMapList("presets");
            for (Map<?, ?> raw : rawPresets) {
                Preset p = Preset.fromConfig(raw);
                if (p != null) {
                    presets.add(p);
                }
            }
        }
        if (presets.isEmpty()) {
            presets = Preset.defaults();
        }

        // Validate preset slots don't collide with functional slots
        Set<Integer> functionalSlots = new HashSet<>();
        functionalSlots.add(slotInfo);
        functionalSlots.add(slotWager);
        functionalSlots.add(slotArrow);
        functionalSlots.add(slotTarget);
        functionalSlots.add(slotStats);
        functionalSlots.add(slotMinus32);
        functionalSlots.add(slotMinus16);
        functionalSlots.add(slotMinus1);
        functionalSlots.add(slotCount);
        functionalSlots.add(slotPlus1);
        functionalSlots.add(slotPlus16);
        functionalSlots.add(slotPlus32);
        functionalSlots.add(slotCatalog);
        functionalSlots.add(slotUpgrade);
        functionalSlots.add(slotClose);
        functionalSlots.add(catalogPrev);
        functionalSlots.add(catalogIndicator);
        functionalSlots.add(catalogBack);
        functionalSlots.add(catalogNext);
        for (int s : spinSlots) functionalSlots.add(s);
        for (int s : categorySlots) functionalSlots.add(s);

        for (Preset p : presets) {
            if (functionalSlots.contains(p.slot)) {
                logger.warning(() -> String.format("Preset '%s' uses slot %d which collides with a functional slot; preset will be ignored", p.label, p.slot));
            }
        }

        sanitize();

        logger.info("TheUpgrader configuration loaded");
    }

    private int clampSlot(int slot) {
        return Math.max(0, Math.min(53, slot));
    }

    private void sanitize() {
        houseEdge = clamp(houseEdge, 0.01, 1.0);
        minChance = clamp(minChance, 0.0, 1.0);
        maxChance = clamp(maxChance, 0.01, 1.0);
        spinTicks = (int) clamp(spinTicks, 20, 600);
        maxTargetCount = Math.max(1, (int) clamp(maxTargetCount, 1, 64));
        attemptLimit = Math.max(0, (int) clamp(attemptLimit, 0, 10000));
        attemptWindowSeconds = Math.max(0, (int) clamp(attemptWindowSeconds, 0, 86400));
        globalValueMultiplier = clamp(globalValueMultiplier, 0.01, 1000.0);
        fallbackValue = Math.max(0.01, fallbackValue);

        valueOverrides.entrySet().removeIf(e -> e.getKey().isBlank() || !Double.isFinite(e.getValue()) || e.getValue() <= 0.0);
        valueMultipliers.entrySet().removeIf(e -> e.getKey().isBlank() || !Double.isFinite(e.getValue()) || e.getValue() <= 0.0);
        blacklist.removeIf(String::isBlank);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    // --- Getters for new sections ---

    public boolean hasCustomBaseValues() {
        return hasCustomBaseValues;
    }

    public Map<String, Double> getBaseValues() {
        return new HashMap<>(baseValues);
    }

    public List<ItemCategory> getCategories() {
        return new ArrayList<>(categories);
    }

    // GUI slot getters
    public int slotInfo() { return slotInfo; }
    public int slotWager() { return slotWager; }
    public int slotArrow() { return slotArrow; }
    public int slotTarget() { return slotTarget; }
    public int slotStats() { return slotStats; }
    public int slotMinus32() { return slotMinus32; }
    public int slotMinus16() { return slotMinus16; }
    public int slotMinus1() { return slotMinus1; }
    public int slotCount() { return slotCount; }
    public int slotPlus1() { return slotPlus1; }
    public int slotPlus16() { return slotPlus16; }
    public int slotPlus32() { return slotPlus32; }
    public int slotCatalog() { return slotCatalog; }
    public int slotUpgrade() { return slotUpgrade; }
    public int slotClose() { return slotClose; }
    public int[] categorySlots() { return categorySlots.clone(); }
    public int catalogPrev() { return catalogPrev; }
    public int catalogIndicator() { return catalogIndicator; }
    public int catalogBack() { return catalogBack; }
    public int catalogNext() { return catalogNext; }
    public int[] spinSlots() { return spinSlots.clone(); }

    // Stats GUI slot getters
    public int statsPlayerStats() { return statsPlayerStats; }
    public int statsLeaderboards() { return statsLeaderboards; }
    public int statsAchievements() { return statsAchievements; }
    public int statsServerStats() { return statsServerStats; }
    public int statsBack() { return statsBack; }
    public int statsClose() { return statsClose; }
    public int[] statsContentSlots() { return statsContentSlots.clone(); }
    public int statsLeaderboardPrev() { return statsLeaderboardPrev; }
    public int statsLeaderboardBack() { return statsLeaderboardBack; }
    public int statsLeaderboardIndicator() { return statsLeaderboardIndicator; }
    public int statsLeaderboardNext() { return statsLeaderboardNext; }
    public int statsAchievementBack() { return statsAchievementBack; }
    public int statsAchievementClose() { return statsAchievementClose; }
    public int statsServerStatsBack() { return statsServerStatsBack; }
    public int statsServerStatsClose() { return statsServerStatsClose; }

    public List<Preset> getPresets() {
        return new ArrayList<>(presets);
    }

    // Preset class
    public static final class Preset {
        public enum Mode { MULTIPLIER, CHANCE }

        private final String label;
        private final Material icon;
        private final Component color; // Changed from ChatColor to Component
        private final int slot;
        private final Mode mode;
        private final double value;

        private Preset(String label, Material icon, Component color, int slot, Mode mode, double value) {
            this.label = label;
            this.icon = icon;
            this.color = color;
            this.slot = slot;
            this.mode = mode;
            this.value = value;
        }

        public String label() { return label; }
        public Material icon() { return icon; }
        public Component color() { return color; }
        public int slot() { return slot; }
        public Mode mode() { return mode; }
        public double value() { return value; }

        public static Preset fromConfig(Map<?, ?> raw) {
            String label = raw.get("label") != null ? String.valueOf(raw.get("label")) : "";
            String iconStr = raw.get("icon") != null ? String.valueOf(raw.get("icon")) : "";
            String colorStr = raw.get("color") != null ? String.valueOf(raw.get("color")) : "";
            Integer slot = raw.get("slot") instanceof Number ? ((Number) raw.get("slot")).intValue() : -1;
            Double mult = raw.get("multiplier") instanceof Number ? ((Number) raw.get("multiplier")).doubleValue() : null;
            Double chance = raw.get("chance") instanceof Number ? ((Number) raw.get("chance")).doubleValue() : null;

            if (label.isBlank() || iconStr.isBlank() || colorStr.isBlank() || slot < 0 || slot > 53) {
                return null;
            }
            Material icon = Material.matchMaterial(iconStr);
            if (icon == null) {
                return null;
            }
            Component color;
            try {
                NamedTextColor namedColor = NamedTextColor.NAMES.value(colorStr.toUpperCase());
                if (namedColor == null) {
                    return null;
                }
                color = Component.text("", namedColor);
            } catch (IllegalArgumentException e) {
                return null;
            }
            if (mult != null && mult > 0) {
                return new Preset(label, icon, color, slot, Mode.MULTIPLIER, mult);
            }
            if (chance != null && chance > 0 && chance <= 1.0) {
                return new Preset(label, icon, color, slot, Mode.CHANCE, chance);
            }
            return null;
        }

        public static List<Preset> defaults() {
            List<Preset> list = new ArrayList<>();
            list.add(new Preset("×2", Material.GOLD_NUGGET, Component.text("", NamedTextColor.YELLOW), 19, Mode.MULTIPLIER, 2.0));
            list.add(new Preset("×4", Material.GOLD_INGOT, Component.text("", NamedTextColor.YELLOW), 20, Mode.MULTIPLIER, 4.0));
            list.add(new Preset("×8", Material.GOLD_BLOCK, Component.text("", NamedTextColor.YELLOW), 21, Mode.MULTIPLIER, 8.0));
            list.add(new Preset("30%", Material.RED_DYE, Component.text("", NamedTextColor.RED), 23, Mode.CHANCE, 0.30));
            list.add(new Preset("50%", Material.ORANGE_DYE, Component.text("", NamedTextColor.GOLD), 24, Mode.CHANCE, 0.50));
            list.add(new Preset("70%", Material.LIME_DYE, Component.text("", NamedTextColor.GREEN), 25, Mode.CHANCE, 0.70));
            return list;
        }
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();

        config.set("house-edge", houseEdge);
        config.set("min-chance", minChance);
        config.set("max-chance", maxChance);
        config.set("spin-ticks", spinTicks);
        config.set("max-target-count", maxTargetCount);
        config.set("attempt-limit", attemptLimit);
        config.set("attempt-window-seconds", attemptWindowSeconds);
        config.set("global-value-multiplier", globalValueMultiplier);
        config.set("use-recipe-pricing", useRecipePricing);
        config.set("fallback-value", fallbackValue);

        // base-values are NOT written back here - they are preserved from the loaded config

        TreeMap<String, Double> sortedOverrides = new TreeMap<>(valueOverrides);
        for (Map.Entry<String, Double> entry : sortedOverrides.entrySet()) {
            config.set("value-overrides." + entry.getKey(), entry.getValue());
        }

        TreeMap<String, Double> sortedMultipliers = new TreeMap<>(valueMultipliers);
        for (Map.Entry<String, Double> entry : sortedMultipliers.entrySet()) {
            config.set("value-multipliers." + entry.getKey(), entry.getValue());
        }

        List<String> sortedBlacklist = new ArrayList<>(blacklist);
        sortedBlacklist.sort(String::compareToIgnoreCase);
        config.set("blacklist", sortedBlacklist);

        plugin.saveConfig();
        logger.info("TheUpgrader configuration saved");
    }

    public void reloadPlugin() {
        plugin.reloadConfig();
        load();
        ItemValues.invalidateCache();
    }

    public Map<String, Double> getValueOverrides() {
        return new HashMap<>(valueOverrides);
    }

    public Map<String, Double> getValueMultipliers() {
        return new HashMap<>(valueMultipliers);
    }

    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }
}
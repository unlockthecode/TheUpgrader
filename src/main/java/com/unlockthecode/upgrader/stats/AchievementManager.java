package com.unlockthecode.upgrader.stats;

import org.bukkit.Material;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Achievement definitions and tracking.
 * Achievements are checked on each upgrade and grant cosmetic rewards.
 */
public class AchievementManager {

    public enum Rarity {
        COMMON(NamedTextColor.WHITE, "Common"),
        UNCOMMON(NamedTextColor.GREEN, "Uncommon"),
        RARE(NamedTextColor.AQUA, "Rare"),
        EPIC(NamedTextColor.LIGHT_PURPLE, "Epic"),
        LEGENDARY(NamedTextColor.GOLD, "Legendary");

        public final NamedTextColor color;
        public final String label;

        Rarity(NamedTextColor color, String label) {
            this.color = color;
            this.label = label;
        }
    }

    public record Achievement(
        String id,
        String name,
        String description,
        Rarity rarity,
        Material icon,
        Function<PlayerStats, Boolean> check
    ) {}

    private static final List<Achievement> ACHIEVEMENTS = new ArrayList<>();

    static {
        // First wins
        register("first_win", "First Blood", "Win your first upgrade", Rarity.COMMON, Material.IRON_SWORD,
            s -> s.getTotalWins() >= 1);

        register("win_10", "Getting Started", "Win 10 upgrades", Rarity.COMMON, Material.GOLDEN_SWORD,
            s -> s.getTotalWins() >= 10);

        register("win_100", "High Roller", "Win 100 upgrades", Rarity.UNCOMMON, Material.DIAMOND_SWORD,
            s -> s.getTotalWins() >= 100);

        register("win_1000", "Legend", "Win 1,000 upgrades", Rarity.LEGENDARY, Material.NETHERITE_SWORD,
            s -> s.getTotalWins() >= 1000);

        // Streaks
        register("streak_3", "Warming Up", "Achieve a 3-win streak", Rarity.COMMON, Material.FIRE_CHARGE,
            s -> s.getBestWinStreak() >= 3);

        register("streak_5", "Hot Streak", "Achieve a 5-win streak", Rarity.UNCOMMON, Material.MAGMA_CREAM,
            s -> s.getBestWinStreak() >= 5);

        register("streak_10", "On Fire!", "Achieve a 10-win streak", Rarity.RARE, Material.BLAZE_ROD,
            s -> s.getBestWinStreak() >= 10);

        register("streak_25", "Legendary Streak", "Achieve a 25-win streak", Rarity.LEGENDARY, Material.NETHER_STAR,
            s -> s.getBestWinStreak() >= 25);

        // Big wins
        register("bigwin_100", "Nice Catch", "Win 100+ value in a single upgrade", Rarity.COMMON, Material.GOLD_INGOT,
            s -> s.getBiggestWinValue() >= 100);

        register("bigwin_1000", "Jackpot!", "Win 1,000+ value in a single upgrade", Rarity.UNCOMMON, Material.DIAMOND,
            s -> s.getBiggestWinValue() >= 1000);

        register("bigwin_10000", "Mega Jackpot", "Win 10,000+ value in a single upgrade", Rarity.EPIC, Material.ENCHANTED_GOLDEN_APPLE,
            s -> s.getBiggestWinValue() >= 10000);

        register("bigwin_100000", "Dragon's Hoard", "Win 100,000+ value in a single upgrade", Rarity.LEGENDARY, Material.DRAGON_EGG,
            s -> s.getBiggestWinValue() >= 100000);

        // Unlikely wins
        register("unlikely_10", "Beating the Odds", "Win with less than 10% chance", Rarity.UNCOMMON, Material.RABBIT_FOOT,
            s -> s.getBiggestWinChance() > 0 && s.getBiggestWinChance() < 0.10);

        register("unlikely_1", "One in a Hundred", "Win with less than 1% chance", Rarity.RARE, Material.TOTEM_OF_UNDYING,
            s -> s.getBiggestWinChance() > 0 && s.getBiggestWinChance() < 0.01);

        register("unlikely_01", "Miracle", "Win with less than 0.1% chance", Rarity.LEGENDARY, Material.NETHER_STAR,
            s -> s.getBiggestWinChance() > 0 && s.getBiggestWinChance() < 0.001);

        // Volume
        register("wager_10000", "Big Spender", "Wager 10,000+ total value", Rarity.COMMON, Material.EMERALD,
            s -> s.getTotalWageredValue() >= 10000);

        register("wager_100000", "Whale", "Wager 100,000+ total value", Rarity.RARE, Material.DIAMOND_BLOCK,
            s -> s.getTotalWageredValue() >= 100000);

        register("wager_1000000", "Economy Breaker", "Wager 1,000,000+ total value", Rarity.LEGENDARY, Material.NETHERITE_BLOCK,
            s -> s.getTotalWageredValue() >= 1000000);

        // Net profit
        register("profit_1000", "In the Green", "Net profit of 1,000+ value", Rarity.COMMON, Material.LIME_DYE,
            s -> s.getNetValue() >= 1000);

        register("profit_10000", "Profit Master", "Net profit of 10,000+ value", Rarity.UNCOMMON, Material.EMERALD_BLOCK,
            s -> s.getNetValue() >= 10000);

        register("profit_100000", "Midas Touch", "Net profit of 100,000+ value", Rarity.EPIC, Material.GOLD_BLOCK,
            s -> s.getNetValue() >= 100000);

        // Specific items
        register("netherite_win", "Ancient Power", "Win a netherite item", Rarity.RARE, Material.NETHERITE_INGOT,
            s -> s.getBiggestWinMaterial() != null && s.getBiggestWinMaterial().name().contains("NETHERITE"));

        register("elytra_win", "Sky King", "Win an Elytra", Rarity.EPIC, Material.ELYTRA,
            s -> s.getBiggestWinMaterial() == Material.ELYTRA);

        register("totem_win", "Death Defied", "Win a Totem of Undying", Rarity.RARE, Material.TOTEM_OF_UNDYING,
            s -> s.getBiggestWinMaterial() == Material.TOTEM_OF_UNDYING);

        register("enchanted_apple_win", "Notch's Apple", "Win an Enchanted Golden Apple", Rarity.EPIC, Material.ENCHANTED_GOLDEN_APPLE,
            s -> s.getBiggestWinMaterial() == Material.ENCHANTED_GOLDEN_APPLE);

        // Dedication
        register("dedication_1", "Day One", "Play on 1 different day", Rarity.COMMON, Material.CLOCK,
            s -> daysPlayed(s) >= 1);

        register("dedication_7", "Weekly Regular", "Play on 7 different days", Rarity.UNCOMMON, Material.CLOCK,
            s -> daysPlayed(s) >= 7);

        register("dedication_30", "Monthly Devotee", "Play on 30 different days", Rarity.RARE, Material.COMPASS,
            s -> daysPlayed(s) >= 30);

        // Losing (positive spin)
        register("loss_100", "Resilient", "Lose 100+ value in a single upgrade", Rarity.COMMON, Material.REDSTONE,
            s -> s.getBiggestLossValue() >= 100);

        register("loss_1000", "It Builds Character", "Lose 1,000+ value in a single upgrade", Rarity.UNCOMMON, Material.NETHERITE_SCRAP,
            s -> s.getBiggestLossValue() >= 1000);
    }

    private static void register(String id, String name, String description, Rarity rarity, Material icon, Function<PlayerStats, Boolean> check) {
        ACHIEVEMENTS.add(new Achievement(id, name, description, rarity, icon, check));
    }

    private static int daysPlayed(PlayerStats stats) {
        // Simple heuristic: estimate days from timestamps
        long span = stats.getLastPlayed() - stats.getFirstPlayed();
        return Math.max(1, (int) (span / (24 * 60 * 60 * 1000)) + 1);
    }

    /** Get all achievement definitions. */
    public static List<Achievement> getAllAchievements() {
        return new ArrayList<>(ACHIEVEMENTS);
    }

    /** Check which achievements a player has earned but not yet unlocked. */
    public static List<Achievement> checkNewAchievements(PlayerStats stats, java.util.Set<String> alreadyUnlocked) {
        List<Achievement> newAchievements = new ArrayList<>();
        for (Achievement ach : ACHIEVEMENTS) {
            if (!alreadyUnlocked.contains(ach.id()) && ach.check().apply(stats)) {
                newAchievements.add(ach);
            }
        }
        return newAchievements;
    }

    /** Check if a player has a specific achievement. */
    public static boolean hasAchievement(PlayerStats stats, String achievementId) {
        for (Achievement ach : ACHIEVEMENTS) {
            if (ach.id().equals(achievementId) && ach.check().apply(stats)) {
                return true;
            }
        }
        return false;
    }

    /** Get the total count of achievements. */
    public static int getTotalCount() {
        return ACHIEVEMENTS.size();
    }
}
package com.unlockthecode.upgrader.stats;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player persistent statistics for the upgrader.
 * Tracks lifetime totals, best wins, streaks, and favorite items.
 */
public class PlayerStats {

    private final UUID playerId;
    private final String playerName;

    // Lifetime totals
    private long totalWageredValue = 0;
    private long totalWonValue = 0;
    private int totalUpgrades = 0;
    private int totalWins = 0;
    private int totalLosses = 0;

    // Best single upgrade
    private long biggestWinValue = 0;
    private Material biggestWinMaterial = Material.AIR;
    private int biggestWinCount = 0;
    private double biggestWinChance = 0;

    private long biggestLossValue = 0;
    private Material biggestLossMaterial = Material.AIR;

    // Streaks
    private int currentWinStreak = 0;
    private int bestWinStreak = 0;

    // Favorite items (material -> times used as wager)
    private final Map<String, Integer> favoriteWagers = new HashMap<>();
    private final Map<String, Integer> favoriteTargets = new HashMap<>();

    // Achievement progress
    private final Map<String, Object> achievementProgress = new HashMap<>();

    // Metadata
    private long firstPlayed = System.currentTimeMillis();
    private long lastPlayed = System.currentTimeMillis();

    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    // --- Getters ---

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public long getTotalWageredValue() { return totalWageredValue; }
    public long getTotalWonValue() { return totalWonValue; }
    public long getNetValue() { return totalWonValue - totalWageredValue; }
    public int getTotalUpgrades() { return totalUpgrades; }
    public int getTotalWins() { return totalWins; }
    public int getTotalLosses() { return totalLosses; }
    public double getWinRate() { return totalUpgrades > 0 ? (double) totalWins / totalUpgrades : 0.0; }
    public long getBiggestWinValue() { return biggestWinValue; }
    public Material getBiggestWinMaterial() { return biggestWinMaterial; }
    public int getBiggestWinCount() { return biggestWinCount; }
    public double getBiggestWinChance() { return biggestWinChance; }
    public long getBiggestLossValue() { return biggestLossValue; }
    public Material getBiggestLossMaterial() { return biggestLossMaterial; }
    public int getCurrentWinStreak() { return currentWinStreak; }
    public int getBestWinStreak() { return bestWinStreak; }
    public Map<String, Integer> getFavoriteWagers() { return new HashMap<>(favoriteWagers); }
    public Map<String, Integer> getFavoriteTargets() { return new HashMap<>(favoriteTargets); }
    public Map<String, Object> getAchievementProgress() { return new HashMap<>(achievementProgress); }
    public long getFirstPlayed() { return firstPlayed; }
    public long getLastPlayed() { return lastPlayed; }

    // --- Mutators ---

    public void recordWager(Material material, double value) {
        totalWageredValue += (long) value;
        totalUpgrades++;
        favoriteWagers.merge(material.name(), 1, Integer::sum);
        lastPlayed = System.currentTimeMillis();
    }

    public void recordWin(Material targetMaterial, int count, double targetValue, double chance) {
        double winValue = targetValue * count;
        totalWonValue += (long) winValue;
        totalWins++;
        currentWinStreak++;
        if (currentWinStreak > bestWinStreak) bestWinStreak = currentWinStreak;

        if (winValue > biggestWinValue) {
            biggestWinValue = (long) winValue;
            biggestWinMaterial = targetMaterial;
            biggestWinCount = count;
            biggestWinChance = chance;
        }

        favoriteTargets.merge(targetMaterial.name(), count, Integer::sum);
        lastPlayed = System.currentTimeMillis();
    }

    public void recordLoss(Material wagerMaterial, double value) {
        totalLosses++;
        currentWinStreak = 0;

        if (value > biggestLossValue) {
            biggestLossValue = (long) value;
            biggestLossMaterial = wagerMaterial;
        }

        lastPlayed = System.currentTimeMillis();
    }

    public void updateAchievementProgress(String key, Object value) {
        achievementProgress.put(key, value);
    }

    public Object getAchievementProgress(String key) {
        return achievementProgress.get(key);
    }

    // --- Serialization ---

    public void saveToConfig(YamlConfiguration config, String path) {
        config.set(path + ".player-id", playerId.toString());
        config.set(path + ".player-name", playerName);
        config.set(path + ".total-wagered-value", totalWageredValue);
        config.set(path + ".total-won-value", totalWonValue);
        config.set(path + ".total-upgrades", totalUpgrades);
        config.set(path + ".total-wins", totalWins);
        config.set(path + ".total-losses", totalLosses);
        config.set(path + ".biggest-win-value", biggestWinValue);
        config.set(path + ".biggest-win-material", biggestWinMaterial.name());
        config.set(path + ".biggest-win-count", biggestWinCount);
        config.set(path + ".biggest-win-chance", biggestWinChance);
        config.set(path + ".biggest-loss-value", biggestLossValue);
        config.set(path + ".biggest-loss-material", biggestLossMaterial.name());
        config.set(path + ".current-win-streak", currentWinStreak);
        config.set(path + ".best-win-streak", bestWinStreak);
        config.set(path + ".first-played", firstPlayed);
        config.set(path + ".last-played", lastPlayed);

        // Favorite wagers
        for (Map.Entry<String, Integer> entry : favoriteWagers.entrySet()) {
            config.set(path + ".favorite-wagers." + entry.getKey(), entry.getValue());
        }
        // Favorite targets
        for (Map.Entry<String, Integer> entry : favoriteTargets.entrySet()) {
            config.set(path + ".favorite-targets." + entry.getKey(), entry.getValue());
        }
        // Achievement progress
        for (Map.Entry<String, Object> entry : achievementProgress.entrySet()) {
            config.set(path + ".achievements." + entry.getKey(), entry.getValue());
        }
    }

    public static PlayerStats loadFromConfig(ConfigurationSection section) {
        if (section == null) return null;
        try {
            UUID playerId = UUID.fromString(section.getString("player-id"));
            String playerName = section.getString("player-name", "Unknown");
            PlayerStats stats = new PlayerStats(playerId, playerName);

            stats.totalWageredValue = section.getLong("total-wagered-value", 0);
            stats.totalWonValue = section.getLong("total-won-value", 0);
            stats.totalUpgrades = section.getInt("total-upgrades", 0);
            stats.totalWins = section.getInt("total-wins", 0);
            stats.totalLosses = section.getInt("total-losses", 0);
            stats.biggestWinValue = section.getLong("biggest-win-value", 0);
            stats.biggestWinMaterial = Material.matchMaterial(section.getString("biggest-win-material", "AIR"));
            if (stats.biggestWinMaterial == null) stats.biggestWinMaterial = Material.AIR;
            stats.biggestWinCount = section.getInt("biggest-win-count", 0);
            stats.biggestWinChance = section.getDouble("biggest-win-chance", 0);
            stats.biggestLossValue = section.getLong("biggest-loss-value", 0);
            stats.biggestLossMaterial = Material.matchMaterial(section.getString("biggest-loss-material", "AIR"));
            if (stats.biggestLossMaterial == null) stats.biggestLossMaterial = Material.AIR;
            stats.currentWinStreak = section.getInt("current-win-streak", 0);
            stats.bestWinStreak = section.getInt("best-win-streak", 0);
            stats.firstPlayed = section.getLong("first-played", System.currentTimeMillis());
            stats.lastPlayed = section.getLong("last-played", System.currentTimeMillis());

            // Favorite wagers
            ConfigurationSection wagers = section.getConfigurationSection("favorite-wagers");
            if (wagers != null) {
                for (String key : wagers.getKeys(false)) {
                    stats.favoriteWagers.put(key, wagers.getInt(key));
                }
            }
            // Favorite targets
            ConfigurationSection targets = section.getConfigurationSection("favorite-targets");
            if (targets != null) {
                for (String key : targets.getKeys(false)) {
                    stats.favoriteTargets.put(key, targets.getInt(key));
                }
            }
            // Achievement progress
            ConfigurationSection achievements = section.getConfigurationSection("achievements");
            if (achievements != null) {
                for (String key : achievements.getKeys(false)) {
                    stats.achievementProgress.put(key, achievements.get(key));
                }
            }

            return stats;
        } catch (Exception e) {
            return null;
        }
    }
}
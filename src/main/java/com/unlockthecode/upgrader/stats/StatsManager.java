package com.unlockthecode.upgrader.stats;

import com.unlockthecode.upgrader.UpgraderPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages persistent player statistics across server restarts.
 * Uses a single YAML file (stats.yml) in the plugin data folder.
 */
public class StatsManager {

    private final UpgraderPlugin plugin;
    private final File statsFile;
    private final YamlConfiguration statsConfig;
    private final Map<UUID, PlayerStats> playerStatsCache = new HashMap<>();

    public StatsManager(UpgraderPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        loadAll();
    }

    /** Load all player stats from disk into memory. */
    private void loadAll() {
        if (!statsFile.exists()) return;

        for (String key : statsConfig.getKeys(false)) {
            var section = statsConfig.getConfigurationSection(key);
            if (section != null) {
                PlayerStats stats = PlayerStats.loadFromConfig(section);
                if (stats != null) {
                    playerStatsCache.put(stats.getPlayerId(), stats);
                }
            }
        }
        plugin.getLogger().info(String.format("Loaded stats for %d players", playerStatsCache.size()));
    }

    /** Get a player's stats, creating new ones if they don't exist. */
    public PlayerStats getStats(Player player) {
        UUID id = player.getUniqueId();
        PlayerStats stats = playerStatsCache.get(id);
        if (stats == null) {
            stats = new PlayerStats(id, player.getName());
            playerStatsCache.put(id, stats);
        } else {
            // Update name in case it changed (keep existing stats in cache)
            // The cached stats are returned; name update could be done via a setter if needed
        }
        return playerStatsCache.get(id);
    }

    /** Get stats by UUID (for offline players / leaderboards). */
    public PlayerStats getStats(UUID uuid) {
        return playerStatsCache.get(uuid);
    }

    /** Save a specific player's stats to disk. */
    public void saveStats(UUID uuid) {
        PlayerStats stats = playerStatsCache.get(uuid);
        if (stats == null) return;

        String path = uuid.toString();
        stats.saveToConfig(statsConfig, path);
        saveConfig();
    }

    /** Save all cached stats to disk. */
    public void saveAll() {
        for (PlayerStats stats : playerStatsCache.values()) {
            stats.saveToConfig(statsConfig, stats.getPlayerId().toString());
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }

    /** Get all cached stats (for leaderboards). */
    public Map<UUID, PlayerStats> getAllStats() {
        return new HashMap<>(playerStatsCache);
    }

    /** Called on plugin disable to ensure all data is saved. */
    public void shutdown() {
        saveAll();
    }
}
package com.unlockthecode.upgrader.stats;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;

/**
 * Leaderboard system for tracking top players across various categories.
 */
public class LeaderboardManager {

    public enum Category {
        BIGGEST_WIN("Biggest Single Win", Material.DIAMOND),
        BIGGEST_LOSS("Biggest Single Loss", Material.REDSTONE),
        BEST_STREAK("Best Win Streak", Material.BLAZE_ROD),
        TOTAL_WAGERED("Total Value Wagered", Material.EMERALD),
        TOTAL_WON("Total Value Won", Material.GOLD_INGOT),
        NET_PROFIT("Net Profit", Material.LIME_DYE),
        TOTAL_WINS("Total Wins", Material.IRON_SWORD),
        WIN_RATE("Win Rate (min 50 games)", Material.CLOCK);

        public final String displayName;
        public final Material icon;

        Category(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    public record Entry(
        UUID playerId,
        String playerName,
        long value,
        String formattedValue,
        int rank
    ) {}

    private final StatsManager statsManager;

    public LeaderboardManager(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    /** Get top N players for a category. */
    public List<Entry> getLeaderboard(Category category, int limit) {
        Map<UUID, PlayerStats> allStats = statsManager.getAllStats();

        List<Entry> entries = allStats.entrySet().stream()
            .map(entry -> createEntry(entry.getKey(), entry.getValue(), category))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(Entry::value).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        // Add ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.set(i, new Entry(
                entries.get(i).playerId(),
                entries.get(i).playerName(),
                entries.get(i).value(),
                entries.get(i).formattedValue(),
                i + 1
            ));
        }

        return entries;
    }

    /** Get a specific player's rank in a category. */
    public OptionalInt getPlayerRank(UUID playerId, Category category) {
        Map<UUID, PlayerStats> allStats = statsManager.getAllStats();

        List<Entry> sorted = allStats.entrySet().stream()
            .map(entry -> createEntry(entry.getKey(), entry.getValue(), category))
            .filter(Objects::nonNull)
            .sorted(comparingLong(Entry::value).reversed())
            .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).playerId().equals(playerId)) {
                return OptionalInt.of(i + 1);
            }
        }
        return OptionalInt.empty();
    }

    private Entry createEntry(UUID playerId, PlayerStats stats, Category category) {
        long value;
        String formatted;

        switch (category) {
            case BIGGEST_WIN -> {
                value = stats.getBiggestWinValue();
                formatted = formatValue(value);
            }
            case BIGGEST_LOSS -> {
                value = stats.getBiggestLossValue();
                formatted = formatValue(value);
            }
            case BEST_STREAK -> {
                value = stats.getBestWinStreak();
                formatted = String.valueOf(value);
            }
            case TOTAL_WAGERED -> {
                value = stats.getTotalWageredValue();
                formatted = formatValue(value);
            }
            case TOTAL_WON -> {
                value = stats.getTotalWonValue();
                formatted = formatValue(value);
            }
            case NET_PROFIT -> {
                value = stats.getNetValue();
                formatted = (value >= 0 ? "+" : "") + formatValue(value);
            }
            case TOTAL_WINS -> {
                value = stats.getTotalWins();
                formatted = String.valueOf(value);
            }
            case WIN_RATE -> {
                if (stats.getTotalUpgrades() < 50) return null;
                value = Math.round(stats.getWinRate() * 10000); // Store as basis points
                formatted = String.format("%.2f%%", stats.getWinRate() * 100);
            }
            default -> {
                return null;
            }
        }

        if (value <= 0 && category != Category.NET_PROFIT) return null;

        String name = stats.getPlayerName();
        // Try to get current name from server if online
        OfflinePlayer offline = org.bukkit.Bukkit.getOfflinePlayer(playerId);
        if (offline.hasPlayedBefore() && offline.getName() != null) {
            name = offline.getName();
        }

        return new Entry(playerId, name, value, formatted, 0);
    }

    /** Get all categories. */
    public List<Category> getCategories() {
        return Arrays.asList(Category.values());
    }

    private String formatValue(long value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }
}
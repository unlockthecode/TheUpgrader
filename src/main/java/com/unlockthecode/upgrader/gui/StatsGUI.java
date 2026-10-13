package com.unlockthecode.upgrader.gui;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.stats.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Statistics GUI for viewing player and server stats.
 */
public class StatsGUI {

    private final UpgraderPlugin plugin;
    private final StatsManager statsManager;
    private final LeaderboardManager leaderboardManager;

    // Layout slots (loaded from config)
    private int slotPlayerStats;
    private int slotLeaderboards;
    private int slotAchievements;
    private int slotServerStats;
    private int slotBack;
    private int slotClose;
    private int[] contentSlots;
    private int slotLeaderboardPrev;
    private int slotLeaderboardBack;
    private int slotLeaderboardIndicator;
    private int slotLeaderboardNext;
    private int slotAchievementBack;
    private int slotAchievementClose;
    private int slotServerStatsBack;
    private int slotServerStatsClose;

    public StatsGUI(UpgraderPlugin plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
        this.leaderboardManager = new LeaderboardManager(statsManager);
        loadConfig();
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void loadConfig() {
        var config = plugin.getUpgraderConfig();
        this.slotPlayerStats = config.statsPlayerStats();
        this.slotLeaderboards = config.statsLeaderboards();
        this.slotAchievements = config.statsAchievements();
        this.slotServerStats = config.statsServerStats();
        this.slotBack = config.statsBack();
        this.slotClose = config.statsClose();
        this.contentSlots = config.statsContentSlots();
        this.slotLeaderboardPrev = config.statsLeaderboardPrev();
        this.slotLeaderboardBack = config.statsLeaderboardBack();
        this.slotLeaderboardIndicator = config.statsLeaderboardIndicator();
        this.slotLeaderboardNext = config.statsLeaderboardNext();
        this.slotAchievementBack = config.statsAchievementBack();
        this.slotAchievementClose = config.statsAchievementClose();
        this.slotServerStatsBack = config.statsServerStatsBack();
        this.slotServerStatsClose = config.statsServerStatsClose();
    }

    public void openMainMenu(Player player) {
        StatsHolder holder = new StatsHolder(StatsHolder.Mode.MAIN, 0);
        Inventory gui = Bukkit.createInventory(holder, 54, Component.text("Statistics", NamedTextColor.DARK_PURPLE));
        holder.inventory(gui);
        drawMainMenu(player, gui);
        player.openInventory(gui);
    }

    public void openPlayerStats(Player player) {
        StatsHolder holder = new StatsHolder(StatsHolder.Mode.PLAYER_STATS, 0);
        Inventory gui = Bukkit.createInventory(holder, 54, Component.text("Your Statistics", NamedTextColor.DARK_PURPLE));
        holder.inventory(gui);
        drawPlayerStats(player, gui);
        player.openInventory(gui);
    }

    public void openLeaderboards(Player player, int page) {
        StatsHolder holder = new StatsHolder(StatsHolder.Mode.LEADERBOARDS, page);
        Inventory gui = Bukkit.createInventory(holder, 54, Component.text("Leaderboards (Page " + (page + 1) + ")", NamedTextColor.DARK_PURPLE));
        holder.inventory(gui);
        drawLeaderboards(player, gui, page);
        player.openInventory(gui);
    }

    public void openAchievements(Player player) {
        StatsHolder holder = new StatsHolder(StatsHolder.Mode.ACHIEVEMENTS, 0);
        Inventory gui = Bukkit.createInventory(holder, 54, Component.text("Achievements", NamedTextColor.DARK_PURPLE));
        holder.inventory(gui);
        drawAchievements(player, gui);
        player.openInventory(gui);
    }

    public void openServerStats(Player player) {
        StatsHolder holder = new StatsHolder(StatsHolder.Mode.SERVER_STATS, 0);
        Inventory gui = Bukkit.createInventory(holder, 54, Component.text("Server Statistics", NamedTextColor.DARK_PURPLE));
        holder.inventory(gui);
        drawServerStats(gui);
        player.openInventory(gui);
    }

    private void drawMainMenu(Player player, Inventory gui) {
        fillBorder(gui);

        // Player stats button
        gui.setItem(slotPlayerStats, createButton(Material.PLAYER_HEAD, Component.text("Your Stats", NamedTextColor.AQUA),
            Component.text("View your personal statistics", NamedTextColor.GRAY),
            Component.text("Total upgrades, wins, losses", NamedTextColor.GRAY),
            Component.text("Biggest win, best streak, net profit", NamedTextColor.GRAY)));

        // Leaderboards button
        gui.setItem(slotLeaderboards, createButton(Material.GOLD_INGOT, Component.text("Leaderboards", NamedTextColor.GOLD),
            Component.text("Compete with other players", NamedTextColor.GRAY),
            Component.text("Biggest wins, best streaks", NamedTextColor.GRAY),
            Component.text("Most value wagered, win rates", NamedTextColor.GRAY)));

        // Achievements button
        PlayerStats stats = statsManager.getStats(player);
        int earned = countEarnedAchievements(stats);
        int total = AchievementManager.getTotalCount();
        gui.setItem(slotAchievements, createButton(Material.NETHER_STAR, Component.text("Achievements", NamedTextColor.LIGHT_PURPLE),
            Component.text("Unlock achievements for rewards", NamedTextColor.GRAY),
            Component.text("Progress: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(earned), NamedTextColor.WHITE))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(total), NamedTextColor.WHITE))));

        // Server stats button
        gui.setItem(slotServerStats, createButton(Material.END_CRYSTAL, Component.text("Server Stats", NamedTextColor.YELLOW),
            Component.text("Global server statistics", NamedTextColor.GRAY),
            Component.text("Total upgrades, biggest jackpots", NamedTextColor.GRAY),
            Component.text("Economy overview", NamedTextColor.GRAY)));

        // Close button
        gui.setItem(slotClose, simpleButton(Material.BARRIER, Component.text("Close", NamedTextColor.RED), Component.text("Close the statistics menu", NamedTextColor.GRAY)));
    }

    private void drawPlayerStats(Player player, Inventory gui) {
        fillBorder(gui);
        PlayerStats stats = statsManager.getStats(player);

        // Overview items
        gui.setItem(contentSlot(0), statItem(Material.EMERALD, Component.text("Total Wagered", NamedTextColor.GREEN),
            Component.text(formatValue(stats.getTotalWageredValue()), NamedTextColor.WHITE),
            Component.text("Total value risked in all upgrades", NamedTextColor.GRAY)));

        gui.setItem(contentSlot(1), statItem(Material.GOLD_INGOT, Component.text("Total Won", NamedTextColor.GOLD),
            Component.text(formatValue(stats.getTotalWonValue()), NamedTextColor.WHITE),
            Component.text("Total value won from upgrades", NamedTextColor.GRAY)));

        gui.setItem(contentSlot(2), statItem(
            stats.getNetValue() >= 0 ? Material.LIME_DYE : Material.RED_DYE,
            stats.getNetValue() >= 0 ? Component.text("Net Profit", NamedTextColor.GREEN) : Component.text("Net Loss", NamedTextColor.RED),
            Component.text((stats.getNetValue() >= 0 ? "+" : "") + formatValue(stats.getNetValue()), NamedTextColor.WHITE),
            Component.text("Won minus wagered", NamedTextColor.GRAY)));

        gui.setItem(contentSlot(3), statItem(Material.IRON_SWORD, Component.text("Total Upgrades", NamedTextColor.WHITE),
            Component.text(String.valueOf(stats.getTotalUpgrades()), NamedTextColor.WHITE),
            Component.text("Total upgrade attempts", NamedTextColor.GRAY)));

        gui.setItem(contentSlot(4), statItem(Material.DIAMOND_SWORD, Component.text("Wins / Losses", NamedTextColor.AQUA),
            Component.text(String.valueOf(stats.getTotalWins()), NamedTextColor.GREEN).append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(stats.getTotalLosses()), NamedTextColor.RED)),
            Component.text("Win rate: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f%%", stats.getWinRate() * 100), NamedTextColor.WHITE))));

        gui.setItem(contentSlot(5), statItem(Material.CLOCK, Component.text("Best Streak", NamedTextColor.YELLOW),
            Component.text(String.valueOf(stats.getBestWinStreak()), NamedTextColor.WHITE),
            Component.text("Longest consecutive win streak", NamedTextColor.GRAY)));

        gui.setItem(contentSlot(6), statItem(Material.FIRE_CHARGE, Component.text("Current Streak", NamedTextColor.GOLD),
            Component.text(String.valueOf(stats.getCurrentWinStreak()), NamedTextColor.WHITE),
            Component.text("Current consecutive wins", NamedTextColor.GRAY)));

        // Biggest win
        Material winMat = stats.getBiggestWinMaterial();
        if (winMat != null && winMat != Material.AIR) {
            gui.setItem(contentSlot(7), statItem(winMat, Component.text("Biggest Win", NamedTextColor.GOLD),
                Component.text(formatValue(stats.getBiggestWinValue()), NamedTextColor.WHITE),
                Component.text(stats.getBiggestWinCount() + "x " + pretty(winMat), NamedTextColor.GRAY),
                Component.text("Chance: " + String.format("%.2f%%", stats.getBiggestWinChance() * 100), NamedTextColor.GRAY)));
        } else {
            gui.setItem(contentSlot(7), statItem(Material.BARRIER, Component.text("Biggest Win", NamedTextColor.GRAY),
                Component.text("None yet", NamedTextColor.WHITE),
                Component.text("Win an upgrade to set a record!", NamedTextColor.GRAY)));
        }

        // Biggest loss
        Material lossMat = stats.getBiggestLossMaterial();
        if (lossMat != null && lossMat != Material.AIR) {
            gui.setItem(contentSlot(8), statItem(lossMat, Component.text("Biggest Loss", NamedTextColor.RED),
                Component.text(formatValue(stats.getBiggestLossValue()), NamedTextColor.WHITE),
                Component.text("Lost: " + pretty(lossMat), NamedTextColor.GRAY)));
        } else {
            gui.setItem(contentSlot(8), statItem(Material.BARRIER, Component.text("Biggest Loss", NamedTextColor.GRAY),
                Component.text("None yet", NamedTextColor.WHITE),
                Component.text("Stay lucky!", NamedTextColor.GRAY)));
        }

        // Favorite wager
        Map<String, Integer> favWagers = stats.getFavoriteWagers();
        if (!favWagers.isEmpty()) {
            String topWager = favWagers.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + "x)")
                .orElse("None");
            Material wagerMat = Material.matchMaterial(favWagers.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey())
                .orElse("STONE"));
            gui.setItem(contentSlot(9), statItem(wagerMat != null ? wagerMat : Material.STONE, Component.text("Favorite Wager", NamedTextColor.AQUA),
                Component.text(topWager, NamedTextColor.WHITE),
                Component.text("Most used item for wagering", NamedTextColor.GRAY)));
        }

        // Favorite target
        Map<String, Integer> favTargets = stats.getFavoriteTargets();
        if (!favTargets.isEmpty()) {
            String topTarget = favTargets.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + "x)")
                .orElse("None");
            Material targetMat = Material.matchMaterial(favTargets.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey())
                .orElse("STONE"));
            gui.setItem(contentSlot(10), statItem(targetMat != null ? targetMat : Material.STONE, Component.text("Favorite Target", NamedTextColor.LIGHT_PURPLE),
                Component.text(topTarget, NamedTextColor.WHITE),
                Component.text("Most targeted item", NamedTextColor.GRAY)));
        }

        // Back button
        gui.setItem(slotBack, simpleButton(Material.ARROW, Component.text("« Back", NamedTextColor.YELLOW), Component.text("Return to main menu", NamedTextColor.GRAY)));
        gui.setItem(slotClose, simpleButton(Material.BARRIER, Component.text("Close", NamedTextColor.RED), Component.text("Close the statistics menu", NamedTextColor.GRAY)));
    }

    /** Content slot by index, clamped to the configured content area. */
    private int contentSlot(int index) {
        return contentSlots[Math.max(0, Math.min(index, contentSlots.length - 1))];
    }

    private void drawLeaderboards(Player player, Inventory gui, int page) {
        fillBorder(gui);
        List<LeaderboardManager.Category> categories = leaderboardManager.getCategories();

        int start = page * 7;
        int end = Math.min(start + 7, categories.size());

        for (int i = start; i < end; i++) {
            LeaderboardManager.Category cat = categories.get(i);
            int slot = contentSlots[i - start];
            List<LeaderboardManager.Entry> top = leaderboardManager.getLeaderboard(cat, 10);

            ItemStack stack = new ItemStack(cat.icon);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(cat.displayName, NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Top 10:", NamedTextColor.GRAY));
                for (int j = 0; j < Math.min(5, top.size()); j++) {
                    LeaderboardManager.Entry e = top.get(j);
                    lore.add(Component.text("  #" + e.rank() + " ", NamedTextColor.WHITE)
                        .append(Component.text(e.playerName(), NamedTextColor.YELLOW))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(e.formattedValue(), NamedTextColor.WHITE)));
                }
                if (top.size() > 5) {
                    lore.add(Component.text("  ... and " + (top.size() - 5) + " more", NamedTextColor.DARK_GRAY));
                }
                // Show player's rank
                var myRank = leaderboardManager.getPlayerRank(player.getUniqueId(), cat);
                if (myRank.isPresent()) {
                    lore.add(Component.empty());
                    lore.add(Component.text("Your rank: #", NamedTextColor.AQUA).append(Component.text(String.valueOf(myRank.getAsInt()), NamedTextColor.WHITE)));
                } else {
                    lore.add(Component.empty());
                    lore.add(Component.text("You are not ranked yet", NamedTextColor.GRAY));
                }
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            gui.setItem(slot, stack);
        }

        // Navigation
        if (page > 0) {
            gui.setItem(slotLeaderboardPrev, simpleButton(Material.ARROW, Component.text("« Previous", NamedTextColor.YELLOW), Component.text("Previous page", NamedTextColor.GRAY)));
        }
        gui.setItem(slotLeaderboardBack, simpleButton(Material.ARROW, Component.text("« Back", NamedTextColor.YELLOW), Component.text("Return to main menu", NamedTextColor.GRAY)));
        gui.setItem(slotLeaderboardIndicator, simpleButton(Material.PAPER, Component.text("Page " + (page + 1) + " / " + ((categories.size() + 6) / 7), NamedTextColor.GRAY), Component.empty()));
        if (end < categories.size()) {
            gui.setItem(slotLeaderboardNext, simpleButton(Material.ARROW, Component.text("Next »", NamedTextColor.YELLOW), Component.text("Next page", NamedTextColor.GRAY)));
        }
    }

    private void drawAchievements(Player player, Inventory gui) {
        fillBorder(gui);
        PlayerStats stats = statsManager.getStats(player);
        java.util.Set<String> unlocked = getUnlockedAchievements(stats);
        List<AchievementManager.Achievement> all = AchievementManager.getAllAchievements();

        for (int i = 0; i < Math.min(all.size(), contentSlots.length); i++) {
            AchievementManager.Achievement ach = all.get(i);
            boolean earned = unlocked.contains(ach.id());
            gui.setItem(contentSlots[i], achievementItem(ach, earned));
        }

        gui.setItem(slotAchievementBack, simpleButton(Material.ARROW, Component.text("« Back", NamedTextColor.YELLOW), Component.text("Return to main menu", NamedTextColor.GRAY)));
        gui.setItem(slotAchievementClose, simpleButton(Material.BARRIER, Component.text("Close", NamedTextColor.RED), Component.text("Close the achievements menu", NamedTextColor.GRAY)));
    }

    private void drawServerStats(Inventory gui) {
        fillBorder(gui);
        Map<UUID, PlayerStats> allStats = statsManager.getAllStats();

        long totalPlayers = allStats.size();
        long totalUpgrades = allStats.values().stream().mapToLong(PlayerStats::getTotalUpgrades).sum();
        long totalWagered = allStats.values().stream().mapToLong(PlayerStats::getTotalWageredValue).sum();
        long totalWon = allStats.values().stream().mapToLong(PlayerStats::getTotalWonValue).sum();
        long biggestWin = allStats.values().stream().mapToLong(PlayerStats::getBiggestWinValue).max().orElse(0);
        long bestStreak = allStats.values().stream().mapToInt(PlayerStats::getBestWinStreak).max().orElse(0);

        // Find biggest winner
        var biggestWinner = allStats.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Long.compare(a.getBiggestWinValue(), b.getBiggestWinValue())))
            .orElse(null);

        gui.setItem(contentSlot(0), statItem(Material.PLAYER_HEAD, Component.text("Total Players", NamedTextColor.AQUA), Component.text(String.valueOf(totalPlayers), NamedTextColor.WHITE), Component.text("Players who have used the upgrader", NamedTextColor.GRAY)));
        gui.setItem(contentSlot(1), statItem(Material.IRON_SWORD, Component.text("Total Upgrades", NamedTextColor.WHITE), Component.text(formatValue(totalUpgrades), NamedTextColor.WHITE), Component.text("All upgrades attempted server-wide", NamedTextColor.GRAY)));
        gui.setItem(contentSlot(2), statItem(Material.EMERALD, Component.text("Total Wagered", NamedTextColor.GREEN), Component.text(formatValue(totalWagered), NamedTextColor.WHITE), Component.text("Total value risked by all players", NamedTextColor.GRAY)));
        gui.setItem(contentSlot(3), statItem(Material.GOLD_INGOT, Component.text("Total Won", NamedTextColor.GOLD), Component.text(formatValue(totalWon), NamedTextColor.WHITE), Component.text("Total value won by all players", NamedTextColor.GRAY)));
        gui.setItem(contentSlot(4), statItem(Material.DIAMOND, Component.text("Server Net", NamedTextColor.AQUA), Component.text((totalWon - totalWagered >= 0 ? "+" : "") + formatValue(totalWon - totalWagered), NamedTextColor.WHITE), Component.text("Server's net profit/loss", NamedTextColor.GRAY)));
        gui.setItem(contentSlot(5), statItem(Material.NETHER_STAR, Component.text("Biggest Jackpot", NamedTextColor.LIGHT_PURPLE), Component.text(formatValue(biggestWin), NamedTextColor.WHITE),
            Component.text(biggestWinner != null ? "By: " + biggestWinner.getValue().getPlayerName() : "None yet", NamedTextColor.GRAY)));
        gui.setItem(contentSlot(6), statItem(Material.BLAZE_ROD, Component.text("Best Streak", NamedTextColor.GOLD), Component.text(String.valueOf(bestStreak), NamedTextColor.WHITE),
            Component.text("Longest win streak server-wide", NamedTextColor.GRAY)));

        gui.setItem(slotServerStatsBack, simpleButton(Material.ARROW, Component.text("« Back", NamedTextColor.YELLOW), Component.text("Return to main menu", NamedTextColor.GRAY)));
        gui.setItem(slotServerStatsClose, simpleButton(Material.BARRIER, Component.text("Close", NamedTextColor.RED), Component.text("Close the statistics menu", NamedTextColor.GRAY)));
    }

    public void handleClick(Player player, int slot, ClickType click) {
        StatsHolder holder = currentHolder(player);
        StatsHolder.Mode mode = holder != null ? holder.mode : StatsHolder.Mode.MAIN;

        switch (mode) {
            case MAIN -> {
                if (slot == slotClose) {
                    player.closeInventory();
                } else if (slot == slotPlayerStats) {
                    openPlayerStats(player);
                } else if (slot == slotLeaderboards) {
                    openLeaderboards(player, 0);
                } else if (slot == slotAchievements) {
                    openAchievements(player);
                } else if (slot == slotServerStats) {
                    openServerStats(player);
                }
            }
            case PLAYER_STATS -> {
                if (slot == slotClose) {
                    player.closeInventory();
                } else if (slot == slotBack) {
                    openMainMenu(player);
                }
            }
            case LEADERBOARDS -> {
                if (slot == slotLeaderboardBack) {
                    openMainMenu(player);
                } else if (slot == slotLeaderboardPrev && holder != null && holder.page > 0) {
                    openLeaderboards(player, holder.page - 1);
                } else if (slot == slotLeaderboardNext && holder != null) {
                    openLeaderboards(player, holder.page + 1);
                }
            }
            case ACHIEVEMENTS -> {
                if (slot == slotAchievementClose) {
                    player.closeInventory();
                } else if (slot == slotAchievementBack) {
                    openMainMenu(player);
                }
            }
            case SERVER_STATS -> {
                if (slot == slotServerStatsClose) {
                    player.closeInventory();
                } else if (slot == slotServerStatsBack) {
                    openMainMenu(player);
                }
            }
        }
    }

    private StatsHolder currentHolder(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof StatsHolder holder) {
            return holder;
        }
        return null;
    }

    // --- Helpers ---

    private void fillBorder(Inventory gui) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            border.setItemMeta(meta);
        }
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(45 + i, border);
        }
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border);
            gui.setItem(row * 9 + 8, border);
        }
    }

    private ItemStack createButton(Material icon, Component name, Component... lore) {
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack statItem(Material icon, Component name, Component value, Component... extra) {
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            List<Component> lore = new ArrayList<>();
            lore.add(value);
            if (extra != null) {
                lore.addAll(Arrays.asList(extra));
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack achievementItem(AchievementManager.Achievement ach, boolean earned) {
        ItemStack stack = new ItemStack(ach.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            Component prefix = earned
                ? Component.text("", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                : Component.text("", NamedTextColor.DARK_GRAY);
            meta.displayName(prefix.append(Component.text(ach.name())));
            List<Component> lore = new ArrayList<>();
            if (earned) {
                lore.add(Component.text("✓ UNLOCKED", ach.rarity().color).decorate(TextDecoration.BOLD));
            } else {
                lore.add(Component.text(ach.description(), NamedTextColor.GRAY));
                lore.add(Component.empty());
                lore.add(Component.text("Rarity: ", NamedTextColor.GRAY).append(Component.text(ach.rarity().label, ach.rarity().color)));
            }
            meta.lore(lore);
            if (earned) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LOYALTY, 1, false);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack simpleButton(Material icon, Component name, Component desc) {
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(Collections.singletonList(desc));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private int countEarnedAchievements(PlayerStats stats) {
        java.util.Set<String> unlocked = getUnlockedAchievements(stats);
        return unlocked.size();
    }

    private java.util.Set<String> getUnlockedAchievements(PlayerStats stats) {
        java.util.Set<String> unlocked = new java.util.HashSet<>();
        for (AchievementManager.Achievement ach : AchievementManager.getAllAchievements()) {
            if (ach.check().apply(stats)) {
                unlocked.add(ach.id());
            }
        }
        return unlocked;
    }

    private String formatValue(long value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }

    private String pretty(Material material) {
        if (material == null) return "item";
        StringBuilder sb = new StringBuilder();
        for (String part : material.name().toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public static final class StatsHolder implements InventoryHolder {
        public enum Mode { MAIN, PLAYER_STATS, LEADERBOARDS, ACHIEVEMENTS, SERVER_STATS }
        public final Mode mode;
        public final int page;
        private Inventory inventory;

        public StatsHolder(Mode mode, int page) {
            this.mode = mode;
            this.page = page;
        }

        void inventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
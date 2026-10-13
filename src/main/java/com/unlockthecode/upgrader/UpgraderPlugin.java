package com.unlockthecode.upgrader;

import com.unlockthecode.upgrader.config.UpgraderConfig;
import com.unlockthecode.upgrader.gui.UpgraderGUI;
import com.unlockthecode.upgrader.gui.StatsGUI;
import com.unlockthecode.upgrader.stats.StatsManager;
import com.unlockthecode.upgrader.value.ItemValues;
import com.unlockthecode.upgrader.command.UpgraderCommand;
import com.unlockthecode.upgrader.listener.GUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class UpgraderPlugin extends JavaPlugin {

    private static UpgraderPlugin instance;
    private UpgraderConfig config;
    private UpgraderGUI upgraderGUI;
    private StatsGUI statsGUI;
    private ItemValues itemValues;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load configuration
        this.config = new UpgraderConfig(this);
        this.config.load();

        // Initialize item values calculator
        this.itemValues = new ItemValues(this);

        // Initialize stats manager
        this.statsManager = new StatsManager(this);

        // Register GUI
        this.upgraderGUI = new UpgraderGUI(this);
        this.statsGUI = new StatsGUI(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Register commands
        getCommand("upgrade").setExecutor(new UpgraderCommand(this));

        getLogger().info("TheUpgrader plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            statsManager.shutdown();
        }
        getLogger().info("TheUpgrader plugin disabled!");
    }

    public static UpgraderPlugin getInstance() {
        return instance;
    }

    public UpgraderConfig getUpgraderConfig() {
        return config;
    }

    public UpgraderGUI getUpgraderGUI() {
        return upgraderGUI;
    }

    public ItemValues getItemValues() {
        return itemValues;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public StatsGUI getStatsGUI() {
        return statsGUI;
    }

    public void reloadPlugin() {
        reloadConfig();
        config.load();
        ItemValues.invalidateCache();
        statsGUI.reloadConfig();
    }
}
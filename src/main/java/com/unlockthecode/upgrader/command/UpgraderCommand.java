package com.unlockthecode.upgrader.command;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.config.UpgraderConfig;
import com.unlockthecode.upgrader.gui.UpgraderGUI;
import com.unlockthecode.upgrader.gui.StatsGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UpgraderCommand implements CommandExecutor, TabCompleter {

    private final UpgraderPlugin plugin;
    private final UpgraderConfig config;
    private final UpgraderGUI upgraderGUI;
    private final StatsGUI statsGUI;

    public UpgraderCommand(UpgraderPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getUpgraderConfig();
        this.upgraderGUI = plugin.getUpgraderGUI();
        this.statsGUI = new StatsGUI(plugin);
    }

    @Override
    @SuppressWarnings("null")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // Only handle /upgrade command
        if (!cmdName.equals("upgrade")) {
            return false; // Let other commands pass through
        }

        if (args.length == 0) {
            // /upgrade with no args - open GUI
            if (!sender.hasPermission("theupgrader.use")) {
                sender.sendMessage(error("No permission!"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(error("This command can only be run by a player!"));
                return true;
            }
            upgraderGUI.openGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "config" -> {
                if (!sender.hasPermission("theupgrader.admin")) {
                    sender.sendMessage(error("No permission!"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(error("Config GUI can only be opened by a player!"));
                    return true;
                }
                upgraderGUI.openGUI((Player) sender);
            }
            case "stats" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(error("This command can only be run by a player!"));
                    return true;
                }
                statsGUI.openMainMenu(player);
            }
            case "reload" -> {
                if (!sender.hasPermission("theupgrader.admin")) {
                    sender.sendMessage(error("No permission!"));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(success("Upgrader plugin reloaded!"));
            }
            case "set" -> {
                if (!sender.hasPermission("theupgrader.admin")) {
                    sender.sendMessage(error("No permission!"));
                    return true;
                }
                handleSetCommand(sender, args);
            }
            case "get" -> {
                if (!sender.hasPermission("theupgrader.admin")) {
                    sender.sendMessage(error("No permission!"));
                    return true;
                }
                handleGetCommand(sender, args);
            }
            default -> {
                sender.sendMessage(error("Unknown subcommand: " + subCommand));
                sendHelp(sender);
            }
        }
        return true;
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /upgrade set <setting> <value>"));
            sender.sendMessage(warning("Settings: house-edge, min-chance, max-chance, spin-ticks, max-target-count,"));
            sender.sendMessage(warning("  attempt-limit, attempt-window, global-multiplier"));
            return;
        }

        String setting = args[1].toLowerCase();
        String value = args[2];

        try {
            switch (setting) {
                case "house-edge" -> config.houseEdge = Double.parseDouble(value);
                case "min-chance" -> config.minChance = Double.parseDouble(value);
                case "max-chance" -> config.maxChance = Double.parseDouble(value);
                case "spin-ticks" -> config.spinTicks = Integer.parseInt(value);
                case "max-target-count" -> config.maxTargetCount = Integer.parseInt(value);
                case "attempt-limit" -> config.attemptLimit = Integer.parseInt(value);
                case "attempt-window" -> config.attemptWindowSeconds = Integer.parseInt(value);
                case "global-multiplier" -> config.globalValueMultiplier = Double.parseDouble(value);
                case "override" -> {
                    if (args.length < 4) {
                        sender.sendMessage(error("Usage: /upgrade set override <item> <value>"));
                        return;
                    }
                    String item = args[2].toLowerCase();
                    config.valueOverrides.put(item, Double.parseDouble(args[3]));
                }
                case "multiplier" -> {
                    if (args.length < 4) {
                        sender.sendMessage(error("Usage: /upgrade set multiplier <item> <value>"));
                        return;
                    }
                    String item = args[2].toLowerCase();
                    config.valueMultipliers.put(item, Double.parseDouble(args[3]));
                }
                case "blacklist" -> {
                    if (args.length < 4) {
                        sender.sendMessage(error("Usage: /upgrade set blacklist <add|remove> <item>"));
                        return;
                    }
                    String action = args[2].toLowerCase();
                    String item = args[3].toLowerCase();
                    switch (action) {
                        case "add" -> config.blacklist.add(item);
                        case "remove" -> config.blacklist.remove(item);
                        default -> {
                            sender.sendMessage(error("Action must be 'add' or 'remove'"));
                            return;
                        }
                    }
                }
                default -> {
                    sender.sendMessage(error("Unknown setting: " + setting));
                    return;
                }
            }
            config.save();
            sender.sendMessage(success("Setting updated and saved!"));
        } catch (NumberFormatException e) {
            sender.sendMessage(error("Invalid number format!"));
        }
    }

    private void handleGetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /upgrade get <setting>"));
            return;
        }

        String setting = args[1].toLowerCase();

        switch (setting) {
            case "house-edge" -> sender.sendMessage(info("House Edge: ").append(value(String.valueOf(config.houseEdge))));
            case "min-chance" -> sender.sendMessage(info("Min Chance: ").append(value(String.valueOf(config.minChance))));
            case "max-chance" -> sender.sendMessage(info("Max Chance: ").append(value(String.valueOf(config.maxChance))));
            case "spin-ticks" -> sender.sendMessage(info("Spin Ticks: ").append(value(String.valueOf(config.spinTicks))));
            case "max-target-count" -> sender.sendMessage(info("Max Target Count: ").append(value(String.valueOf(config.maxTargetCount))));
            case "attempt-limit" -> sender.sendMessage(info("Attempt Limit: ").append(value(String.valueOf(config.attemptLimit))));
            case "attempt-window" -> sender.sendMessage(info("Attempt Window: ").append(value(config.attemptWindowSeconds + "s")));
            case "global-multiplier" -> sender.sendMessage(info("Global Multiplier: ").append(value(String.valueOf(config.globalValueMultiplier))));
            case "overrides" -> {
                sender.sendMessage(info("Value Overrides:"));
                config.valueOverrides.forEach((k, v) -> sender.sendMessage(Component.text("  " + k + " = " + v)));
            }
            case "multipliers" -> {
                sender.sendMessage(info("Value Multipliers:"));
                config.valueMultipliers.forEach((k, v) -> sender.sendMessage(Component.text("  " + k + " = " + v)));
            }
            case "blacklist" -> {
                sender.sendMessage(info("Blacklist:"));
                config.blacklist.forEach(item -> sender.sendMessage(Component.text("  " + item)));
            }
            case "all" -> {
                sender.sendMessage(header("=== Upgrader Config ==="));
                sender.sendMessage(Component.text("House Edge: " + config.houseEdge));
                sender.sendMessage(Component.text("Min Chance: " + config.minChance));
                sender.sendMessage(Component.text("Max Chance: " + config.maxChance));
                sender.sendMessage(Component.text("Spin Ticks: " + config.spinTicks));
                sender.sendMessage(Component.text("Max Target Count: " + config.maxTargetCount));
                sender.sendMessage(Component.text("Attempt Limit: " + config.attemptLimit));
                sender.sendMessage(Component.text("Attempt Window: " + config.attemptWindowSeconds + "s"));
                sender.sendMessage(Component.text("Global Multiplier: " + config.globalValueMultiplier));
                sender.sendMessage(Component.text("Overrides: " + config.valueOverrides.size()));
                sender.sendMessage(Component.text("Multipliers: " + config.valueMultipliers.size()));
                sender.sendMessage(Component.text("Blacklist: " + config.blacklist.size()));
            }
            default -> sender.sendMessage(error("Unknown setting: " + setting));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(header("=== Upgrader Commands ==="));
        sender.sendMessage(cmd("/upgrade ").append(desc("- Open the upgrade GUI")));
        sender.sendMessage(cmd("/upgrade config ").append(desc("- Open the upgrade GUI (admin only)")));
        sender.sendMessage(cmd("/upgrade stats ").append(desc("- Open statistics GUI")));
        sender.sendMessage(cmd("/upgrade reload ").append(desc("- Reload plugin config (admin only)")));
        sender.sendMessage(cmd("/upgrade set <setting> <value> ").append(desc("- Set a config value (admin only)")));
        sender.sendMessage(cmd("/upgrade get <setting> ").append(desc("- Get a config value (admin only)")));
        sender.sendMessage(desc("Settings: house-edge, min-chance, max-chance, spin-ticks, max-target-count,"));
        sender.sendMessage(desc("  attempt-limit, attempt-window, global-multiplier, override, multiplier, blacklist"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        // Only handle /upgrade command
        if (!cmdName.equals("upgrade")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("config", "stats", "reload", "set", "get"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                completions.addAll(Arrays.asList("house-edge", "min-chance", "max-chance", "spin-ticks",
                        "max-target-count", "attempt-limit", "attempt-window", "global-multiplier",
                        "override", "multiplier", "blacklist"));
            } else if (args[0].equalsIgnoreCase("get")) {
                completions.addAll(Arrays.asList("house-edge", "min-chance", "max-chance", "spin-ticks",
                        "max-target-count", "attempt-limit", "attempt-window", "global-multiplier",
                        "overrides", "multipliers", "blacklist", "all"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (args[1].equalsIgnoreCase("override") || args[1].equalsIgnoreCase("multiplier")) {
                // Could add material completions here
            } else if (args[1].equalsIgnoreCase("blacklist")) {
                completions.addAll(Arrays.asList("add", "remove"));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    // Helper methods for Adventure Components
    private Component error(String msg) {
        return Component.text(msg, NamedTextColor.RED);
    }

    private Component success(String msg) {
        return Component.text(msg, NamedTextColor.GREEN);
    }

    private Component warning(String msg) {
        return Component.text(msg, NamedTextColor.YELLOW);
    }

    private Component info(String msg) {
        return Component.text(msg, NamedTextColor.GREEN);
    }

    private Component value(String msg) {
        return Component.text(msg, NamedTextColor.WHITE);
    }

    private Component header(String msg) {
        return Component.text(msg, NamedTextColor.GOLD);
    }

    private Component cmd(String msg) {
        return Component.text(msg, NamedTextColor.YELLOW);
    }

    private Component desc(String msg) {
        return Component.text(msg, NamedTextColor.GRAY);
    }
}
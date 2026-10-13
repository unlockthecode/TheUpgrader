package com.unlockthecode.upgrader.value;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.config.UpgraderConfig;
import org.bukkit.inventory.ItemStack;

public class UpgradeOdds {

    private final UpgraderConfig config;
    private final ItemValues itemValues;

    public UpgradeOdds(UpgraderPlugin plugin) {
        this.config = plugin.getUpgraderConfig();
        this.itemValues = plugin.getItemValues();
    }

    public double chance(ItemStack input, org.bukkit.Material target, int targetCount) {
        if (input == null || input.getType() == org.bukkit.Material.AIR || target == null || targetCount <= 0) {
            return 0.0;
        }
        double in = itemValues.stackValue(input);
        double out = itemValues.unitValue(target) * targetCount;
        return chance(in, out);
    }

    public double chance(double inputValue, double targetValue) {
        if (inputValue <= 0.0 || targetValue <= 0.0) {
            return 0.0;
        }
        double raw = config.houseEdge * inputValue / targetValue;
        return Math.max(config.minChance, Math.min(config.maxChance, raw));
    }
}
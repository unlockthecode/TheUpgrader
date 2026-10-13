package com.unlockthecode.upgrader.listener;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.config.UpgraderConfig;
import com.unlockthecode.upgrader.gui.UpgraderGUI;
import com.unlockthecode.upgrader.gui.StatsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;

public class GUIListener implements Listener {

    private final UpgraderGUI upgraderGUI;
    private final StatsGUI statsGUI;
    private final UpgraderConfig config;

    public GUIListener(UpgraderPlugin plugin) {
        this.upgraderGUI = plugin.getUpgraderGUI();
        this.statsGUI = plugin.getStatsGUI();
        this.config = plugin.getUpgraderConfig();
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();

        // Handle StatsGUI clicks
        if (top.getHolder() instanceof StatsGUI.StatsHolder) {
            statsGUI.handleClick(player, event.getRawSlot(), event.getClick());
            event.setCancelled(true);
            return;
        }

        if (!(top.getHolder() instanceof UpgraderGUI.UpgraderHolder holder)) return;

        int raw = event.getRawSlot();

        // Handle clicks in the GUI (top inventory)
        if (raw < top.getSize()) {
            // UPGRADER MODE: Handle button clicks and drops on input/target slots
            if (holder.mode == UpgraderGUI.UpgraderHolder.Mode.UPGRADER) {
                if (isButtonSlot(holder.mode, raw)) {
                    // Button clicks (upgrade, close, catalog, +/- count, presets)
                    upgraderGUI.handleUpgraderClick(player, holder.state, raw, event.getClick());
                    // Cancel to prevent item pickup/swap on button slots
                    event.setCancelled(true);
                } else if (raw == config.slotWager() || raw == config.slotTarget()) {
                    // Wager/Target slot - always cancel to prevent swap with display item
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        // Drop: cursor has item - take it as wager/target
                        event.setCancelled(true);
                        if (raw == config.slotWager()) {
                            upgraderGUI.handleWagerDrop(player, holder.state, cursor);
                        } else {
                            upgraderGUI.handleTargetDrop(player, holder.state, cursor);
                        }
                        // Clear cursor - item is now consumed as wager/target
                        event.setCursor(null);
                    } else {
                        // Click: cursor empty - cancel to prevent picking up display item
                        event.setCancelled(true);
                    }
                } else {
                    // Cancel clicks on other GUI slots (arrow, info, etc.) to prevent item removal
                    event.setCancelled(true);
                }
            }
            // CATALOG MODE: Handle ALL clicks via handleCatalogClick, then cancel to prevent item pickup
            else if (holder.mode == UpgraderGUI.UpgraderHolder.Mode.CATALOG) {
                upgraderGUI.handleCatalogClick(player, holder.state, raw, event.getCurrentItem());
                event.setCancelled(true); // Prevent taking items from catalogue
            }
        }
        // Handle clicks in player's inventory (bottom inventory)
        else if (holder.mode == UpgraderGUI.UpgraderHolder.Mode.UPGRADER) {
            // Allow inventory clicks to proceed - we rely on drag and drop for item selection
            // The old click-based wager/target selection is disabled
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof UpgraderGUI.UpgraderHolder)) {
            return;
        }

        // Check if drag involves wager/target slots in the top inventory (GUI)
        // Prevent dragging items OUT of the display-only wager/target slots
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                if (slot == config.slotWager() || slot == config.slotTarget()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        // Allow other drags
        event.setCancelled(false);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof UpgraderGUI.UpgraderHolder holder) {
            upgraderGUI.handleClose(player, holder.state);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Safety net; the close event fires first on disconnect and normally covers this.
        upgraderGUI.handleQuit(event.getPlayer());
    }

    /** Check if a slot contains a button that should respond to clicks. */
    private boolean isButtonSlot(UpgraderGUI.UpgraderHolder.Mode mode, int slot) {
        if (mode == UpgraderGUI.UpgraderHolder.Mode.UPGRADER) {
            // Upgrader view buttons
            return slot == config.slotUpgrade() ||
                   slot == config.slotClose() ||
                   slot == config.slotCatalog() ||
                   slot == config.slotStats() ||
                   slot == config.slotMinus32() ||
                   slot == config.slotMinus16() ||
                   slot == config.slotMinus1() ||
                   slot == config.slotCount() ||
                   slot == config.slotPlus1() ||
                   slot == config.slotPlus16() ||
                   slot == config.slotPlus32() ||
                   slot == config.slotInfo() || // Win streak display (right-click for info)
                   (slot >= 19 && slot <= 25); // Preset buttons: ×2, ×4, ×8, 30%, 50%, 70%
        } else {
            // Catalog view buttons
            return slot == config.catalogBack() ||
                   slot == config.catalogPrev() ||
                   slot == config.catalogNext();
        }
    }
}

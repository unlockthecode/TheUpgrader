package com.unlockthecode.upgrader.gui;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.config.UpgraderConfig;
import com.unlockthecode.upgrader.listener.AttemptLimiter;
import com.unlockthecode.upgrader.stats.AchievementManager;
import com.unlockthecode.upgrader.stats.PlayerStats;
import com.unlockthecode.upgrader.stats.StatsManager;
import com.unlockthecode.upgrader.util.VersionUtil;
import com.unlockthecode.upgrader.value.ItemCategory;
import com.unlockthecode.upgrader.value.ItemValues;
import com.unlockthecode.upgrader.value.UpgradeOdds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chest-GUI front end for the upgrader.
 *
 * The GUI is display-only: every click inside the view is cancelled. Wagers are
 * picked by left-clicking an item in the player's own inventory, targets by
 * right-clicking one (or via the presets/catalog). The wagered stack stays in
 * the player's inventory until UPGRADE is pressed, at which point it is
 * consumed up-front and the payout is delivered after the spin.
 */
public class UpgraderGUI {

    // Layout slots for the 6x9 upgrader view - will be loaded from config
    private int slotInfo;
    private int slotInput;
    private int slotArrow;
    private int slotTarget;
    private int slotStats;
    private int slotMinus32;
    private int slotMinus16;
    private int slotMinus1;
    private int slotCount;
    private int slotPlus1;
    private int slotPlus16;
    private int slotPlus32;
    private int slotCatalog;
    private int slotUpgrade;
    private int slotClose;

    // Catalog layout
    private static final int CATALOG_ITEMS_PER_PAGE = 45;
    private int catalogPrev;
    private int catalogIndicator;
    private int catalogBack;
    private int catalogNext;
    private int[] categorySlots;
    private int[] spinSlots;

    // Expanded pool of materials for the crate animation (more variety)
    private static final Material[] CRATE_CYCLE = {
            Material.COBBLESTONE, Material.STONE, Material.COAL, Material.COPPER_INGOT,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT,
            Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING
    };

    // Cache the "all" category for performance
    private ItemCategory allCategory;
    private List<UpgraderConfig.Preset> presets;

    private final UpgraderPlugin plugin;
    private final ItemValues itemValues;
    private final UpgradeOdds upgradeOdds;
    private final AttemptLimiter attemptLimiter;

    public UpgraderGUI(UpgraderPlugin plugin) {
        this.plugin = plugin;
        this.itemValues = plugin.getItemValues();
        this.upgradeOdds = new UpgradeOdds(plugin);
        this.attemptLimiter = new AttemptLimiter(plugin);
        loadConfig();
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void loadConfig() {
        UpgraderConfig config = plugin.getUpgraderConfig();
        this.slotInfo = config.slotInfo();
        this.slotInput = config.slotWager();
        this.slotArrow = config.slotArrow();
        this.slotTarget = config.slotTarget();
        this.slotStats = config.slotStats();
        this.slotMinus32 = config.slotMinus32();
        this.slotMinus16 = config.slotMinus16();
        this.slotMinus1 = config.slotMinus1();
        this.slotCount = config.slotCount();
        this.slotPlus1 = config.slotPlus1();
        this.slotPlus16 = config.slotPlus16();
        this.slotPlus32 = config.slotPlus32();
        this.slotCatalog = config.slotCatalog();
        this.slotUpgrade = config.slotUpgrade();
        this.slotClose = config.slotClose();
        this.categorySlots = config.categorySlots();
        this.catalogPrev = config.catalogPrev();
        this.catalogIndicator = config.catalogIndicator();
        this.catalogBack = config.catalogBack();
        this.catalogNext = config.catalogNext();
        this.spinSlots = config.spinSlots();
        this.presets = config.getPresets();

        // Find the "all" category
        this.allCategory = config.getCategories().stream()
                .filter(c -> c.id().equals("all"))
                .findFirst()
                .orElseGet(() -> {
                    // Fallback to the default "all" category if not found in config
                    return ItemCategory.defaults().stream()
                            .filter(c -> c.id().equals("all"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Default 'all' category missing"));
                });
    }

    // ---------------------------------------------------------------- state

    /** Per-open GUI state; carried inside the inventory holder. */
    public static class GUIState {
        ItemStack inputItem = null;
        double inputValue = 0;
        Material targetMaterial = null;
        double targetValue = 0;
        int targetCount = 1;
        ItemCategory catalogCategory = null; // Will be set to "all" category in openGUI
        int catalogPage = 0;
        public boolean viewingCategories = true; // true = category selection screen, false = item grid
        boolean spinning = false;
        boolean pendingWin = false;
        double spinChance = 0;
        BukkitTask spinTask = null;
        int winStreak = 0; // Track consecutive wins
        boolean isSwitchingViews = false; // Prevent item return when switching between upgrader/catalog

        // Preset cycle tracking: preset slot -> current cycle index
        private final java.util.Map<Integer, Integer> presetCycleIndex = new java.util.HashMap<>();

        int getPresetCycleIndex(int presetSlot) {
            return presetCycleIndex.getOrDefault(presetSlot, 0);
        }

        void incrementPresetCycleIndex(int presetSlot) {
            int current = presetCycleIndex.getOrDefault(presetSlot, 0);
            presetCycleIndex.put(presetSlot, current + 1);
        }
    }

    /** Marks inventories owned by this GUI so the listener can route clicks. */
    public static final class UpgraderHolder implements InventoryHolder {
        public enum Mode { UPGRADER, CATALOG }

        public final Mode mode;
        public final GUIState state;
        public final int page;
        private Inventory inventory;

        UpgraderHolder(Mode mode, GUIState state, int page) {
            this.mode = mode;
            this.state = state;
            this.page = page;
        }

        void inventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // ---------------------------------------------------------------- views

    public void openGUI(Player player) {
        GUIState state = new GUIState();
        state.catalogCategory = allCategory;
        openUpgraderView(player, state);
    }

    private void openUpgraderView(Player player, GUIState state) {
        // Prevent handleClose from returning items when switching views
        state.isSwitchingViews = true;
        UpgraderHolder holder = new UpgraderHolder(UpgraderHolder.Mode.UPGRADER, state, 0);
        Inventory gui = Bukkit.createInventory(holder, 54, Component.empty());
        holder.inventory(gui);
        drawUpgrader(player, gui, state);
        player.openInventory(gui);
        // Reset flag after inventory is opened
        state.isSwitchingViews = false;
    }

    public void openCatalog(Player player, GUIState state) {
        // Prevent handleClose from returning items when switching views
        state.isSwitchingViews = true;
        UpgraderHolder holder = new UpgraderHolder(UpgraderHolder.Mode.CATALOG, state, 0);
        Component title = state.viewingCategories
                ? Component.text("Select Category", NamedTextColor.DARK_PURPLE)
                : getCategoryTitle(state.catalogCategory);
        Inventory gui = Bukkit.createInventory(holder, 54, title);
        holder.inventory(gui);

        if (state.viewingCategories) {
            // Draw category selection screen
            drawCategorySelectionScreen(gui);
        } else {
            // Draw item grid for the selected category
            drawItemGridScreen(gui, state);
        }

        player.openInventory(gui);
        // Reset flag after inventory is opened
        state.isSwitchingViews = false;
    }

    private void drawCategorySelectionScreen(Inventory gui) {
        // Draw category buttons from config
        int[] catSlots = categorySlots;
        List<ItemCategory> categories = plugin.getUpgraderConfig().getCategories();

        for (int i = 0; i < Math.min(catSlots.length, categories.size()); i++) {
            gui.setItem(catSlots[i], categoryTab(categories.get(i), false));
        }

        // Add "Back" button
        gui.setItem(45, simpleButton(Material.ARROW, Component.text("« Back", NamedTextColor.YELLOW), "Return to main menu"));
    }

    private void drawItemGridScreen(Inventory gui, GUIState state) {
        List<Material> catalog = itemValues.catalog(state.catalogCategory);
        int pages = Math.max(1, (catalog.size() + CATALOG_ITEMS_PER_PAGE - 1) / CATALOG_ITEMS_PER_PAGE);
        int page = Math.max(0, Math.min(state.catalogPage, pages - 1));
        state.catalogPage = page;

        ItemStack border = borderItem();
        // Only set border in bottom row (inventory row 5, slots 45-53)
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, border);
        }

        // Draw items in a full grid: slots 0-44 (45 items per page)
        int start = page * CATALOG_ITEMS_PER_PAGE;
        for (int i = 0; i < CATALOG_ITEMS_PER_PAGE && start + i < catalog.size(); i++) {
            gui.setItem(i, catalogItem(catalog.get(start + i)));
        }

        // Navigation buttons (bottom row)
        if (page > 0) {
            gui.setItem(catalogPrev, navButton(Component.text("« Previous", NamedTextColor.YELLOW), "Go to previous page"));
        }
        gui.setItem(catalogIndicator, pageIndicator(page + 1, pages));
        gui.setItem(catalogBack, simpleButton(Material.BARRIER, Component.text("Back", NamedTextColor.RED), "Return to categories"));
        if (page < pages - 1) {
            gui.setItem(catalogNext, navButton(Component.text("Next »", NamedTextColor.YELLOW), "Go to next page"));
        }
    }

    private Component getCategoryTitle(ItemCategory category) {
        return Component.text(category.label(), NamedTextColor.LIGHT_PURPLE);
    }

    private void drawUpgrader(Player player, Inventory gui, GUIState state) {
        ItemStack filler = fillerItem();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, filler);
        }
        ItemStack border = borderItem();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(45 + i, border);
        }
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border);
            gui.setItem(row * 9 + 8, border);
        }

        gui.setItem(slotInfo, winStreakDisplay(state));
        gui.setItem(slotInput, state.inputItem != null ? wagerDisplay(state) : wagerPlaceholder());
        gui.setItem(slotArrow, arrowItem());
        gui.setItem(slotTarget, state.targetMaterial != null ? targetDisplay(state) : targetPlaceholder());

        // Draw presets from config
        for (UpgraderConfig.Preset preset : presets) {
            if (preset.slot() >= 0 && preset.slot() < 54) {
                gui.setItem(preset.slot(), presetButton(preset.label(), preset.icon(), preset.color(), preset.mode() == UpgraderConfig.Preset.Mode.MULTIPLIER
                        ? "Target worth about " + preset.value() + "× your wager"
                        : "Pick a target with a ~" + (int)(preset.value() * 100) + "% win chance"));
            }
        }

        // Count adjustment buttons
        gui.setItem(slotMinus32, simpleButton(Material.RED_CONCRETE, Component.text("−32", NamedTextColor.RED), "Remove 32 from target count"));
        gui.setItem(slotMinus16, simpleButton(Material.RED_CONCRETE, Component.text("−16", NamedTextColor.RED), "Remove 16 from target count"));
        gui.setItem(slotMinus1, simpleButton(Material.RED_CONCRETE, Component.text("−1", NamedTextColor.RED), "Remove one from target count"));
        gui.setItem(slotCount, countDisplay(state));
        gui.setItem(slotPlus1, simpleButton(Material.LIME_CONCRETE, Component.text("+1", NamedTextColor.GREEN), "Add one to target count"));
        gui.setItem(slotPlus16, simpleButton(Material.LIME_CONCRETE, Component.text("+16", NamedTextColor.GREEN), "Add 16 to target count"));
        gui.setItem(slotPlus32, simpleButton(Material.LIME_CONCRETE, Component.text("+32", NamedTextColor.GREEN), "Add 32 to target count"));

        // Stats button
        gui.setItem(slotStats, statsButton());

        gui.setItem(slotCatalog, catalogButton());
        gui.setItem(slotUpgrade, upgradeButton(player, state));
        gui.setItem(slotClose, simpleButton(Material.BARRIER, Component.text("Close", NamedTextColor.RED), "Close the upgrader"));
    }

    private void refresh(Player player, GUIState state) {
        Inventory top = topMatching(player, state);
        if (top != null) {
            drawUpgrader(player, top, state);
        }
    }

    // ------------------------------------------------------------- clicking

    /** Click inside the upgrader view (already cancelled by the listener). */
    public void handleUpgraderClick(Player player, GUIState state, int slot, ClickType click) {
        if (state.spinning) return;
        if (slot == slotInput) {
            if (click.isRightClick() && state.inputItem != null) {
                state.inputItem = null;
                state.inputValue = 0;
                refresh(player, state);
            }
        } else if (slot == slotTarget) {
            if (click.isRightClick() && state.targetMaterial != null) {
                state.targetMaterial = null;
                state.targetValue = 0;
                state.targetCount = 1;
                refresh(player, state);
            }
        } else if (slot == slotMinus32) {
            adjustCount(state, -32);
            refresh(player, state);
        } else if (slot == slotMinus16) {
            adjustCount(state, -16);
            refresh(player, state);
        } else if (slot == slotMinus1) {
            adjustCount(state, -1);
            refresh(player, state);
        } else if (slot == slotCount) {
            state.targetCount = 1;
            refresh(player, state);
        } else if (slot == slotPlus1) {
            adjustCount(state, 1);
            refresh(player, state);
        } else if (slot == slotPlus16) {
            adjustCount(state, 16);
            refresh(player, state);
        } else if (slot == slotPlus32) {
            adjustCount(state, 32);
            refresh(player, state);
        } else if (slot == slotStats) {
            // Open the stats GUI
            plugin.getStatsGUI().openMainMenu(player);
        } else if (slot == slotCatalog) {
            state.viewingCategories = true;
            openCatalog(player, state);
        } else if (slot == slotUpgrade) {
            handleUpgrade(player, state);
        } else if (slot == slotClose) {
            player.closeInventory();
        } else {
            // Check if slot is a preset slot
            for (UpgraderConfig.Preset preset : presets) {
                if (preset.slot() == slot) {
                    handlePreset(player, state, preset);
                    refresh(player, state);
                    return;
                }
            }
        }
    }

    /** Click in the player's own inventory while the upgrader view is open. */
    public void handleWagerClick(Player player, GUIState state, ItemStack clicked, ClickType click) {
        if (state.spinning) return;
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (itemValues.isBlacklisted(clicked.getType())) {
            player.sendMessage(Component.text("That item is blacklisted and can't be used!", NamedTextColor.RED));
            return;
        }
        if (click.isRightClick()) {
            state.targetMaterial = clicked.getType();
            state.targetValue = itemValues.unitValue(clicked.getType());
            state.targetCount = 1;
        } else {
            state.inputItem = clicked.clone();
            state.inputValue = itemValues.stackValue(clicked);
        }
        refresh(player, state);
    }

    /** Item dragged and dropped into the input slot (wager). */
    public void handleWagerDrop(Player player, GUIState state, ItemStack dropped) {
        if (state.spinning) return;
        if (dropped == null || dropped.getType() == Material.AIR) return;
        if (itemValues.isBlacklisted(dropped.getType())) {
            player.sendMessage(Component.text("That item is blacklisted and can't be used!", NamedTextColor.RED));
            return;
        }

        // If there's already a wager
        if (state.inputItem != null) {
            // If same material, add to stack
            if (state.inputItem.getType() == dropped.getType()) {
                int maxStack = dropped.getMaxStackSize();
                int newAmount = Math.min(maxStack, state.inputItem.getAmount() + dropped.getAmount());
                state.inputItem.setAmount(newAmount);
                state.inputValue = itemValues.stackValue(state.inputItem);
                refresh(player, state);
                return;
            }
            // Different material: return old wager to inventory
            ItemStack oldWager = state.inputItem.clone();
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(oldWager);
            for (ItemStack extra : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }
        }

        // Set new wager
        state.inputItem = dropped.clone();
        state.inputValue = itemValues.stackValue(dropped);
        refresh(player, state);
    }

    /** Item dragged and dropped into the target slot. */
    public void handleTargetDrop(Player player, GUIState state, ItemStack dropped) {
        if (state.spinning) return;
        if (dropped == null || dropped.getType() == Material.AIR) return;
        if (itemValues.isBlacklisted(dropped.getType())) {
            player.sendMessage(Component.text("That item is blacklisted and can't be used!", NamedTextColor.RED));
            return;
        }
        state.targetMaterial = dropped.getType();
        state.targetValue = itemValues.unitValue(dropped.getType());
        // Set target count to the dropped amount, but respect stack limits
        int maxStack = Math.min(plugin.getUpgraderConfig().maxTargetCount, dropped.getType().getMaxStackSize());
        state.targetCount = Math.min(maxStack, dropped.getAmount());
        // Ensure at least 1
        if (state.targetCount < 1) {
            state.targetCount = 1;
        }
        refresh(player, state);
    }

    /** Click inside the catalog view. */
    public void handleCatalogClick(Player player, GUIState state, int slot, ItemStack clicked) {
        if (state.viewingCategories) {
            // Category selection screen - handle category clicks
            if (slot == 45) {
                // Back to upgrader button
                openUpgraderView(player, state);
                return;
            }
            int[] catSlots = categorySlots;
            List<ItemCategory> categories = plugin.getUpgraderConfig().getCategories();
            for (int i = 0; i < Math.min(catSlots.length, categories.size()); i++) {
                if (slot == catSlots[i]) {
                    state.viewingCategories = false;
                    state.catalogCategory = categories.get(i);
                    state.catalogPage = 0;
                    openCatalog(player, state);
                    return;
                }
            }
        } else {
            // Item grid screen
            if (slot == catalogPrev) {
                state.catalogPage = Math.max(0, state.catalogPage - 1);
                openCatalog(player, state);
            } else if (slot == catalogNext) {
                state.catalogPage++;
                openCatalog(player, state);
            } else if (slot == catalogBack) {
                state.viewingCategories = true;
                openCatalog(player, state);
            } else {
                // Handle item clicks - items fill slots 0-44 (above the nav row)
                if (isCatalogItemSlot(slot)) {
                    int index = getCatalogItemIndex(slot);
                    int globalIndex = state.catalogPage * CATALOG_ITEMS_PER_PAGE + index;
                    List<Material> catalog = itemValues.catalog(state.catalogCategory);
                    if (globalIndex < catalog.size() && clicked != null && clicked.getType() != Material.AIR) {
                        state.targetMaterial = clicked.getType();
                        state.targetValue = itemValues.unitValue(clicked.getType());
                        state.targetCount = 1;
                        openUpgraderView(player, state);
                    }
                }
            }
        }
    }

    /** Check if a slot is a valid item slot in the catalog grid (slots 0-44, above the nav row). */
    private boolean isCatalogItemSlot(int slot) {
        return slot >= 0 && slot < CATALOG_ITEMS_PER_PAGE;
    }

    /** Get the 0-based index of an item slot in the catalog grid. */
    private int getCatalogItemIndex(int slot) {
        return slot;
    }

    /** Called when the upgrader view closes; resolves a pending spin immediately. */
    public void handleClose(Player player, GUIState state) {
        if (state == null) return;

        // Don't return items if we're just switching views (upgrader <-> catalog)
        if (state.isSwitchingViews) {
            return;
        }

        if (state.spinning) {
            if (state.spinTask != null) {
                state.spinTask.cancel();
            }
            resolve(player, state);
        } else {
            // Return wager item to player if not spinning
            if (state.inputItem != null) {
                ItemStack wager = state.inputItem.clone();
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(wager);
                for (ItemStack extra : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), extra);
                }
                state.inputItem = null;
                state.inputValue = 0;
            }
            // Do NOT return target item - it's just a selection, not a wager
            state.targetMaterial = null;
            state.targetValue = 0;
            state.targetCount = 0;
        }
    }

    /** Fallback for disconnects; the close event normally covers this. */
    public void handleQuit(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && top.getHolder() instanceof UpgraderHolder holder && holder.state.spinning) {
            handleClose(player, holder.state);
        }
    }

    // -------------------------------------------------------------- upgrade

    private void handleUpgrade(Player player, GUIState state) {
        if (state.spinning) return;
        if (state.inputItem == null) {
            player.sendMessage(Component.text("Place a wager first — left-click an item in your inventory!", NamedTextColor.RED));
            return;
        }
        if (state.targetMaterial == null) {
            player.sendMessage(Component.text("Choose a target — right-click an item, use a preset, or open the catalog!", NamedTextColor.RED));
            return;
        }
        int cooldown = attemptLimiter.cooldownRemaining(player);
        if (cooldown > 0) {
            player.sendMessage(Component.text("You're on cooldown — wait " + cooldown + "s.", NamedTextColor.RED));
            refresh(player, state);
            return;
        }

        // The wager is already stored in state.inputItem (taken on drop)
        // No need to check/remove from player inventory
        ItemStack wager = state.inputItem;

        double chance = upgradeOdds.chance(wager, state.targetMaterial, state.targetCount);

        attemptLimiter.record(player);
        state.pendingWin = ThreadLocalRandom.current().nextDouble() < chance;
        state.spinChance = chance;
        state.spinning = true;
        startSpin(player, state);
    }

    private void startSpin(Player player, GUIState state) {
        // Crate animation: balanced speed for addictive feel
        int totalSteps = 40; // Balanced - not too fast, not too slow

        // Pre-generate the sequence of items that will appear in the animation
        // The last item will be the actual result (target item on win, or a "lose" item)
        Material[] animationSequence = generateCrateSequence(state);

        Inventory gui = topMatching(player, state);
        if (gui != null) {
            gui.setItem(slotUpgrade, upgradeButton(player, state));
            // Initialize the spin slots with first items
            for (int i = 0; i < spinSlots.length; i++) {
                gui.setItem(spinSlots[i], createSpinDisplayItem(animationSequence[i % animationSequence.length]));
            }
        }

        state.spinTask = new BukkitRunnable() {
            int step = 0;
            int currentIndex = 0;

            @Override
            public void run() {
                if (!state.spinning || !player.isOnline()) {
                    cancel();
                    return;
                }
                Inventory top = topMatching(player, state);
                if (top != null) {
                    // Crate-style: shift all items left, new item appears on the right
                    for (int i = 0; i < spinSlots.length - 1; i++) {
                        ItemStack nextItem = top.getItem(spinSlots[i + 1]);
                        if (nextItem != null) {
                            top.setItem(spinSlots[i], nextItem.clone());
                        }
                    }
                    // Add new item at the end
                    int seqIndex = (currentIndex + spinSlots.length - 1) % animationSequence.length;
                    top.setItem(spinSlots[spinSlots.length - 1],
                        createSpinDisplayItem(animationSequence[seqIndex]));
                }

                // Play sound - pitch increases as we approach the end (slowing down feel)
                float pitch = 0.8f + (float)step / totalSteps * 0.6f;
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_BLOCK_PLING", 0.5f, pitch);

                // Also play a subtle "tick" sound
                if (step % 2 == 0) {
                    VersionUtil.playSoundSafe(player.getLocation(), "UI_BUTTON_CLICK", "BLOCK_NOTE_BLOCK_PLING", 0.3f, 1.0f);
                }

                currentIndex = (currentIndex + 1) % animationSequence.length;
                step++;

                // Slow down towards the end - increase delay
                // We'll handle this by cancelling and rescheduling with longer delay
                // For simplicity, use fixed stepTicks but the visual effect of more steps creates the slowdown
                if (step >= totalSteps) {
                    cancel();
                    // Set the final result in the center slot
                    Inventory finalTop = topMatching(player, state);
                    if (finalTop != null) {
                        Material finalMaterial = state.pendingWin ? state.targetMaterial : Material.RED_STAINED_GLASS_PANE;
                        ItemStack finalItem = new ItemStack(finalMaterial);
                        ItemMeta meta = finalItem.getItemMeta();
                        if (meta != null) {
                            meta.displayName(state.pendingWin
                                ? Component.text("WIN!", NamedTextColor.GREEN, TextDecoration.BOLD)
                                : Component.text("LOST", NamedTextColor.RED, TextDecoration.BOLD));
                            meta.lore(Collections.singletonList(
                                state.pendingWin
                                    ? Component.text("You won " + state.targetCount + "x " + pretty(state.targetMaterial), NamedTextColor.GRAY)
                                    : Component.text("Better luck next time!", NamedTextColor.GRAY)));
                            finalItem.setItemMeta(meta);
                        }
                        finalTop.setItem(spinSlots[spinSlots.length / 2], finalItem); // Center slot
                    }
                    resolve(player, state);
                }
            }
        }.runTaskTimer(plugin, 2L, 3L); // Balanced speed for addictive feel
    }

    private void resolve(Player player, GUIState state) {
        if (!state.spinning) return;
        state.spinning = false;

        boolean win = state.pendingWin;
        double inputValue = state.inputValue;
        double targetValue = state.targetValue;
        int targetCount = state.targetCount;
        Material wagerMaterial = state.inputItem != null ? state.inputItem.getType() : Material.AIR;

        // Record wager in stats
        StatsManager statsManager = plugin.getStatsManager();
        PlayerStats stats = statsManager.getStats(player);
        stats.recordWager(wagerMaterial, inputValue);

        // Near-miss feedback: when chance is very low (1-5%) but player loses
        double chance = state.spinChance;
        boolean nearMiss = !win && chance > 0.01 && chance <= 0.05;

        if (win && state.targetMaterial != null) {
            int count = targetCount;
            giveItems(player, new ItemStack(state.targetMaterial, count));
            double valueGained = (targetValue * count) - inputValue;
            player.sendMessage(Component.text("✔ Upgrade successful! You won ", NamedTextColor.GREEN)
                    .append(Component.text(count + " × " + pretty(state.targetMaterial), NamedTextColor.YELLOW))
                    .append(Component.text("!", NamedTextColor.GREEN)));

            // Record win in stats
            stats.recordWin(state.targetMaterial, count, targetValue, chance);

            // Check for new achievements
            checkAchievements(player, stats);

            // Enhanced sound effects based on value gained
            if (valueGained >= 1000) {
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_ENDER_DRAGON_DEATH", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.2f);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", 1.5f, 1.5f);
            } else if (valueGained >= 500) {
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_WITHER_DEATH", "ENTITY_WITHER_SPAWN", 1.0f, 1.0f);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", 1.2f, 1.2f);
            } else if (valueGained >= 100) {
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_PLAYER_LEVELUP", 1.5f, 1.5f);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.2f);
            } else {
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", 0.8f, 1.4f);
            }

            // Particle effects for big wins - simplified for compatibility
            if (valueGained >= 200) {
                VersionUtil.spawnParticleSafe(player.getEyeLocation(), "FLAME", "SMOKE", 5, 0.5, 0.5, 0.5, 0.2);
            }

            // Fireworks for big wins!
            if (valueGained >= 500) {
                // Epic fireworks display
                VersionUtil.spawnParticleSafe(player.getLocation().add(0, 1, 0), "EXPLOSION", "EXPLOSION", 50, 1.5, 1, 1.5, 0.2);
                VersionUtil.spawnParticleSafe(player.getLocation().add(0, 2, 0), "EXPLOSION", "EXPLOSION", 10, 1, 1, 1, 0);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_FIREWORK_ROCKET_LAUNCH", "ENTITY_PLAYER_LEVELUP", 1.5f, 1.2f);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_FIREWORK_ROCKET_BLAST", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.5f);
            } else if (valueGained >= 200) {
                // Nice fireworks
                VersionUtil.spawnParticleSafe(player.getLocation().add(0, 1, 0), "EXPLOSION", "EXPLOSION", 20, 1, 0.5, 1, 0.1);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_FIREWORK_ROCKET_LAUNCH", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_FIREWORK_ROCKET_BLAST", "ENTITY_PLAYER_LEVELUP", 0.8f, 1.3f);
            }

            // Global chat announcement for unlikely wins (low odds)
            if (chance < 0.10) { // Less than 10% chance - JACKPOT!
                Bukkit.broadcast(Component.text("✦ ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                        .append(Component.text("JACKPOT! ", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text("✦", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));
                Bukkit.broadcast(Component.text(player.getName(), NamedTextColor.YELLOW)
                        .append(Component.text(" just beat ", NamedTextColor.WHITE))
                        .append(Component.text(String.format("%.1f%%", chance * 100), NamedTextColor.RED))
                        .append(Component.text(" odds and won ", NamedTextColor.WHITE))
                        .append(Component.text(count + "x " + pretty(state.targetMaterial), NamedTextColor.GOLD))
                        .append(Component.text("!", NamedTextColor.WHITE)));
                Bukkit.broadcast(Component.text("Wagered: ", NamedTextColor.GRAY)
                        .append(Component.text(pretty(state.inputItem != null ? state.inputItem.getType() : Material.AIR), NamedTextColor.GRAY))
                        .append(Component.text(" | Value gained: ", NamedTextColor.GRAY))
                        .append(Component.text(formatValue(valueGained), NamedTextColor.GREEN)));
                // Epic server-wide fireworks at winner's location
                for (int i = 0; i < 5; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            VersionUtil.spawnParticleSafe(player.getLocation().add(0, 2, 0), "EXPLOSION", "EXPLOSION", 30, 2, 1.5, 2, 0.3);
                            VersionUtil.spawnParticleSafe(player.getLocation().add(0, 3, 0), "EXPLOSION", "EXPLOSION", 3, 0, 0, 0, 0);
                        }
                    }, i * 10L);
                }
            } else if (chance < 0.25) { // Less than 25% chance - BIG WIN!
                Bukkit.broadcast(Component.text("★ BIG WIN! ★ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" won ", NamedTextColor.WHITE))
                        .append(Component.text(count + "x " + pretty(state.targetMaterial), NamedTextColor.GOLD))
                        .append(Component.text(" at ", NamedTextColor.WHITE))
                        .append(Component.text(String.format("%.1f%%", chance * 100), NamedTextColor.RED))
                        .append(Component.text(" chance!", NamedTextColor.WHITE)));
            } else if (chance < 0.50) { // Less than 50% chance - Nice win
                // Only announce to nearby players (within 50 blocks)
                for (Player nearby : player.getWorld().getPlayers()) {
                    if (nearby.getLocation().distance(player.getLocation()) <= 50) {
                        nearby.sendMessage(Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.ITALIC)
                                .append(Component.text(" just won ", NamedTextColor.AQUA))
                                .append(Component.text(count + "x " + pretty(state.targetMaterial), NamedTextColor.YELLOW))
                                .append(Component.text(" (" + String.format("%.0f%%", chance * 100) + " chance)", NamedTextColor.AQUA)));
                    }
                }
            }

            // Increment win streak
            state.winStreak++;
        } else {
            String wagered = state.inputItem != null ? pretty(state.inputItem.getType()) : "item";
            double valueLost = inputValue;
            player.sendMessage(Component.text("✘ The upgrade failed — your ", NamedTextColor.RED)
                    .append(Component.text(wagered, NamedTextColor.RED))
                    .append(Component.text(" was destroyed.", NamedTextColor.RED)));

            // Record loss in stats
            stats.recordLoss(wagerMaterial, valueLost);

            // Near-miss feedback: so close to winning!
            if (nearMiss) {
                player.sendMessage(Component.text("⚠ SO CLOSE! ⚠ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("Only " + String.format("%.1f%%", chance * 100) + " away from victory!", NamedTextColor.YELLOW)));
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_PLAYER_LEVELUP", 1.0f, 0.5f);
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_NOTE_BLOCK_BELL", "BLOCK_NOTE_BLOCK_PLING", 0.8f, 0.6f);
                VersionUtil.spawnParticleSafe(player.getEyeLocation(), "END_ROD", "ENCHANTMENT_TABLE", 20, 0.8, 0.8, 0.8, 0.3);
            }

            // Enhanced sound effects based on value lost
            if (valueLost >= 1000) {
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_WITHER_SPAWN", "ENTITY_WITHER_SPAWN", 1.5f, 0.8f);
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_ANVIL_BREAK", "BLOCK_ANVIL_LAND", 2.0f, 0.6f);
            } else if (valueLost >= 500) {
                VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_ELDER_GUARDIAN_DEATH", "ENTITY_WITHER_SPAWN", 1.2f, 0.8f);
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_ANVIL_LAND", "BLOCK_ANVIL_LAND", 1.5f, 0.7f);
            } else if (valueLost >= 100) {
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_ANVIL_HIT", "BLOCK_ANVIL_LAND", 1.0f, 0.8f);
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_ANVIL_USE", "BLOCK_ANVIL_LAND", 0.8f, 0.9f);
            } else {
                VersionUtil.playSoundSafe(player.getLocation(), "BLOCK_ANVIL_LAND", "BLOCK_ANVIL_LAND", 0.7f, 0.8f);
            }

            // Add blindness effect for 3 seconds on loss (as requested)
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 2)); // 3 seconds = 60 ticks

            // Particle effects for losses - simplified for compatibility
            VersionUtil.spawnParticleSafe(player.getEyeLocation(), "FLAME", "SMOKE", 8, 0.4, 0.4, 0.4, 0.1);

            // Reset win streak on loss
            state.winStreak = 0;
        }

        state.inputItem = null;
        state.inputValue = 0;

        // If the GUI is still open, flash the outcome on the arrow slot.
        // Longer flash for higher win streaks (up to 3 seconds max)
        Inventory top = topMatching(player, state);
        if (top != null) {
            top.setItem(slotArrow, win
                    ? flashPane(Material.LIME_STAINED_GLASS_PANE, Component.text("WIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                    : flashPane(Material.RED_STAINED_GLASS_PANE, Component.text("LOST", NamedTextColor.RED, TextDecoration.BOLD)));
            top.setItem(slotInput, wagerPlaceholder());
            top.setItem(slotUpgrade, upgradeButton(player, state));
            int flashDuration = Math.min(45 + (state.winStreak * 5), 180); // 45 ticks base + 5 per win streak, max 180 ticks (9 seconds)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Inventory again = topMatching(player, state);
                if (again != null) {
                    drawUpgrader(player, again, state);
                }
            }, flashDuration);
        }
    }

    private void giveItems(Player player, ItemStack reward) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
        for (ItemStack extra : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    /**
     * Represents a candidate material with its optimal count and value difference from the target.
     */
    private record MaterialCandidate(Material material, double unitValue, int count, double diff) {
        double totalValue() { return unitValue * count; }
    }

    /**
     * Find all valid materials sorted by their total value difference from the wanted value.
     * Returns a list with the closest match first.
     */
    private List<MaterialCandidate> findClosestItems(double wanted, ItemCategory category) {
        if (wanted <= 0) return Collections.emptyList();

        int maxTargetCount = plugin.getUpgraderConfig().maxTargetCount;
        List<MaterialCandidate> candidates = new ArrayList<>();

        for (Material material : itemValues.catalog(category)) {
            double unitVal = itemValues.unitValue(material);
            if (unitVal <= 0) continue;

            // Calculate optimal count for this material to reach wanted total value
            int maxStack = Math.min(maxTargetCount, material.getMaxStackSize());
            int optimalCount = (int) Math.round(wanted / unitVal);
            optimalCount = Math.max(1, Math.min(maxStack, optimalCount));

            double totalVal = unitVal * optimalCount;
            double diff = Math.abs(totalVal - wanted);

            candidates.add(new MaterialCandidate(material, unitVal, optimalCount, diff));
        }

        // Sort by difference (closest first)
        candidates.sort(Comparator.comparingDouble(MaterialCandidate::diff));
        return candidates;
    }

    private void handlePreset(Player player, GUIState state, UpgraderConfig.Preset preset) {
        if (state.inputValue <= 0) {
            player.sendMessage(Component.text("Place a wager first!", NamedTextColor.RED));
            return;
        }
        double edge = plugin.getUpgraderConfig().houseEdge;
        double wanted;
        if (preset.mode() == UpgraderConfig.Preset.Mode.MULTIPLIER) {
            wanted = state.inputValue * preset.value();
        } else {
            // CHANCE mode: calculate target value for the desired chance
            wanted = state.inputValue * edge / preset.value();
        }

        // Get all candidates sorted by distance from wanted value
        List<MaterialCandidate> candidates = findClosestItems(wanted, state.catalogCategory);
        if (candidates.isEmpty()) {
            player.sendMessage(Component.text("No valid items found!", NamedTextColor.RED));
            return;
        }

        // Get current cycle index for this preset slot
        int cycleIndex = state.getPresetCycleIndex(preset.slot());
        int candidateIndex = cycleIndex % candidates.size();

        // Select the candidate at the cycle index
        MaterialCandidate selected = candidates.get(candidateIndex);
        state.targetMaterial = selected.material();
        state.targetValue = selected.unitValue();
        state.targetCount = selected.count();

        // Increment cycle index for next click
        state.incrementPresetCycleIndex(preset.slot());

        refresh(player, state);

        // Notify player which alternative they're on (optional feedback)
        if (candidates.size() > 1) {
            player.sendActionBar(Component.text("Target: " + pretty(selected.material()) + " (alt " + (candidateIndex + 1) + "/" + candidates.size() + ")", NamedTextColor.YELLOW));
        }
    }

    private void findClosestItem(Player player, GUIState state, double wanted) {
        if (wanted <= 0) return;
        Material best = null;
        int bestCount = 1;
        double bestDiff = Double.MAX_VALUE;

        int maxTargetCount = plugin.getUpgraderConfig().maxTargetCount;

        for (Material material : itemValues.catalog(state.catalogCategory)) {
            double unitVal = itemValues.unitValue(material);
            if (unitVal <= 0) continue;

            // Calculate optimal count for this material to reach wanted total value
            int maxStack = Math.min(maxTargetCount, material.getMaxStackSize());
            int optimalCount = (int) Math.round(wanted / unitVal);
            optimalCount = Math.max(1, Math.min(maxStack, optimalCount));

            double totalVal = unitVal * optimalCount;
            double diff = Math.abs(totalVal - wanted);

            if (diff < bestDiff) {
                bestDiff = diff;
                best = material;
                bestCount = optimalCount;
            }
        }

        if (best != null) {
            state.targetMaterial = best;
            state.targetValue = itemValues.unitValue(best);
            state.targetCount = bestCount;
            refresh(player, state);
        }
    }

    private void adjustCount(GUIState state, int delta) {
        if (state.targetMaterial == null) return;
        int max = Math.min(plugin.getUpgraderConfig().maxTargetCount, state.targetMaterial.getMaxStackSize());
        state.targetCount = Math.max(1, Math.min(max, state.targetCount + delta));
    }

    // ---------------------------------------------------------------- items

    /** Display the current win streak. */
    private ItemStack winStreakDisplay(GUIState state) {
        ItemStack stack;
        ItemMeta meta;

        if (state.winStreak > 0) {
            // Hot streak - use fire/blaze materials
            if (state.winStreak >= 10) {
                stack = new ItemStack(Material.NETHER_STAR);
            } else if (state.winStreak >= 5) {
                stack = new ItemStack(Material.BLAZE_ROD);
            } else if (state.winStreak >= 3) {
                stack = new ItemStack(Material.MAGMA_CREAM);
            } else {
                stack = new ItemStack(Material.FIRE_CHARGE);
            }
        } else {
            // No streak - use a neutral item
            stack = new ItemStack(Material.CLOCK);
        }

        meta = stack.getItemMeta();
        if (meta != null) {
            String streakText;
            NamedTextColor streakColor;

            if (state.winStreak == 0) {
                streakText = "No streak";
                streakColor = NamedTextColor.GRAY;
            } else if (state.winStreak < 3) {
                streakText = "Warming up...";
                streakColor = NamedTextColor.YELLOW;
            } else if (state.winStreak < 5) {
                streakText = "Hot streak!";
                streakColor = NamedTextColor.GOLD;
            } else if (state.winStreak < 10) {
                streakText = "ON FIRE!";
                streakColor = NamedTextColor.RED;
            } else {
                streakText = "LEGENDARY!";
                streakColor = NamedTextColor.LIGHT_PURPLE;
            }

            meta.displayName(Component.text("WIN STREAK: ", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true)
                    .append(Component.text(String.valueOf(state.winStreak), streakColor)));
            meta.lore(List.of(
                    Component.text(streakText, NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Consecutive wins increase", NamedTextColor.GRAY),
                    Component.text("the WIN flash duration!", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Right-click for help", NamedTextColor.DARK_GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack wagerPlaceholder() {
        ItemStack stack = new ItemStack(Material.HOPPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("WAGER", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("Left-click an item in your", NamedTextColor.GRAY),
                    Component.text("inventory to risk it.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Right-click here to clear", NamedTextColor.DARK_GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack targetPlaceholder() {
        ItemStack stack = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("TARGET", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("Right-click an item in your inventory,", NamedTextColor.GRAY),
                    Component.text("use a preset, or browse the catalog.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Right-click here to clear", NamedTextColor.DARK_GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack arrowItem() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack wagerDisplay(GUIState state) {
        ItemStack stack = state.inputItem.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Value: ", NamedTextColor.GRAY)
                    .append(Component.text(formatValue(state.inputValue), NamedTextColor.GOLD)));
            lore.add(Component.text("Right-click here to clear", NamedTextColor.DARK_GRAY));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack targetDisplay(GUIState state) {
        int amount = Math.max(1, Math.min(state.targetCount, state.targetMaterial.getMaxStackSize()));
        ItemStack stack = new ItemStack(state.targetMaterial, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(pretty(state.targetMaterial), NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("Each: ", NamedTextColor.GRAY)
                            .append(Component.text(formatValue(state.targetValue), NamedTextColor.GOLD)),
                    Component.text("Count: ", NamedTextColor.GRAY)
                            .append(Component.text(String.valueOf(state.targetCount), NamedTextColor.WHITE)),
                    Component.text("Total: ", NamedTextColor.GRAY)
                            .append(Component.text(formatValue(state.targetValue * state.targetCount), NamedTextColor.GOLD)),
                    Component.empty(),
                    Component.text("Right-click here to clear", NamedTextColor.DARK_GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack countDisplay(GUIState state) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            int max = state.targetMaterial != null
                    ? Math.min(plugin.getUpgraderConfig().maxTargetCount, state.targetMaterial.getMaxStackSize())
                    : plugin.getUpgraderConfig().maxTargetCount;
            meta.displayName(Component.text("Count: ", NamedTextColor.GOLD)
                    .append(Component.text(String.valueOf(state.targetCount), NamedTextColor.WHITE)));
            meta.lore(List.of(
                    Component.text("Number of target items to win", NamedTextColor.GRAY),
                    Component.text("Max: " + max, NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("Click to reset to 1", NamedTextColor.YELLOW)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack presetButton(String label, Material icon, Component color, String desc) {
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(color.append(Component.text(label)).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text(desc, NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to pick target", NamedTextColor.YELLOW)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack catalogButton() {
        ItemStack stack = new ItemStack(Material.CHEST);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("CATALOG", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("Browse items by category", NamedTextColor.GRAY),
                    Component.text("Click to open the item catalog", NamedTextColor.GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack statsButton() {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("STATISTICS", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("View your stats, leaderboards,", NamedTextColor.GRAY),
                    Component.text("and achievements", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to open", NamedTextColor.YELLOW)));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LOYALTY, 1, false);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack upgradeButton(Player player, GUIState state) {
        ItemStack stack;
        Component name;
        List<Component> lore = new ArrayList<>();
        if (state.spinning) {
            stack = new ItemStack(Material.ORANGE_CONCRETE);
            name = Component.text("SPINNING...", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
            lore.add(Component.text("Rolling the dice...", NamedTextColor.GRAY));
        } else {
            int cooldown = attemptLimiter.cooldownRemaining(player);
            if (cooldown > 0) {
                stack = new ItemStack(Material.REDSTONE_BLOCK);
                name = Component.text("COOLDOWN", NamedTextColor.RED).decoration(TextDecoration.BOLD, true);
                lore.add(Component.text("Try again in " + cooldown + "s", NamedTextColor.GRAY));
            } else if (state.inputItem == null || state.targetMaterial == null) {
                stack = new ItemStack(Material.EMERALD_BLOCK);
                name = Component.text("UPGRADE", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true);
                lore.add(Component.text("Set a wager and a target first", NamedTextColor.GRAY));
            } else {
                stack = new ItemStack(Material.EMERALD_BLOCK);
                name = Component.text("UPGRADE", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
                double chance = upgradeOdds.chance(state.inputValue, state.targetValue * state.targetCount);
                lore.add(Component.text("Win chance: ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%.1f%%", chance * 100), NamedTextColor.YELLOW)));
                lore.add(Component.text("Wager value: ", NamedTextColor.GRAY)
                        .append(Component.text(formatValue(state.inputValue), NamedTextColor.GOLD)));
                lore.add(Component.text("Target total: ", NamedTextColor.GRAY)
                        .append(Component.text(formatValue(state.targetValue * state.targetCount), NamedTextColor.GOLD)));
                lore.add(Component.empty());
                lore.add(Component.text("Click to roll!", NamedTextColor.YELLOW));
            }
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Generate the sequence of materials for the crate animation. */
    private Material[] generateCrateSequence(GUIState state) {
        List<Material> sequence = new ArrayList<>();

        // Add a mix of items from the crate cycle
        for (Material m : CRATE_CYCLE) {
            sequence.add(m);
        }

        // Add the target material multiple times if winning (so it's more likely to land on it visually)
        if (state.pendingWin && state.targetMaterial != null) {
            for (int i = 0; i < 3; i++) {
                sequence.add(state.targetMaterial);
            }
        }

        // Add some "near miss" items (similar value items) for tension
        if (state.targetMaterial != null) {
            double targetVal = itemValues.unitValue(state.targetMaterial);
            for (Material m : itemValues.catalog(allCategory)) {
                double val = itemValues.unitValue(m);
                if (val > targetVal * 0.5 && val < targetVal * 2.0 && m != state.targetMaterial) {
                    sequence.add(m);
                }
            }
        }

        return sequence.toArray(new Material[0]);
    }

    /** Create a display item for the spin animation. */
    private ItemStack createSpinDisplayItem(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("SPINNING", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(Component.text(pretty(material) + " ")
                    .append(Component.text(formatValue(itemValues.unitValue(material)), NamedTextColor.GOLD))));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack flashPane(Material material, Component name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack catalogItem(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(pretty(material), NamedTextColor.WHITE));
            meta.lore(List.of(
                    Component.text("Value: ", NamedTextColor.GRAY)
                            .append(Component.text(formatValue(itemValues.unitValue(material)), NamedTextColor.GOLD)),
                    Component.empty(),
                    Component.text("Click to set as target", NamedTextColor.YELLOW)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack categoryTab(ItemCategory category, boolean isSelected) {
        ItemStack stack;
        if (isSelected) {
            // Selected tab: enchanted with glow effect
            stack = new ItemStack(category.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(category.label(), NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LOYALTY, 1, false);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.lore(Arrays.asList(
                        Component.text("Currently selected", NamedTextColor.GRAY),
                        Component.text("Items: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(itemValues.catalog(category).size()), NamedTextColor.WHITE))));
                stack.setItemMeta(meta);
            }
        } else {
            // Unselected tab: plain item
            stack = new ItemStack(category.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(category.label(), NamedTextColor.WHITE));
                meta.lore(Arrays.asList(
                        Component.text("Items: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(itemValues.catalog(category).size()), NamedTextColor.WHITE))));
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    private ItemStack navButton(Component name, String desc) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(List.of(Component.text(desc, NamedTextColor.GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack pageIndicator(int page, int pages) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text()
                    .append(Component.text("Page ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(page), NamedTextColor.WHITE))
                    .append(Component.text(" / ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(pages), NamedTextColor.WHITE))
                    .build());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack simpleButton(Material icon, Component name, String desc) {
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(List.of(Component.text(desc, NamedTextColor.GRAY)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack borderItem() {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack fillerItem() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ------------------------------------------------------------- helpers

    private Inventory topMatching(Player player, GUIState state) {
        if (!player.isOnline()) return null;
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && top.getHolder() instanceof UpgraderHolder holder
                && holder.mode == UpgraderHolder.Mode.UPGRADER && holder.state == state) {
            return top;
        }
        return null;
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

    private String formatValue(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000);
        return String.format("%.1f", value);
    }

    /**
     * Check for newly earned achievements and notify the player.
     */
    private void checkAchievements(Player player, PlayerStats stats) {
        // Get already unlocked achievements from persistent storage
        Set<String> unlocked = new java.util.HashSet<>();
        Map<String, Object> progress = stats.getAchievementProgress();
        for (String key : progress.keySet()) {
            if (Boolean.TRUE.equals(progress.get(key))) {
                unlocked.add(key);
            }
        }

        List<AchievementManager.Achievement> newAchievements = AchievementManager.checkNewAchievements(stats, unlocked);
        for (AchievementManager.Achievement ach : newAchievements) {
            // Mark as unlocked in persistent storage
            stats.updateAchievementProgress(ach.id(), true);
            plugin.getStatsManager().saveStats(player.getUniqueId());

            // Notify player
            NamedTextColor rarityColor = ach.rarity().color;
            player.sendMessage(Component.text("✦ ACHIEVEMENT UNLOCKED! ✦", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text()
                    .append(Component.text(ach.name(), rarityColor).decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" — ", NamedTextColor.WHITE))
                    .append(Component.text(ach.description(), NamedTextColor.GRAY))
                    .build());
            player.sendMessage(Component.text("Rarity: ", NamedTextColor.YELLOW)
                    .append(Component.text(ach.rarity().label, rarityColor)));

            // Celebration effects
            VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", 1.5f, 1.5f);
            VersionUtil.playSoundSafe(player.getLocation(), "ENTITY_FIREWORK_ROCKET_BLAST", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.8f);
            VersionUtil.spawnParticleSafe(player.getLocation().add(0, 1, 0), "ENCHANTMENT_TABLE", "ENCHANTMENT_TABLE", 30, 1, 1, 1, 0.2);
            VersionUtil.spawnParticleSafe(player.getLocation().add(0, 2, 0), "END_ROD", "END_ROD", 20, 1, 1, 1, 0.3);
        }
    }
}
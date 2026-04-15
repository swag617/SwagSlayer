package com.swag.swagslayer.listeners;

import com.swag.swagslayer.managers.GUIManager;
import com.swag.swagslayer.managers.GUIManager.GUIType;
import com.swag.swagslayer.managers.SlayerManager;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Routes inventory clicks and close events for all SwagSlayer GUI screens.
 *
 * Click routing summary:
 *   MAIN_MENU
 *     Slots 10/12/14/16  → openTypeMenu for the corresponding SlayerType
 *     Slot 49            → openLeaderboard(player, null)  [overall]
 *
 *   TYPE_DETAIL
 *     Slot 31            → openLeaderboardWithBack(player, type, type)
 *     Slot 48            → openMainMenu(player)
 *
 *   LEADERBOARD
 *     Slot 49            → back logic:
 *                            null backDest  → openMainMenu(player)
 *                            non-null       → openTypeMenu(player, backDest)
 *
 * Only clicks on the TOP inventory are processed; clicks in the player's own
 * inventory section of the GUI are ignored.
 */
public class GUIListener implements Listener {

    private final GUIManager guiManager;
    private final SlayerManager slayerManager;

    public GUIListener(GUIManager guiManager, SlayerManager slayerManager) {
        this.guiManager = guiManager;
        this.slayerManager = slayerManager;
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        // We require a Player clicker.
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();

        // Fast exit: not one of our GUIs.
        if (!guiManager.isSwagSlayerGUI(topInv)) return;

        // Cancel all interaction within our GUIs — players cannot take items out.
        event.setCancelled(true);

        // Only process clicks that land on the top inventory.
        if (event.getClickedInventory() != topInv) return;

        int slot = event.getSlot();
        GUIType guiType = guiManager.getGUIType(topInv);
        if (guiType == null) return;

        switch (guiType) {
            case MAIN_MENU    -> handleMainMenuClick(player, topInv, slot);
            case TYPE_DETAIL  -> handleTypeDetailClick(player, topInv, slot);
            case LEADERBOARD  -> handleLeaderboardClick(player, topInv, slot);
        }
    }

    // -------------------------------------------------------------------------
    // Close handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (guiManager.isSwagSlayerGUI(topInv)) {
            guiManager.removeTracking(topInv);
        }
    }

    // -------------------------------------------------------------------------
    // Per-screen click handlers
    // -------------------------------------------------------------------------

    private void handleMainMenuClick(Player player, Inventory inv, int slot) {
        // Slots 10, 12, 14, 16 → type detail views.
        SlayerType typeAtSlot = guiManager.getTypeForMainMenuSlot(slot);
        if (typeAtSlot != null) {
            guiManager.openTypeMenu(player, typeAtSlot);
            return;
        }

        // Slot 49 → overall leaderboard (back destination = null → main menu).
        if (slot == 49) {
            guiManager.openLeaderboardWithBack(player, null, null);
        }
    }

    private void handleTypeDetailClick(Player player, Inventory inv, int slot) {
        SlayerType type = guiManager.getGUIContext(inv);

        if (slot == 29 && type != null) {
            // Task button — assign task if none active, otherwise do nothing.
            player.closeInventory();
            slayerManager.assignTask(player, type);
            return;
        }

        if (slot == 33 && type != null) {
            // Leaderboard button — back destination is this type's detail screen.
            guiManager.openLeaderboardWithBack(player, type, type);
            return;
        }

        if (slot == 48) {
            // Back button → main menu.
            guiManager.openMainMenu(player);
        }
    }

    private void handleLeaderboardClick(Player player, Inventory inv, int slot) {
        if (slot == 49) {
            // Back button — destination depends on how the leaderboard was opened.
            SlayerType backDest = guiManager.getLeaderboardBack(inv);
            if (backDest == null) {
                // Came from the main menu or overall leaderboard.
                guiManager.openMainMenu(player);
            } else {
                // Came from a type detail screen.
                guiManager.openTypeMenu(player, backDest);
            }
        }
    }
}

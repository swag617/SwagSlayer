package com.swag.swagslayer.listeners;

import com.swag.swagslayer.managers.BossManager;
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
 *     Slot 22            → openContracts(player)
 *     Slot 31            → openBestiary(player)
 *     Slot 49            → openLeaderboardWithBack(player, null, null)
 *
 *   TYPE_DETAIL
 *     Slot 11            → summonBoss (if allowed)
 *     Slot 29            → assignTask
 *     Slot 33            → openLeaderboardWithBack(player, type, type)
 *     Slot 48            → openMainMenu(player)
 *
 *   LEADERBOARD
 *     Slot 49            → back: null→main menu, non-null→type detail
 *
 *   BESTIARY / CONTRACTS
 *     Slot 49            → openMainMenu(player)
 */
public class GUIListener implements Listener {

    private final GUIManager guiManager;
    private final SlayerManager slayerManager;
    private final BossManager bossManager;

    public GUIListener(GUIManager guiManager, SlayerManager slayerManager, BossManager bossManager) {
        this.guiManager    = guiManager;
        this.slayerManager = slayerManager;
        this.bossManager   = bossManager;
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!guiManager.isSwagSlayerGUI(topInv)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != topInv) return;

        int slot = event.getSlot();
        GUIType guiType = guiManager.getGUIType(topInv);
        if (guiType == null) return;

        switch (guiType) {
            case MAIN_MENU   -> handleMainMenuClick(player, topInv, slot);
            case TYPE_DETAIL -> handleTypeDetailClick(player, topInv, slot);
            case LEADERBOARD -> handleLeaderboardClick(player, topInv, slot);
            case BESTIARY    -> handleSimpleBackClick(player, slot);
            case CONTRACTS   -> handleSimpleBackClick(player, slot);
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
        SlayerType typeAtSlot = guiManager.getTypeForMainMenuSlot(slot);
        if (typeAtSlot != null) {
            guiManager.openTypeMenu(player, typeAtSlot);
            return;
        }

        if (slot == 22) {
            guiManager.openContracts(player);
            return;
        }

        if (slot == 31) {
            guiManager.openBestiary(player);
            return;
        }

        if (slot == 49) {
            guiManager.openLeaderboardWithBack(player, null, null);
        }
    }

    private void handleTypeDetailClick(Player player, Inventory inv, int slot) {
        SlayerType type = guiManager.getGUIContext(inv);

        if (slot == 11 && type != null && bossManager != null) {
            // Boss summon — canSummon sends its own error message if blocked.
            if (bossManager.canSummon(player, type)) {
                player.closeInventory();
                bossManager.summonBoss(player, type);
            }
            return;
        }

        if (slot == 29 && type != null) {
            player.closeInventory();
            slayerManager.assignTask(player, type);
            return;
        }

        if (slot == 33 && type != null) {
            guiManager.openLeaderboardWithBack(player, type, type);
            return;
        }

        if (slot == 48) {
            guiManager.openMainMenu(player);
        }
    }

    private void handleLeaderboardClick(Player player, Inventory inv, int slot) {
        if (slot == 49) {
            SlayerType backDest = guiManager.getLeaderboardBack(inv);
            if (backDest == null) {
                guiManager.openMainMenu(player);
            } else {
                guiManager.openTypeMenu(player, backDest);
            }
        }
    }

    /** For screens that only have a Back button at slot 49 (BESTIARY, CONTRACTS). */
    private void handleSimpleBackClick(Player player, int slot) {
        if (slot == 49) {
            guiManager.openMainMenu(player);
        }
    }
}

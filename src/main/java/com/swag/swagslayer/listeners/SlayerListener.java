package com.swag.swagslayer.listeners;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.managers.DataManager;
import com.swag.swagslayer.managers.SlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for entity deaths and routes qualifying kills to SlayerManager.
 *
 * Also handles player join/quit to warm/flush the profile cache.
 */
public class SlayerListener implements Listener {

    private final SwagSlayer plugin;
    private final SlayerManager slayerManager;
    private final DataManager dataManager;

    public SlayerListener(SwagSlayer plugin, SlayerManager slayerManager, DataManager dataManager) {
        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.dataManager = dataManager;
    }

    /**
     * Fires when any entity dies. We only process kills where:
     *   1. The killer is a Player (not a projectile owned by a non-player, not
     *      environmental damage).
     *   2. The dead entity is not a player (we don't track PvP kills here).
     *
     * Priority is NORMAL so other plugins can cancel the event first if needed.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        slayerManager.handleKill(killer, event.getEntity().getType());
    }

    /**
     * Pre-loads the player's profile into cache on join so the first kill
     * doesn't trigger a synchronous disk read mid-gameplay.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // getProfile() populates the cache if not already loaded.
        dataManager.getProfile(event.getPlayer().getUniqueId());
    }

    /**
     * Saves and unloads the profile when the player leaves.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        dataManager.saveProfile(dataManager.getProfile(uuid));
        dataManager.unloadProfile(uuid);
        slayerManager.clearStreak(uuid);
    }
}

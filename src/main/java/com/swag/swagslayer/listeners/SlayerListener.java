package com.swag.swagslayer.listeners;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.managers.BossManager;
import com.swag.swagslayer.managers.ContractManager;
import com.swag.swagslayer.managers.DataManager;
import com.swag.swagslayer.managers.SlayerManager;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Routes entity deaths to the appropriate subsystem.
 *
 * Boss kills and regular kills are mutually exclusive:
 *   - If BossManager.handleBossDeath() returns true, the entity was a tagged
 *     boss. Regular kill logic (XP, task, contracts, cosmetics) is skipped so
 *     the player doesn't receive double XP and the task doesn't tick for boss
 *     kills.
 *   - If handleBossDeath() returns false, normal kill processing runs.
 */
public class SlayerListener implements Listener {

    private final SwagSlayer plugin;
    private final SlayerManager slayerManager;
    private final DataManager dataManager;
    private final BossManager bossManager;
    private final ContractManager contractManager;

    public SlayerListener(SwagSlayer plugin,
                          SlayerManager slayerManager,
                          DataManager dataManager,
                          BossManager bossManager,
                          ContractManager contractManager) {
        this.plugin          = plugin;
        this.slayerManager   = slayerManager;
        this.dataManager     = dataManager;
        this.bossManager     = bossManager;
        this.contractManager = contractManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        LivingEntity entity = event.getEntity();

        // Boss check first — returns true if the entity was a tagged boss.
        // Boss kills are handled entirely by BossManager (owner gets XP,
        // boss kill is tracked for bestiary). Regular kill logic is skipped
        // to prevent double XP and unintended task progress.
        if (bossManager.handleBossDeath(killer, entity)) return;

        // Regular kill — XP, kill count, task progress, combo cosmetics.
        slayerManager.handleKill(killer, entity);

        // Contract tick — only for mob types tracked by a SlayerType.
        SlayerType slayerType = SlayerType.fromEntityType(entity.getType());
        if (slayerType != null) {
            contractManager.handleKill(killer, slayerType);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        dataManager.getProfile(uuid);
        contractManager.onPlayerJoin(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        dataManager.saveProfile(dataManager.getProfile(uuid));
        dataManager.unloadProfile(uuid);
        slayerManager.clearStreak(uuid);
        contractManager.onPlayerQuit(uuid);
    }
}

package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.BossSpawn;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Manages per-player boss summons.
 *
 * Rules:
 *   - A player may have at most one active boss per SlayerType.
 *   - Boss level requirement: MIN_LEVEL_FOR_BOSS (currently 2).
 *   - Bosses expire after 5 minutes; cleanup runs every 10 seconds.
 *   - XP is always awarded to the boss OWNER, not whoever lands the killing blow.
 *     This prevents other players from stealing credit for a summoned boss.
 *   - Boss kills do NOT also fire regular kill logic in SlayerListener.
 *     handleBossDeath() returns true when it consumes a boss death so the
 *     listener can skip the normal kill path.
 */
public class BossManager {

    /** Minimum slayer level required to summon a boss. Used by GUIManager too. */
    public static final int MIN_LEVEL_FOR_BOSS = 2;

    private final SwagSlayer plugin;
    private final DataManager dataManager;
    private CosmeticsManager cosmeticsManager;
    private SlayerManager slayerManager;

    private final Map<UUID, BossSpawn> activeBosses = new HashMap<>();
    private final Random random = new Random();

    private final NamespacedKey keyOwner;
    private final NamespacedKey keyType;

    public BossManager(SwagSlayer plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.keyOwner = new NamespacedKey(plugin, "boss_owner");
        this.keyType  = new NamespacedKey(plugin, "boss_type");

        // Cleanup expired bosses every 10 seconds (200 ticks).
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredBosses, 200L, 200L);
    }

    public void setCosmeticsManager(CosmeticsManager cosmeticsManager) {
        this.cosmeticsManager = cosmeticsManager;
    }

    public void setSlayerManager(SlayerManager slayerManager) {
        this.slayerManager = slayerManager;
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    public boolean isActiveBossForType(UUID playerUuid, SlayerType type) {
        BossSpawn spawn = activeBosses.get(playerUuid);
        return spawn != null && spawn.getType() == type && !spawn.isExpired();
    }

    public BossSpawn getBossSpawn(UUID playerUuid) {
        return activeBosses.get(playerUuid);
    }

    // -------------------------------------------------------------------------
    // Summon
    // -------------------------------------------------------------------------

    /**
     * Checks whether the player is allowed to summon a boss of the given type,
     * sending a descriptive error message if not.
     */
    public boolean canSummon(Player player, SlayerType type) {
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        if (profile.getLevel(type) < MIN_LEVEL_FOR_BOSS) {
            player.sendMessage(ChatColor.RED + "You need "
                    + ChatColor.WHITE + type.getDisplayName() + " Slayer Level " + MIN_LEVEL_FOR_BOSS
                    + ChatColor.RED + " to summon a boss!");
            return false;
        }
        if (isActiveBossForType(player.getUniqueId(), type)) {
            player.sendMessage(ChatColor.RED + "You already have an active "
                    + type.getDisplayName() + " boss!");
            return false;
        }
        return true;
    }

    /**
     * Spawns a scaled boss near the player and registers it.
     * Callers must check {@link #canSummon} first.
     */
    public void summonBoss(Player player, SlayerType type) {
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        int level = profile.getLevel(type);
        double hp = 40.0 * level;

        // Random horizontal offset, 5 blocks away, snapped to ground level.
        double angle = random.nextDouble() * 2 * Math.PI;
        Location loc = player.getLocation().clone()
                .add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);

        // Find the highest solid block at the target column so the boss lands
        // on terrain rather than floating mid-air or clipping into the ground.
        if (loc.getWorld() != null) {
            int highestY = loc.getWorld().getHighestBlockYAt(loc);
            loc.setY(highestY + 1);
        }

        Entity entity = player.getWorld().spawnEntity(loc, type.getBossEntityType());
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            player.sendMessage(ChatColor.RED + "Failed to spawn boss entity.");
            return;
        }

        AttributeInstance maxHealth = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(hp);
        }
        living.setHealth(hp);

        living.setCustomName("§c§l" + type.getDisplayName() + " Boss §8[Lv." + level + "]");
        living.setCustomNameVisible(true);

        PersistentDataContainer pdc = living.getPersistentDataContainer();
        pdc.set(keyOwner, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(keyType,  PersistentDataType.STRING, type.name());

        activeBosses.put(player.getUniqueId(),
                new BossSpawn(player.getUniqueId(), type, living.getUniqueId()));

        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Boss summoned! "
                + ChatColor.RESET + ChatColor.RED + "Defeat it within 5 minutes!");
    }

    // -------------------------------------------------------------------------
    // Death handling
    // -------------------------------------------------------------------------

    /**
     * Called from SlayerListener on every EntityDeathEvent.
     *
     * @return true  if the entity was a tagged boss (event consumed — caller
     *               must skip normal kill processing for this entity).
     *         false if the entity is not a boss (caller continues normally).
     */
    public boolean handleBossDeath(Player killer, LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(keyOwner, PersistentDataType.STRING)) return false;

        String ownerStr = pdc.get(keyOwner, PersistentDataType.STRING);
        String typeStr  = pdc.get(keyType,  PersistentDataType.STRING);
        if (ownerStr == null || typeStr == null) return false;

        UUID ownerUuid;
        try {
            ownerUuid = UUID.fromString(ownerStr);
        } catch (IllegalArgumentException e) {
            return false;
        }

        SlayerType type = SlayerType.fromName(typeStr);
        if (type == null) return false;

        // Remove from active bosses regardless of outcome.
        BossSpawn spawn = activeBosses.remove(ownerUuid);

        if (spawn == null || spawn.isExpired()) {
            killer.sendMessage(ChatColor.RED + "That boss expired — no reward.");
            return true; // still consumed
        }

        // --- Award XP to the OWNER, not the killer ---
        // This prevents other players from stealing credit for a summoned boss.
        SlayerProfile ownerProfile = dataManager.getProfile(ownerUuid);
        int level  = ownerProfile.getLevel(type);
        int reward = type.getBossXpReward() * level;
        ownerProfile.incrementBossKills(type);

        Player ownerPlayer = Bukkit.getPlayer(ownerUuid);
        if (ownerPlayer != null && slayerManager != null) {
            // Owner is online — award with full feedback (level-up messages, perks).
            slayerManager.awardXp(ownerPlayer, type, reward);
            ownerPlayer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Boss defeated! "
                    + ChatColor.RESET + ChatColor.YELLOW + "Earned "
                    + ChatColor.GOLD + reward + " XP" + ChatColor.YELLOW
                    + " for slaying the " + type.getDisplayName() + " Boss!");
            if (cosmeticsManager != null) {
                cosmeticsManager.handleBossKillCosmetics(ownerPlayer, entity.getLocation());
            }
        } else {
            // Owner offline — award silently; profile is saved on shutdown.
            ownerProfile.addXp(type, reward);
        }

        // Notify the killer if they were not the owner.
        if (!killer.getUniqueId().equals(ownerUuid)) {
            killer.sendMessage(ChatColor.YELLOW + "You helped defeat a "
                    + type.getDisplayName() + " boss — the owner received the reward.");
        }

        publishDiscordNotify(ownerPlayer != null ? ownerPlayer.getName() : ownerStr, type, reward);

        return true;
    }

    /**
     * Optional Discord announcement — DiscordUtils (if installed with a matching
     * webhooks.* entry configured) picks this up with zero coupling here.
     */
    private void publishDiscordNotify(String ownerName, SlayerType type, int reward) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)) return;
        com.SwagDev.SwagAPI.api.IEventBusService bus = plugin.getBusService();
        if (bus == null) return;

        String webhookName = plugin.getConfig().getString("discord.webhook-name", "slayer");
        Map<String, Object> data = new HashMap<>();
        data.put("webhook", webhookName);
        data.put("description", "**" + ownerName + "** slew the **" + type.getDisplayName()
                + "** boss for " + reward + " XP!");
        data.put("color", 0xAA0000);
        data.put("username", "SwagSlayer");

        bus.publish(new com.SwagDev.SwagAPI.events.SwagCrossPluginMessageEvent(
                "discordutils:notify", "SwagSlayer", data, null));
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    public void cleanupExpiredBosses() {
        Iterator<Map.Entry<UUID, BossSpawn>> it = activeBosses.entrySet().iterator();
        while (it.hasNext()) {
            BossSpawn spawn = it.next().getValue();
            if (spawn.isExpired()) {
                Entity e = Bukkit.getEntity(spawn.getBossEntityUuid());
                if (e != null && !e.isDead()) {
                    e.remove();
                }
                it.remove();
            }
        }
    }
}

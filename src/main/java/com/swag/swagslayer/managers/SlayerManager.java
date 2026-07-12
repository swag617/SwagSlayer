package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerTask;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Core slayer logic: kill handling, XP awards, combo streaks, task assignment,
 * and config value application to SlayerType enum constants.
 *
 * awardXp() is the single point of truth for giving XP with level-up feedback.
 * Both BossManager and ContractManager delegate to it so level-up messages and
 * perk notifications are never silently dropped.
 */
public class SlayerManager {

    private final SwagSlayer plugin;
    private final DataManager dataManager;

    private CosmeticsManager cosmeticsManager;
    private PerkManager perkManager;

    private final Map<UUID, Integer> streakCount   = new HashMap<>();
    private final Map<UUID, Long>    lastKillTime  = new HashMap<>();

    private final Random random = new Random();

    public SlayerManager(SwagSlayer plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        applyConfigToTypes();
    }

    public void setCosmeticsManager(CosmeticsManager cosmeticsManager) {
        this.cosmeticsManager = cosmeticsManager;
    }

    public void setPerkManager(PerkManager perkManager) {
        this.perkManager = perkManager;
    }

    // -------------------------------------------------------------------------
    // Config application
    // -------------------------------------------------------------------------

    /**
     * Reads config overrides into SlayerType enum constants.
     *
     * Two-phase: validate and collect all changes first, then apply all at once.
     * This prevents a partial-config state if parsing fails mid-loop.
     */
    public void applyConfigToTypes() {
        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("slayer_types");
        if (typesSection == null) {
            plugin.getLogger().warning("Config is missing 'slayer_types' section — using built-in defaults.");
            return;
        }

        int maxLevel = plugin.getConfig().getInt("general.max_level", 5);

        // Phase 1 — collect.
        record TypeUpdate(
            String displayName,
            EntityType bossEntityType,
            int[] killThresholds,
            int xpPerKill,
            int bossXpReward
        ) {}

        Map<SlayerType, TypeUpdate> pending = new HashMap<>();

        for (SlayerType type : SlayerType.values()) {
            ConfigurationSection sec = typesSection.getConfigurationSection(type.name());
            if (sec == null) {
                plugin.getLogger().warning("No config entry for slayer type " + type.name() + " — using defaults.");
                continue;
            }

            String newDisplayName = sec.getString("display_name", type.getDisplayName());

            String bossMobStr = sec.getString("boss_mob", type.getBossEntityType().name());
            EntityType newBossType;
            try {
                newBossType = EntityType.valueOf(bossMobStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown boss_mob '" + bossMobStr + "' for type "
                        + type.name() + " — keeping existing value.");
                newBossType = type.getBossEntityType();
            }

            java.util.List<Integer> thresholdList = sec.getIntegerList("kill_threshold_per_level");
            int[] newThresholds;
            if (!thresholdList.isEmpty()) {
                newThresholds = new int[maxLevel];
                for (int i = 0; i < maxLevel; i++) {
                    newThresholds[i] = (i < thresholdList.size()) ? thresholdList.get(i) : 100;
                }
            } else {
                newThresholds = type.getKillThresholds(); // keep existing
            }

            int newXpPerKill    = sec.getInt("xp_per_kill",    type.getXpPerKill());
            int newBossXpReward = sec.getInt("boss_xp_reward", type.getBossXpReward());

            pending.put(type, new TypeUpdate(newDisplayName, newBossType, newThresholds, newXpPerKill, newBossXpReward));
        }

        // Phase 2 — apply atomically.
        for (Map.Entry<SlayerType, TypeUpdate> entry : pending.entrySet()) {
            SlayerType type  = entry.getKey();
            TypeUpdate update = entry.getValue();
            type.setDisplayName(update.displayName());
            type.setBossEntityType(update.bossEntityType());
            type.setKillThresholds(update.killThresholds());
            type.setXpPerKill(update.xpPerKill());
            type.setBossXpReward(update.bossXpReward());
        }
    }

    // -------------------------------------------------------------------------
    // XP award — single point of truth
    // -------------------------------------------------------------------------

    /**
     * Awards XP to a player and fires level-up feedback if a threshold is crossed.
     * All subsystems (SlayerManager, BossManager, ContractManager) must use this
     * method so that level-up messages and perk notifications are never silently dropped.
     */
    public void awardXp(Player player, SlayerType type, int amount) {
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        boolean leveledUp = profile.addXp(type, amount);
        if (leveledUp) {
            int newLevel = profile.getLevel(type);
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "LEVEL UP! "
                    + ChatColor.RESET + ChatColor.GREEN
                    + type.getDisplayName() + " Slayer is now level " + newLevel + "!");
            if (perkManager != null) {
                perkManager.notifyLevelUp(player, type, newLevel);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Kill handling
    // -------------------------------------------------------------------------

    /**
     * Called by SlayerListener when a player kills a tracked regular mob.
     * Do NOT call this for boss entities — BossManager handles those separately.
     */
    public void handleKill(Player player, LivingEntity entity) {
        SlayerType slayerType = SlayerType.fromEntityType(entity.getType());
        if (slayerType == null) return;

        UUID uuid = player.getUniqueId();
        SlayerProfile profile = dataManager.getProfile(uuid);

        updateStreak(uuid);
        double multiplier = getComboMultiplier(uuid);
        int streak = getStreak(uuid);

        profile.setKillCount(slayerType, profile.getKillCount(slayerType) + 1);

        int earnedXp = (int) Math.round(slayerType.getXpPerKill() * multiplier);
        awardXp(player, slayerType, earnedXp);

        // Task progress.
        SlayerTask task = profile.getActiveTask(slayerType);
        if (task != null) {
            task.incrementKills();
            if (task.isComplete()) {
                profile.clearTask(slayerType);
                int reward = slayerType.getBossXpReward();
                awardXp(player, slayerType, reward);
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Task complete! "
                        + ChatColor.RESET + ChatColor.YELLOW
                        + "You finished your " + slayerType.getDisplayName() + " task and earned "
                        + ChatColor.GOLD + reward + " XP" + ChatColor.YELLOW + "!");
            } else {
                player.sendMessage(ChatColor.GRAY + "[" + slayerType.getDisplayName() + " Task] "
                        + ChatColor.WHITE + task.getKillsCompleted()
                        + ChatColor.GRAY + "/" + ChatColor.WHITE + task.getKillGoal());
            }
        }

        if (cosmeticsManager != null) {
            cosmeticsManager.handleKillCosmetics(player, entity.getLocation(), streak, multiplier);
        }
    }

    // -------------------------------------------------------------------------
    // Task assignment
    // -------------------------------------------------------------------------

    public void assignTask(Player player, SlayerType type) {
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());
        if (profile.getActiveTask(type) != null) {
            player.sendMessage(ChatColor.RED + "You already have an active "
                    + type.getDisplayName() + " task!");
            return;
        }

        int level = profile.getLevel(type);
        int[] range = taskRangeForLevel(level);
        int goal = range[0] + random.nextInt(range[1] - range[0] + 1);

        SlayerTask task = new SlayerTask(type, goal);
        profile.setActiveTask(type, task);

        player.sendMessage(ChatColor.GOLD + "New " + type.getDisplayName() + " task: "
                + ChatColor.WHITE + "Kill " + goal + " " + type.getDisplayName() + "s!");
    }

    private int[] taskRangeForLevel(int level) {
        return switch (level) {
            case 1  -> new int[]{15,  25};
            case 2  -> new int[]{30,  50};
            case 3  -> new int[]{60, 100};
            case 4  -> new int[]{120, 200};
            default -> new int[]{200, 350};
        };
    }

    // -------------------------------------------------------------------------
    // Combo streak
    // -------------------------------------------------------------------------

    private void updateStreak(UUID uuid) {
        long now      = System.currentTimeMillis();
        long timeoutMs = plugin.getConfig().getLong("general.combo_timeout_seconds", 10) * 1000L;
        Long last     = lastKillTime.get(uuid);

        if (last == null || (now - last) > timeoutMs) {
            streakCount.put(uuid, 1);
        } else {
            streakCount.merge(uuid, 1, Integer::sum);
        }
        lastKillTime.put(uuid, now);
    }

    public double getComboMultiplier(UUID uuid) {
        int streak       = streakCount.getOrDefault(uuid, 1);
        double perStreak = plugin.getConfig().getDouble("general.combo_multiplier_per_streak", 0.1);
        return Math.min(2.0, 1.0 + ((streak - 1) * perStreak));
    }

    public int getStreak(UUID uuid) {
        return streakCount.getOrDefault(uuid, 1);
    }

    public void clearStreak(UUID uuid) {
        streakCount.remove(uuid);
        lastKillTime.remove(uuid);
    }
}

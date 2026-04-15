package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerTask;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Core slayer logic: kill handling, XP calculation, combo streaks, and
 * config value application to SlayerType enum constants.
 *
 * Combo system:
 *   - Each kill within combo_timeout_seconds of the last kill increments the streak.
 *   - The XP multiplier is 1.0 + (streak * combo_multiplier_per_streak), capped at 2.0.
 *   - The streak is per-player and per-type-agnostic (any slayer kill keeps the streak alive).
 */
public class SlayerManager {

    private final SwagSlayer plugin;
    private final DataManager dataManager;

    /** Current consecutive kill streak per player. */
    private final Map<UUID, Integer> streakCount = new HashMap<>();
    /** Timestamp (ms) of the last qualifying kill per player. */
    private final Map<UUID, Long> lastKillTime = new HashMap<>();

    private final Random random = new Random();

    public SlayerManager(SwagSlayer plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        applyConfigToTypes();
    }

    // -------------------------------------------------------------------------
    // Config application
    // -------------------------------------------------------------------------

    /**
     * Reads the slayer_types section from config and pushes the values into the
     * SlayerType enum constants. Called on startup and on /slayadmin reload.
     *
     * This allows server operators to change display names, kill thresholds, and
     * XP values without recompiling the plugin.
     */
    public void applyConfigToTypes() {
        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("slayer_types");
        if (typesSection == null) {
            plugin.getLogger().warning("Config is missing 'slayer_types' section — using built-in defaults.");
            return;
        }

        for (SlayerType type : SlayerType.values()) {
            ConfigurationSection sec = typesSection.getConfigurationSection(type.name());
            if (sec == null) {
                plugin.getLogger().warning("No config entry for slayer type " + type.name() + " — using defaults.");
                continue;
            }

            String displayName = sec.getString("display_name", type.getDisplayName());
            type.setDisplayName(displayName);

            String bossMobStr = sec.getString("boss_mob", type.name());
            try {
                EntityType et = EntityType.valueOf(bossMobStr.toUpperCase());
                type.setBossEntityType(et);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown boss_mob '" + bossMobStr + "' for type " + type.name()
                        + " — keeping previous value.");
            }

            // kill_threshold_per_level is a List<Integer> in YAML.
            java.util.List<Integer> thresholdList = sec.getIntegerList("kill_threshold_per_level");
            int maxLevel = plugin.getConfig().getInt("general.max_level", 5);
            if (!thresholdList.isEmpty()) {
                int[] thresholds = new int[maxLevel];
                for (int i = 0; i < maxLevel; i++) {
                    thresholds[i] = (i < thresholdList.size()) ? thresholdList.get(i) : 100;
                }
                type.setKillThresholds(thresholds);
            }

            type.setXpPerKill(sec.getInt("xp_per_kill", type.getXpPerKill()));
            type.setBossXpReward(sec.getInt("boss_xp_reward", type.getBossXpReward()));
        }
    }

    // -------------------------------------------------------------------------
    // Kill handling
    // -------------------------------------------------------------------------

    /**
     * Main entry point called by SlayerListener when a player kills a mob.
     *
     * Steps:
     *   1. Resolve EntityType -> SlayerType (null = not a tracked mob, return early).
     *   2. Update combo streak.
     *   3. Increment kill count.
     *   4. Calculate XP with combo multiplier.
     *   5. Add XP to profile; if level-up occurs, notify the player.
     */
    public void handleKill(Player player, EntityType entityType) {
        SlayerType slayerType = SlayerType.fromEntityType(entityType);
        if (slayerType == null) return;

        UUID uuid = player.getUniqueId();
        SlayerProfile profile = dataManager.getProfile(uuid);

        // --- Combo streak update ---
        updateStreak(uuid);
        double multiplier = getComboMultiplier(uuid);

        // --- Kill count ---
        profile.setKillCount(slayerType, profile.getKillCount(slayerType) + 1);

        // --- XP calculation ---
        int baseXp = slayerType.getXpPerKill();
        int earnedXp = (int) Math.round(baseXp * multiplier);

        // --- Level-up check ---
        boolean leveledUp = profile.addXp(slayerType, earnedXp);

        // --- Level-up feedback ---
        if (leveledUp) {
            int newLevel = profile.getLevel(slayerType);
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "LEVEL UP! "
                    + ChatColor.RESET + ChatColor.GREEN
                    + slayerType.getDisplayName() + " Slayer is now level " + newLevel + "!");
        }

        // --- Task progress ---
        SlayerTask task = profile.getActiveTask(slayerType);
        if (task != null) {
            task.incrementKills();
            if (task.isComplete()) {
                profile.clearTask(slayerType);
                int reward = slayerType.getBossXpReward();
                profile.addXp(slayerType, reward);
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Task complete! "
                        + ChatColor.RESET + ChatColor.YELLOW
                        + "You finished your " + slayerType.getDisplayName() + " task and earned "
                        + ChatColor.GOLD + reward + " XP" + ChatColor.YELLOW + "!");
            } else {
                player.sendMessage(ChatColor.GRAY + "[" + slayerType.getDisplayName() + " Task] "
                        + ChatColor.WHITE + task.getKillsCompleted() + ChatColor.GRAY + "/"
                        + ChatColor.WHITE + task.getKillGoal());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Task assignment
    // -------------------------------------------------------------------------

    /**
     * Assigns a new slayer task to the player for the given type.
     * Does nothing if the player already has an active task for that type.
     * Kill goal is randomised based on the player's current level.
     */
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

    /** Returns {min, max} kill goal range for the given slayer level. */
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

    /**
     * Updates the streak counter for the given player.
     * If the time since last kill exceeds the configured timeout, the streak resets to 1.
     * Otherwise the streak increments by 1.
     */
    private void updateStreak(UUID uuid) {
        long now = System.currentTimeMillis();
        long timeoutMs = plugin.getConfig().getLong("general.combo_timeout_seconds", 10) * 1000L;
        Long last = lastKillTime.get(uuid);

        if (last == null || (now - last) > timeoutMs) {
            streakCount.put(uuid, 1);
        } else {
            streakCount.merge(uuid, 1, Integer::sum);
        }
        lastKillTime.put(uuid, now);
    }

    /**
     * Returns the XP multiplier for the player's current streak.
     * Formula: 1.0 + (streak * combo_multiplier_per_streak), capped at 2.0.
     * A streak of 1 (first kill or after timeout) yields exactly 1.0.
     */
    public double getComboMultiplier(UUID uuid) {
        int streak = streakCount.getOrDefault(uuid, 1);
        double perStreak = plugin.getConfig().getDouble("general.combo_multiplier_per_streak", 0.1);
        // Streak of 1 = no bonus (first kill in a chain).
        double raw = 1.0 + ((streak - 1) * perStreak);
        return Math.min(2.0, raw);
    }

    /**
     * Returns the current streak count for a player (1 = no combo active).
     */
    public int getStreak(UUID uuid) {
        return streakCount.getOrDefault(uuid, 1);
    }

    /**
     * Cleans up streak state when a player leaves to prevent memory leaks.
     */
    public void clearStreak(UUID uuid) {
        streakCount.remove(uuid);
        lastKillTime.remove(uuid);
    }
}

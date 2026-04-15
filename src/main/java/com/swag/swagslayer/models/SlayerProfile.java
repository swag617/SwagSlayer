package com.swag.swagslayer.models;

import com.swag.swagslayer.SwagSlayer;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds one player's complete slayer progression state.
 *
 * Level semantics:
 *   - Levels run from 1 (starting) to max_level (default 5).
 *   - XP is accumulated per type. XP thresholds for level-up are computed as:
 *       threshold(level N -> N+1) = killThresholds[N-1] * xpPerKill
 *     where killThresholds and xpPerKill come from the SlayerType at the time
 *     addXp() is called, so config reloads take effect immediately.
 *   - Kill counts are tracked independently of XP and are purely informational.
 *
 * This class is not thread-safe. All access must occur on the main server thread.
 */
public class SlayerProfile {

    private final UUID playerUuid;

    // All maps are pre-populated for every SlayerType so null checks are unnecessary.
    private final Map<SlayerType, Integer> levels     = new EnumMap<>(SlayerType.class);
    private final Map<SlayerType, Integer> xp         = new EnumMap<>(SlayerType.class);
    private final Map<SlayerType, Integer> killCounts = new EnumMap<>(SlayerType.class);
    /** Active task per type — null entry means no active task for that type. */
    private final Map<SlayerType, SlayerTask> activeTasks = new EnumMap<>(SlayerType.class);

    public SlayerProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
        for (SlayerType type : SlayerType.values()) {
            levels.put(type, 1);
            xp.put(type, 0);
            killCounts.put(type, 0);
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getLevel(SlayerType type) {
        return levels.getOrDefault(type, 1);
    }

    public int getXp(SlayerType type) {
        return xp.getOrDefault(type, 0);
    }

    public int getKillCount(SlayerType type) {
        return killCounts.getOrDefault(type, 0);
    }

    /** Exposes the full levels map for serialization. Caller should not mutate. */
    public Map<SlayerType, Integer> getLevels() {
        return levels;
    }

    /** Exposes the full xp map for serialization. Caller should not mutate. */
    public Map<SlayerType, Integer> getXpMap() {
        return xp;
    }

    /** Exposes the full killCounts map for serialization. Caller should not mutate. */
    public Map<SlayerType, Integer> getKillCounts() {
        return killCounts;
    }

    // -------------------------------------------------------------------------
    // Setters (used by admin commands and deserialization)
    // -------------------------------------------------------------------------

    public void setLevel(SlayerType type, int level) {
        int maxLevel = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        levels.put(type, Math.max(1, Math.min(level, maxLevel)));
    }

    public void setXp(SlayerType type, int amount) {
        xp.put(type, Math.max(0, amount));
    }

    public void setKillCount(SlayerType type, int count) {
        killCounts.put(type, Math.max(0, count));
    }

    // -------------------------------------------------------------------------
    // Task getters / setters
    // -------------------------------------------------------------------------

    /** Returns the active task for this type, or null if none is assigned. */
    public SlayerTask getActiveTask(SlayerType type) {
        return activeTasks.get(type);
    }

    public void setActiveTask(SlayerType type, SlayerTask task) {
        activeTasks.put(type, task);
    }

    public void clearTask(SlayerType type) {
        activeTasks.remove(type);
    }

    public Map<SlayerType, SlayerTask> getActiveTasks() {
        return activeTasks;
    }

    /** Resets all progression for every type back to starting values. */
    public void resetAll() {
        for (SlayerType type : SlayerType.values()) {
            levels.put(type, 1);
            xp.put(type, 0);
            killCounts.put(type, 0);
            activeTasks.remove(type);
        }
    }

    // -------------------------------------------------------------------------
    // Progression logic
    // -------------------------------------------------------------------------

    /**
     * Adds the given XP amount to the specified type and checks for level-ups.
     *
     * Level-up threshold for level N -> N+1:
     *   killThresholds[N-1] * xpPerKill
     *
     * XP is NOT reset on level-up — it remains cumulative so that partial
     * progress toward the next level is preserved correctly.
     *
     * @return true if at least one level-up occurred, false otherwise.
     */
    public boolean addXp(SlayerType type, int amount) {
        int maxLevel = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        int currentLevel = getLevel(type);

        // Already at cap — record XP for display but no level-up possible.
        if (currentLevel >= maxLevel) {
            xp.merge(type, amount, Integer::sum);
            return false;
        }

        xp.merge(type, amount, Integer::sum);

        boolean leveledUp = false;
        // Loop in case multiple levels are earned from one large XP gain.
        while (getLevel(type) < maxLevel) {
            int lvl = getLevel(type); // current level, 1-based
            int threshold = getXpThresholdForLevel(type, lvl);
            if (getXp(type) >= threshold) {
                levels.put(type, lvl + 1);
                leveledUp = true;
            } else {
                break;
            }
        }
        return leveledUp;
    }

    /**
     * Returns the cumulative XP required to reach level (currentLevel + 1).
     *
     * Cumulative means the total XP the player must have earned since level 1,
     * not the incremental XP needed since the last level-up. This simplifies the
     * comparison in addXp() because we always compare against the running total.
     *
     * For level N (1-based), the XP to reach level N+1 is:
     *   sum(killThresholds[0..N-1]) * xpPerKill
     */
    private int getXpThresholdForLevel(SlayerType type, int currentLevel) {
        int[] thresholds = type.getKillThresholds();
        int xpPerKill = type.getXpPerKill();
        // Sum kill thresholds from level 1 up to and including the current level.
        int totalKills = 0;
        for (int i = 0; i < currentLevel && i < thresholds.length; i++) {
            totalKills += thresholds[i];
        }
        return totalKills * xpPerKill;
    }

    /**
     * Returns the number of kills still needed to reach the next level.
     * Returns 0 if the player is at max level.
     */
    public int getKillsForNextLevel(SlayerType type) {
        int maxLevel = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        int currentLevel = getLevel(type);
        if (currentLevel >= maxLevel) return 0;

        int xpNeeded = getXpThresholdForLevel(type, currentLevel);
        int xpNow = getXp(type);
        int remaining = xpNeeded - xpNow;
        int xpPerKill = type.getXpPerKill();
        if (xpPerKill <= 0) return 0;
        // Ceiling division — partial-kill XP still counts as progress.
        return (int) Math.ceil(Math.max(0, remaining) / (double) xpPerKill);
    }

    /**
     * Returns a 0.0–1.0 float representing progress toward the next level.
     * Returns 1.0 if the player is at max level.
     */
    public float getLevelProgress(SlayerType type) {
        int maxLevel = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        int currentLevel = getLevel(type);
        if (currentLevel >= maxLevel) return 1.0f;

        // XP already accumulated before this level started.
        int xpAtLevelStart = (currentLevel == 1) ? 0 : getXpThresholdForLevel(type, currentLevel - 1);
        int xpAtLevelEnd   = getXpThresholdForLevel(type, currentLevel);
        int xpIntoLevel    = getXp(type) - xpAtLevelStart;
        int xpSpan         = xpAtLevelEnd - xpAtLevelStart;

        if (xpSpan <= 0) return 1.0f;
        return Math.min(1.0f, Math.max(0.0f, xpIntoLevel / (float) xpSpan));
    }
}

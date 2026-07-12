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
 *   - XP is accumulated per type. The cumulative XP required to reach level N+1 is:
 *       sum(killThresholds[0..N-1]) * xpPerKill
 *   - Kill counts are tracked independently of XP and are purely informational.
 *   - At max level, XP continues to accumulate but is capped at Integer.MAX_VALUE.
 *
 * This class is not thread-safe. All access must occur on the main server thread.
 */
public class SlayerProfile {

    private final UUID playerUuid;

    // All maps are pre-populated for every SlayerType so null checks are unnecessary.
    private final Map<SlayerType, Integer>    levels     = new EnumMap<>(SlayerType.class);
    private final Map<SlayerType, Integer>    xp         = new EnumMap<>(SlayerType.class);
    private final Map<SlayerType, Integer>    killCounts = new EnumMap<>(SlayerType.class);
    private final Map<SlayerType, SlayerTask> activeTasks = new EnumMap<>(SlayerType.class);
    private final Map<SlayerType, Integer>    bossKills  = new EnumMap<>(SlayerType.class);

    public SlayerProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
        for (SlayerType type : SlayerType.values()) {
            levels.put(type, 1);
            xp.put(type, 0);
            killCounts.put(type, 0);
            bossKills.put(type, 0);
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getPlayerUuid()               { return playerUuid; }
    public int getLevel(SlayerType type)      { return levels.getOrDefault(type, 1); }
    public int getXp(SlayerType type)         { return xp.getOrDefault(type, 0); }
    public int getKillCount(SlayerType type)  { return killCounts.getOrDefault(type, 0); }

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

    public SlayerTask getActiveTask(SlayerType type) { return activeTasks.get(type); }

    public void setActiveTask(SlayerType type, SlayerTask task) { activeTasks.put(type, task); }

    public void clearTask(SlayerType type) { activeTasks.remove(type); }

    public Map<SlayerType, SlayerTask> getActiveTasks() { return activeTasks; }

    // -------------------------------------------------------------------------
    // Boss kill tracking (Bestiary)
    // -------------------------------------------------------------------------

    public int getBossKills(SlayerType type) { return bossKills.getOrDefault(type, 0); }

    public void incrementBossKills(SlayerType type) { bossKills.merge(type, 1, Integer::sum); }

    public void setBossKills(SlayerType type, int count) {
        bossKills.put(type, Math.max(0, count));
    }

    public void resetAll() {
        for (SlayerType type : SlayerType.values()) {
            levels.put(type, 1);
            xp.put(type, 0);
            killCounts.put(type, 0);
            bossKills.put(type, 0);
            activeTasks.remove(type);
        }
    }

    // -------------------------------------------------------------------------
    // Progression logic
    // -------------------------------------------------------------------------

    /**
     * Adds XP to the given type and checks for level-ups.
     *
     * XP is never reset on level-up — it remains cumulative so partial progress
     * toward the next level is preserved. At max level, XP accumulates but is
     * capped at Integer.MAX_VALUE to prevent overflow.
     *
     * @return true if at least one level-up occurred, false otherwise.
     */
    public boolean addXp(SlayerType type, int amount) {
        int maxLevel     = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        int currentLevel = getLevel(type);

        if (currentLevel >= maxLevel) {
            // Cap at Integer.MAX_VALUE to prevent overflow.
            long newXp = (long) xp.getOrDefault(type, 0) + amount;
            xp.put(type, (int) Math.min(newXp, Integer.MAX_VALUE));
            return false;
        }

        xp.merge(type, amount, Integer::sum);

        boolean leveledUp = false;
        while (getLevel(type) < maxLevel) {
            int lvl       = getLevel(type);
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
     * Cumulative means total XP earned since level 1, not incremental since the
     * last level-up. For level N, the threshold is:
     *   sum(killThresholds[0..N-1]) * xpPerKill
     */
    private int getXpThresholdForLevel(SlayerType type, int currentLevel) {
        int[] thresholds = type.getKillThresholds();
        int xpPerKill    = type.getXpPerKill();
        int totalKills   = 0;
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
        int maxLevel     = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        int currentLevel = getLevel(type);
        if (currentLevel >= maxLevel) return 0;

        int xpNeeded  = getXpThresholdForLevel(type, currentLevel);
        int remaining = xpNeeded - getXp(type);
        int xpPerKill = type.getXpPerKill();
        if (xpPerKill <= 0) return 0;
        return (int) Math.ceil(Math.max(0, remaining) / (double) xpPerKill);
    }

    /**
     * Returns a 0.0–1.0 float representing progress toward the next level.
     * Returns 1.0 if the player is at max level.
     */
    public float getLevelProgress(SlayerType type) {
        int maxLevel     = SwagSlayer.getInstance().getConfig().getInt("general.max_level", 5);
        int currentLevel = getLevel(type);
        if (currentLevel >= maxLevel) return 1.0f;

        int xpAtLevelStart = (currentLevel == 1) ? 0 : getXpThresholdForLevel(type, currentLevel - 1);
        int xpAtLevelEnd   = getXpThresholdForLevel(type, currentLevel);
        int xpSpan         = xpAtLevelEnd - xpAtLevelStart;

        if (xpSpan <= 0) return 1.0f;
        return Math.min(1.0f, Math.max(0.0f, (getXp(type) - xpAtLevelStart) / (float) xpSpan));
    }
}

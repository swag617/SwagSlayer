package com.swag.swagslayer.models;

import org.bukkit.entity.EntityType;

/**
 * Represents a category of slayable mob.
 *
 * Each constant stores the static defaults loaded from the enum definition.
 * At startup, SlayerManager re-reads these values from config so operators
 * can override them without recompiling. The fields here serve as fallback
 * defaults and as the canonical identifier for each type.
 *
 * Kill thresholds are cumulative: index 0 = kills to reach level 2,
 * index 1 = additional kills to reach level 3, and so on.
 */
public enum SlayerType {

    ZOMBIE(
            "Zombie Slayer",
            EntityType.ZOMBIE,
            new int[]{20, 50, 100, 200, 500},
            10,
            100
    ),
    SPIDER(
            "Spider Slayer",
            EntityType.SPIDER,
            new int[]{20, 50, 100, 200, 500},
            10,
            100
    ),
    SKELETON(
            "Skeleton Slayer",
            EntityType.SKELETON,
            new int[]{20, 50, 100, 200, 500},
            10,
            100
    ),
    CREEPER(
            "Creeper Slayer",
            EntityType.CREEPER,
            new int[]{20, 50, 100, 200, 500},
            10,
            100
    );

    private String displayName;
    private EntityType bossEntityType;
    // killThresholds[i] = kills required to advance FROM level (i+1) TO level (i+2).
    private int[] killThresholds;
    private int xpPerKill;
    private int bossXpReward;

    SlayerType(String displayName, EntityType bossEntityType, int[] killThresholds,
               int xpPerKill, int bossXpReward) {
        this.displayName = displayName;
        this.bossEntityType = bossEntityType;
        this.killThresholds = killThresholds;
        this.xpPerKill = xpPerKill;
        this.bossXpReward = bossXpReward;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getDisplayName() {
        return displayName;
    }

    public EntityType getBossEntityType() {
        return bossEntityType;
    }

    /** Returns a defensive copy so callers cannot mutate the threshold array. */
    public int[] getKillThresholds() {
        return killThresholds.clone();
    }

    public int getXpPerKill() {
        return xpPerKill;
    }

    public int getBossXpReward() {
        return bossXpReward;
    }

    // -------------------------------------------------------------------------
    // Setters — called by SlayerManager when applying config overrides
    // -------------------------------------------------------------------------

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setBossEntityType(EntityType bossEntityType) {
        this.bossEntityType = bossEntityType;
    }

    public void setKillThresholds(int[] killThresholds) {
        this.killThresholds = killThresholds.clone();
    }

    public void setXpPerKill(int xpPerKill) {
        this.xpPerKill = xpPerKill;
    }

    public void setBossXpReward(int bossXpReward) {
        this.bossXpReward = bossXpReward;
    }

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the SlayerType whose canonical mob matches the supplied EntityType,
     * or null if the EntityType is not tracked by any slayer category.
     *
     * The match is based on the enum constant name, so EntityType.ZOMBIE maps to
     * SlayerType.ZOMBIE, etc. This keeps the lookup O(n) but avoids a separate
     * HashMap that could fall out of sync after config reloads change bossEntityType.
     */
    public static SlayerType fromEntityType(EntityType entityType) {
        if (entityType == null) return null;
        for (SlayerType type : values()) {
            if (type.bossEntityType == entityType) {
                return type;
            }
        }
        return null;
    }

    /**
     * Case-insensitive lookup by config key name (e.g. "ZOMBIE", "zombie").
     * Returns null if no match.
     */
    public static SlayerType fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.swag.swagslayer.models;

import org.bukkit.entity.EntityType;

/**
 * Represents a category of slayable mob.
 *
 * Each constant stores defaults loaded from the enum definition.
 * At startup, SlayerManager re-reads these values from config so operators
 * can override them without recompiling.
 *
 * Kill thresholds are incremental: index N = additional kills to advance
 * from level (N+1) to level (N+2). Only indices 0..(max_level-2) are used.
 */
public enum SlayerType {

    // Easiest — common overworld mob, low XP but quick grind.
    ZOMBIE(
            "Zombie Slayer",
            EntityType.ZOMBIE,
            new int[]{15, 40, 80, 150, 300},
            8,
            80
    ),
    // Medium — caves and surface, moderate XP.
    SPIDER(
            "Spider Slayer",
            EntityType.SPIDER,
            new int[]{20, 50, 100, 200, 400},
            10,
            120
    ),
    // Harder — ranged attacker, higher XP reward.
    SKELETON(
            "Skeleton Slayer",
            EntityType.SKELETON,
            new int[]{25, 60, 120, 250, 500},
            12,
            150
    ),
    // Hardest — explosive, highest XP but dangerous grind.
    CREEPER(
            "Creeper Slayer",
            EntityType.CREEPER,
            new int[]{30, 70, 150, 300, 600},
            15,
            200
    );

    private String displayName;
    private EntityType bossEntityType;
    private int[] killThresholds;
    private int xpPerKill;
    private int bossXpReward;

    SlayerType(String displayName, EntityType bossEntityType, int[] killThresholds,
               int xpPerKill, int bossXpReward) {
        this.displayName    = displayName;
        this.bossEntityType = bossEntityType;
        this.killThresholds = killThresholds;
        this.xpPerKill      = xpPerKill;
        this.bossXpReward   = bossXpReward;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getDisplayName()     { return displayName; }
    public EntityType getBossEntityType() { return bossEntityType; }
    public int getXpPerKill()          { return xpPerKill; }
    public int getBossXpReward()       { return bossXpReward; }

    /** Returns a defensive copy so callers cannot mutate the threshold array. */
    public int[] getKillThresholds() { return killThresholds.clone(); }

    // -------------------------------------------------------------------------
    // Setters — called by SlayerManager when applying config overrides
    // -------------------------------------------------------------------------

    public void setDisplayName(String displayName)         { this.displayName    = displayName; }
    public void setBossEntityType(EntityType bossEntityType) { this.bossEntityType = bossEntityType; }
    public void setXpPerKill(int xpPerKill)                { this.xpPerKill      = xpPerKill; }
    public void setBossXpReward(int bossXpReward)          { this.bossXpReward   = bossXpReward; }

    public void setKillThresholds(int[] killThresholds) {
        this.killThresholds = killThresholds.clone();
    }

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the SlayerType whose boss entity type matches the given EntityType,
     * or null if no slayer category tracks this entity type.
     *
     * Comparison uses reference equality on the EntityType enum constant.
     * After config reloads that change bossEntityType, this lookup reflects the
     * updated values automatically because it queries the live field.
     */
    public static SlayerType fromEntityType(EntityType entityType) {
        if (entityType == null) return null;
        for (SlayerType type : values()) {
            if (type.bossEntityType == entityType) return type;
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

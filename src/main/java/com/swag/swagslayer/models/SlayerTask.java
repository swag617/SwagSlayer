package com.swag.swagslayer.models;

/**
 * Represents an active slayer task assigned to a player for one mob type.
 * A task is completed when killsCompleted >= killGoal.
 */
public class SlayerTask {

    private final SlayerType type;
    private final int killGoal;
    private int killsCompleted;

    public SlayerTask(SlayerType type, int killGoal) {
        this.type = type;
        this.killGoal = killGoal;
        this.killsCompleted = 0;
    }

    public SlayerType getType() {
        return type;
    }

    public int getKillGoal() {
        return killGoal;
    }

    public int getKillsCompleted() {
        return killsCompleted;
    }

    public void setKillsCompleted(int amount) {
        this.killsCompleted = Math.max(0, amount);
    }

    public void incrementKills() {
        killsCompleted++;
    }

    public boolean isComplete() {
        return killsCompleted >= killGoal;
    }
}

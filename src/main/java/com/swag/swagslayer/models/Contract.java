package com.swag.swagslayer.models;

public class Contract {

    private final SlayerType type;
    private final int killGoal;
    private final int xpReward;
    private final boolean weekly;
    private final long expiresAt;
    private int killsCompleted;
    private boolean completed;

    public Contract(SlayerType type, int killGoal, int xpReward, boolean weekly, long expiresAt) {
        this.type           = type;
        this.killGoal       = killGoal;
        this.xpReward       = xpReward;
        this.weekly         = weekly;
        this.expiresAt      = expiresAt;
        this.killsCompleted = 0;
        this.completed      = false;
    }

    public SlayerType getType()       { return type; }
    public int getKillGoal()          { return killGoal; }
    public int getXpReward()          { return xpReward; }
    public boolean isWeekly()         { return weekly; }
    public long getExpiresAt()        { return expiresAt; }
    public int getKillsCompleted()    { return killsCompleted; }
    public boolean isCompleted()      { return completed; }

    public void setKillsCompleted(int v) { this.killsCompleted = Math.max(0, v); }
    public void setCompleted(boolean v)  { this.completed = v; }

    public boolean isExpired()  { return System.currentTimeMillis() > expiresAt; }
    public boolean isComplete() { return killsCompleted >= killGoal; }
    public void incrementKills() { killsCompleted++; }

    public String getTimeRemainingString() {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long hours   = remaining / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;
        long seconds = (remaining % 60_000L) / 1_000L;
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
}

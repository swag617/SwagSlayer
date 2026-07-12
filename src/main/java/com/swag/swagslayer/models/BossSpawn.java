package com.swag.swagslayer.models;

import java.util.UUID;

/**
 * Tracks an active boss spawned for a specific player.
 * Expires after 5 minutes regardless of whether it has been defeated.
 */
public class BossSpawn {

    private static final long EXPIRY_MS = 5 * 60 * 1_000L; // 5 minutes

    private final UUID ownerUuid;
    private final SlayerType type;
    private final UUID bossEntityUuid;
    private final long spawnTimeMs;

    public BossSpawn(UUID ownerUuid, SlayerType type, UUID bossEntityUuid) {
        this.ownerUuid      = ownerUuid;
        this.type           = type;
        this.bossEntityUuid = bossEntityUuid;
        this.spawnTimeMs    = System.currentTimeMillis();
    }

    public UUID getOwnerUuid()      { return ownerUuid; }
    public SlayerType getType()     { return type; }
    public UUID getBossEntityUuid() { return bossEntityUuid; }

    public boolean isExpired() {
        return System.currentTimeMillis() > spawnTimeMs + EXPIRY_MS;
    }
}

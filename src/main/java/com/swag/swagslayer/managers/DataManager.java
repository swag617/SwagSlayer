package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.database.DatabaseManager;
import com.swag.swagslayer.models.SlayerProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the in-memory SlayerProfile cache. All persistence is delegated to
 * {@link DatabaseManager}, which stores profiles in SwagAPI's shared database.
 *
 * MIGRATED: this class used to read/write plugins/SwagSlayer/data/&lt;uuid&gt;.yml directly via
 * YamlConfiguration (one file per player, synchronous I/O on the main thread). It is now a pure
 * cache layer over the SQL-backed DatabaseManager — see that class for the schema, the
 * SQLite/MySQL-branched upsert logic, and the one-time legacy-YAML import that runs on first
 * connect() after this migration.
 *
 * Public API is unchanged from the YAML-based version so call sites (SlayerListener, GUIManager,
 * SlayerManager, PerkManager, BossManager, ContractManager, commands) did not need to change.
 */
public class DataManager {

    private final SwagSlayer plugin;
    private final DatabaseManager databaseManager;

    /** In-memory cache. Populated on first access (player join or command lookup). */
    private final Map<UUID, SlayerProfile> profileCache = new HashMap<>();

    public DataManager(SwagSlayer plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /** Exposes the underlying SQL persistence layer for managers that need direct DB access
     *  (ContractManager, LeaderboardManager) without duplicating a second DatabaseManager. */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the profile for the given UUID, loading from the database if not cached.
     * Never returns null — creates a new profile if none exists.
     */
    public SlayerProfile getProfile(UUID uuid) {
        if (profileCache.containsKey(uuid)) {
            return profileCache.get(uuid);
        }
        SlayerProfile profile = loadProfile(uuid);
        profileCache.put(uuid, profile);
        return profile;
    }

    /**
     * Loads the profile from the database. If no rows exist, returns a fresh profile.
     * The returned profile is NOT automatically added to the cache — callers should use
     * getProfile() for normal access.
     */
    public SlayerProfile loadProfile(UUID uuid) {
        return databaseManager.loadProfile(uuid);
    }

    /**
     * Persists the given profile to the database, upserting all SlayerType rows.
     */
    public void saveProfile(SlayerProfile profile) {
        databaseManager.saveProfile(profile);
    }

    /**
     * Saves every cached profile. Called on plugin disable to flush in-memory state.
     */
    public void saveAll() {
        for (SlayerProfile profile : profileCache.values()) {
            saveProfile(profile);
        }
        plugin.getLogger().info("Saved " + profileCache.size() + " slayer profile(s).");
    }

    /**
     * Removes the profile from cache. Should be called on PlayerQuitEvent after
     * saving, to prevent unbounded memory growth on busy servers.
     */
    public void unloadProfile(UUID uuid) {
        profileCache.remove(uuid);
    }

    /**
     * Removes the profile from cache AND deletes its rows from the database.
     * Used by /slayadmin reset.
     */
    public void deleteProfile(UUID uuid) {
        profileCache.remove(uuid);
        databaseManager.deleteProfile(uuid);
    }
}

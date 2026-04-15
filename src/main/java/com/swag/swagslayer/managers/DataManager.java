package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerTask;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages flat-file YAML persistence for player slayer profiles.
 *
 * Storage layout:
 *   plugins/SwagSlayer/data/<uuid>.yml
 *
 * YAML structure per file:
 *   uuid: <string>
 *   slayer:
 *     ZOMBIE:
 *       level: 1
 *       xp: 0
 *       kills: 0
 *     SPIDER:
 *       ...
 *
 * All I/O is synchronous on the main thread. This is acceptable for the
 * initial implementation and will be migrated to async when SQLite is added.
 */
public class DataManager {

    private final SwagSlayer plugin;
    private final File dataFolder;

    /** In-memory cache. Populated on first access (player join or command lookup). */
    private final Map<UUID, SlayerProfile> profileCache = new HashMap<>();

    public DataManager(SwagSlayer plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data directory: " + dataFolder.getAbsolutePath());
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the profile for the given UUID, loading from disk if not cached.
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
     * Loads the profile from disk. If no file exists, returns a fresh profile.
     * The returned profile is NOT automatically added to the cache — callers
     * should use getProfile() for normal access.
     */
    public SlayerProfile loadProfile(UUID uuid) {
        File file = profileFile(uuid);
        if (!file.exists()) {
            return new SlayerProfile(uuid);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        SlayerProfile profile = new SlayerProfile(uuid);

        if (yaml.isConfigurationSection("slayer")) {
            for (SlayerType type : SlayerType.values()) {
                String path = "slayer." + type.name() + ".";
                profile.setLevel(type, yaml.getInt(path + "level", 1));
                profile.setXp(type, yaml.getInt(path + "xp", 0));
                profile.setKillCount(type, yaml.getInt(path + "kills", 0));

                int taskGoal = yaml.getInt(path + "task.goal", -1);
                if (taskGoal > 0) {
                    SlayerTask task = new SlayerTask(type, taskGoal);
                    task.setKillsCompleted(yaml.getInt(path + "task.completed", 0));
                    profile.setActiveTask(type, task);
                }
            }
        }

        return profile;
    }

    /**
     * Persists the given profile to disk, overwriting any existing file.
     */
    public void saveProfile(SlayerProfile profile) {
        File file = profileFile(profile.getPlayerUuid());
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("uuid", profile.getPlayerUuid().toString());

        for (SlayerType type : SlayerType.values()) {
            String path = "slayer." + type.name() + ".";
            yaml.set(path + "level", profile.getLevel(type));
            yaml.set(path + "xp", profile.getXp(type));
            yaml.set(path + "kills", profile.getKillCount(type));

            SlayerTask task = profile.getActiveTask(type);
            if (task != null) {
                yaml.set(path + "task.goal", task.getKillGoal());
                yaml.set(path + "task.completed", task.getKillsCompleted());
            } else {
                yaml.set(path + "task", null); // clear any previously saved task
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save slayer profile for " + profile.getPlayerUuid(), e);
        }
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
     * Removes the profile from cache AND deletes the data file from disk.
     * Used by /slayadmin reset.
     */
    public void deleteProfile(UUID uuid) {
        profileCache.remove(uuid);
        File file = profileFile(uuid);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete profile file: " + file.getAbsolutePath());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private File profileFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}

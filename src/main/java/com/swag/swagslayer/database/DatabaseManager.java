package com.swag.swagslayer.database;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.Contract;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerTask;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQL persistence layer for SwagSlayer, backed by SwagAPI's shared HikariCP connection pool.
 *
 * Shape mirrors com.swag.swagjobs.database.DatabaseManager (the reference migration in this
 * ecosystem): connections are borrowed per call via {@code dbService.getConnection()} and
 * returned to the pool via try-with-resources, table creation is idempotent
 * (CREATE TABLE IF NOT EXISTS), and a one-time legacy-file import runs on first {@link #connect()}
 * to pull in pre-SwagAPI YAML data without deleting the originals.
 *
 * Two tables:
 *   slayer_profiles  — one row per (uuid, SlayerType); replaces data/&lt;uuid&gt;.yml
 *   slayer_contracts — one row per (uuid, "daily"|"weekly"); replaces contracts/&lt;uuid&gt;.yml
 *
 * All statements are dialect-branched via dbService.isMySQL()/isSQLite() where upsert syntax
 * differs (SQLite: INSERT ... ON CONFLICT ... DO UPDATE; MySQL: INSERT ... ON DUPLICATE KEY UPDATE).
 */
public class DatabaseManager {

    private final SwagSlayer plugin;
    private final com.SwagDev.SwagAPI.api.IDatabaseService dbService;
    private final Object dbLock = new Object();

    public DatabaseManager(SwagSlayer plugin, com.SwagDev.SwagAPI.api.IDatabaseService dbService) {
        this.plugin = plugin;
        this.dbService = dbService;
    }

    /**
     * Creates tables (if missing) and runs the one-time legacy YAML import. Safe to call
     * exactly once during onEnable(), after SwagAPI's IDatabaseService has been hooked.
     */
    public void connect() {
        try {
            createTables();
            plugin.getLogger().info("Successfully connected to SwagAPI shared database.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create SwagSlayer tables on shared database!", e);
            return;
        }

        // One-time import of pre-SwagAPI per-player YAML files, if the plugin folder still has them.
        importLegacyDataIfPresent();
    }

    private Connection getConnection() throws SQLException {
        return dbService.getConnection();
    }

    private void createTables() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE IF NOT EXISTS slayer_profiles (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "type VARCHAR(16) NOT NULL," +
                    "level INTEGER NOT NULL DEFAULT 1," +
                    "xp INTEGER NOT NULL DEFAULT 0," +
                    "kills INTEGER NOT NULL DEFAULT 0," +
                    "boss_kills INTEGER NOT NULL DEFAULT 0," +
                    "task_goal INTEGER NOT NULL DEFAULT -1," +
                    "task_completed INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (uuid, type))");

            statement.execute("CREATE TABLE IF NOT EXISTS slayer_contracts (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "contract_key VARCHAR(16) NOT NULL," +
                    "type VARCHAR(16) NOT NULL," +
                    "goal INTEGER NOT NULL," +
                    "xp_reward INTEGER NOT NULL," +
                    "weekly BOOLEAN NOT NULL DEFAULT 0," +
                    "expires_at BIGINT NOT NULL," +
                    "kills_completed INTEGER NOT NULL DEFAULT 0," +
                    "completed BOOLEAN NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (uuid, contract_key))");
        }
    }

    // -------------------------------------------------------------------------
    // Profile CRUD
    // -------------------------------------------------------------------------

    /** Loads a profile from the database. Returns a fresh, unsaved profile if no rows exist. */
    public SlayerProfile loadProfile(UUID uuid) {
        synchronized (dbLock) {
            SlayerProfile profile = new SlayerProfile(uuid);
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT type, level, xp, kills, boss_kills, task_goal, task_completed " +
                                 "FROM slayer_profiles WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SlayerType type = SlayerType.fromName(rs.getString("type"));
                        if (type == null) continue;

                        profile.setLevel(type, rs.getInt("level"));
                        profile.setXp(type, rs.getInt("xp"));
                        profile.setKillCount(type, rs.getInt("kills"));
                        profile.setBossKills(type, rs.getInt("boss_kills"));

                        int taskGoal = rs.getInt("task_goal");
                        if (taskGoal > 0) {
                            SlayerTask task = new SlayerTask(type, taskGoal);
                            task.setKillsCompleted(rs.getInt("task_completed"));
                            profile.setActiveTask(type, task);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load slayer profile for " + uuid, e);
            }
            return profile;
        }
    }

    /** Upserts all four SlayerType rows for the given profile in a single transaction. */
    public void saveProfile(SlayerProfile profile) {
        synchronized (dbLock) {
            String sql = upsertSql(dbService.isMySQL(),
                    "slayer_profiles",
                    "uuid, type, level, xp, kills, boss_kills, task_goal, task_completed",
                    "?, ?, ?, ?, ?, ?, ?, ?",
                    "uuid, type",
                    new String[] {"level", "xp", "kills", "boss_kills", "task_goal", "task_completed"});

            Connection connection = null;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    for (SlayerType type : SlayerType.values()) {
                        SlayerTask task = profile.getActiveTask(type);
                        ps.setString(1, profile.getPlayerUuid().toString());
                        ps.setString(2, type.name());
                        ps.setInt(3, profile.getLevel(type));
                        ps.setInt(4, profile.getXp(type));
                        ps.setInt(5, profile.getKillCount(type));
                        ps.setInt(6, profile.getBossKills(type));
                        ps.setInt(7, task != null ? task.getKillGoal() : -1);
                        ps.setInt(8, task != null ? task.getKillsCompleted() : 0);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to rollback profile save", ex);
                    }
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ignored) {
                        // nothing more we can do
                    }
                }
                plugin.getLogger().log(Level.SEVERE, "Failed to save slayer profile for " + profile.getPlayerUuid(), e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {
                        // return-to-pool failure — nothing more we can do
                    }
                }
            }
        }
    }

    public void deleteProfile(UUID uuid) {
        synchronized (dbLock) {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement("DELETE FROM slayer_profiles WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete slayer profile for " + uuid, e);
            }
        }
    }

    /** One row of raw XP data, used by LeaderboardManager to build ranked boards. */
    public static class XpRow {
        public final UUID uuid;
        public final SlayerType type;
        public final int xp;

        public XpRow(UUID uuid, SlayerType type, int xp) {
            this.uuid = uuid;
            this.type = type;
            this.xp = xp;
        }
    }

    /**
     * Loads every (uuid, type, xp) row across all players. Intended to be called from an
     * async thread (see LeaderboardManager) — JDBC calls against the pooled HikariCP
     * connection are safe off the main thread, unlike direct Bukkit API access.
     */
    public List<XpRow> loadAllXp() {
        synchronized (dbLock) {
            List<XpRow> rows = new ArrayList<>();
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT uuid, type, xp FROM slayer_profiles");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(rs.getString("uuid"));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    SlayerType type = SlayerType.fromName(rs.getString("type"));
                    if (type == null) continue;
                    rows.add(new XpRow(uuid, type, rs.getInt("xp")));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load leaderboard data", e);
            }
            return rows;
        }
    }

    // -------------------------------------------------------------------------
    // Contract CRUD
    // -------------------------------------------------------------------------

    /** Loads the "daily" and "weekly" contract rows for a player, keyed by contract_key. */
    public Map<String, Contract> loadContracts(UUID uuid) {
        synchronized (dbLock) {
            Map<String, Contract> result = new HashMap<>();
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT contract_key, type, goal, xp_reward, weekly, expires_at, kills_completed, completed " +
                                 "FROM slayer_contracts WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SlayerType type = SlayerType.fromName(rs.getString("type"));
                        if (type == null) continue;
                        Contract c = new Contract(
                                type,
                                rs.getInt("goal"),
                                rs.getInt("xp_reward"),
                                rs.getBoolean("weekly"),
                                rs.getLong("expires_at"));
                        c.setKillsCompleted(rs.getInt("kills_completed"));
                        c.setCompleted(rs.getBoolean("completed"));
                        result.put(rs.getString("contract_key"), c);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load contracts for " + uuid, e);
            }
            return result;
        }
    }

    /**
     * Upserts whichever of daily/weekly is non-null. Unlike the legacy YAML implementation
     * (which rewrote the whole file every save and so silently wiped out a category that
     * hadn't been touched yet in the current session — e.g. a player who joined and quit
     * without a tracked kill), a null contract here simply leaves its existing row alone
     * instead of deleting previously-saved progress.
     */
    public void saveContracts(UUID uuid, Contract daily, Contract weekly) {
        synchronized (dbLock) {
            if (daily != null) upsertContract(uuid, "daily", daily);
            if (weekly != null) upsertContract(uuid, "weekly", weekly);
        }
    }

    private void upsertContract(UUID uuid, String key, Contract c) {
        String sql = upsertSql(dbService.isMySQL(),
                "slayer_contracts",
                "uuid, contract_key, type, goal, xp_reward, weekly, expires_at, kills_completed, completed",
                "?, ?, ?, ?, ?, ?, ?, ?, ?",
                "uuid, contract_key",
                new String[] {"type", "goal", "xp_reward", "weekly", "expires_at", "kills_completed", "completed"});

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.setString(3, c.getType().name());
            ps.setInt(4, c.getKillGoal());
            ps.setInt(5, c.getXpReward());
            ps.setBoolean(6, c.isWeekly());
            ps.setLong(7, c.getExpiresAt());
            ps.setInt(8, c.getKillsCompleted());
            ps.setBoolean(9, c.isCompleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + key + " contract for " + uuid, e);
        }
    }

    // -------------------------------------------------------------------------
    // Upsert helper — branches SQLite "ON CONFLICT ... DO UPDATE" vs MySQL "ON DUPLICATE KEY UPDATE"
    // -------------------------------------------------------------------------

    private String upsertSql(boolean mysql, String table, String columns, String placeholders,
                              String conflictColumns, String[] updateColumns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table)
                .append(" (").append(columns).append(") VALUES (").append(placeholders).append(") ");
        if (mysql) {
            sql.append("ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < updateColumns.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(updateColumns[i]).append(" = VALUES(").append(updateColumns[i]).append(")");
            }
        } else {
            sql.append("ON CONFLICT(").append(conflictColumns).append(") DO UPDATE SET ");
            for (int i = 0; i < updateColumns.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(updateColumns[i]).append(" = excluded.").append(updateColumns[i]);
            }
        }
        return sql.toString();
    }

    // -------------------------------------------------------------------------
    // Legacy YAML migration (one-time, first boot after this change)
    // -------------------------------------------------------------------------

    /**
     * Drag-and-drop-free migration: on first connect() after upgrading to the SwagAPI-backed
     * database, imports every plugins/SwagSlayer/data/&lt;uuid&gt;.yml and
     * plugins/SwagSlayer/contracts/&lt;uuid&gt;.yml file into the shared database, then renames
     * each source folder to "&lt;name&gt;.imported" so it is never re-scanned on a later restart.
     *
     * Existing rows in the shared database are never overwritten (INSERT OR IGNORE / INSERT
     * IGNORE), so this is safe to re-run against a database that already has data.
     */
    private void importLegacyDataIfPresent() {
        importLegacyProfiles();
        importLegacyContracts();
    }

    private void importLegacyProfiles() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.isDirectory()) return;

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        plugin.getLogger().info("Found legacy 'data/' folder with " + files.length +
                " file(s) — importing pre-SwagAPI slayer profiles into the shared database...");

        String insertSql = (dbService.isMySQL() ? "INSERT IGNORE INTO " : "INSERT OR IGNORE INTO ") +
                "slayer_profiles (uuid, type, level, xp, kills, boss_kills, task_goal, task_completed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int imported = 0;
        boolean ok = true;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSql)) {
            for (File file : files) {
                String name = file.getName();
                String uuidStr = name.substring(0, name.length() - 4);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue; // not a player data file
                }

                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                if (!yaml.isConfigurationSection("slayer")) continue;

                for (SlayerType type : SlayerType.values()) {
                    String path = "slayer." + type.name() + ".";
                    ps.setString(1, uuid.toString());
                    ps.setString(2, type.name());
                    ps.setInt(3, yaml.getInt(path + "level", 1));
                    ps.setInt(4, yaml.getInt(path + "xp", 0));
                    ps.setInt(5, yaml.getInt(path + "kills", 0));
                    ps.setInt(6, yaml.getInt(path + "boss_kills", 0));
                    ps.setInt(7, yaml.getInt(path + "task.goal", -1));
                    ps.setInt(8, yaml.getInt(path + "task.completed", 0));
                    imported += ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            ok = false;
            plugin.getLogger().log(Level.SEVERE, "Failed to import legacy slayer profiles. " +
                    "The 'data/' folder has been left in place so you can retry after fixing the issue.", e);
        }

        plugin.getLogger().info("Legacy profile import complete: " + imported + " row(s) imported.");

        if (ok) {
            renameFolder(dataFolder, "data.imported");
        }
    }

    private void importLegacyContracts() {
        File contractsFolder = new File(plugin.getDataFolder(), "contracts");
        if (!contractsFolder.isDirectory()) return;

        File[] files = contractsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        plugin.getLogger().info("Found legacy 'contracts/' folder with " + files.length +
                " file(s) — importing pre-SwagAPI slayer contracts into the shared database...");

        String insertSql = (dbService.isMySQL() ? "INSERT IGNORE INTO " : "INSERT OR IGNORE INTO ") +
                "slayer_contracts (uuid, contract_key, type, goal, xp_reward, weekly, expires_at, kills_completed, completed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int imported = 0;
        boolean ok = true;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSql)) {
            for (File file : files) {
                String name = file.getName();
                String uuidStr = name.substring(0, name.length() - 4);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                for (String key : new String[] {"daily", "weekly"}) {
                    if (!yaml.isConfigurationSection(key)) continue;
                    String typeName = yaml.getString(key + ".type", "");
                    SlayerType type = SlayerType.fromName(typeName);
                    if (type == null) continue;

                    ps.setString(1, uuid.toString());
                    ps.setString(2, key);
                    ps.setString(3, type.name());
                    ps.setInt(4, yaml.getInt(key + ".goal"));
                    ps.setInt(5, yaml.getInt(key + ".xp_reward"));
                    ps.setBoolean(6, yaml.getBoolean(key + ".weekly"));
                    ps.setLong(7, yaml.getLong(key + ".expires_at"));
                    ps.setInt(8, yaml.getInt(key + ".kills_completed", 0));
                    ps.setBoolean(9, yaml.getBoolean(key + ".completed", false));
                    imported += ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            ok = false;
            plugin.getLogger().log(Level.SEVERE, "Failed to import legacy slayer contracts. " +
                    "The 'contracts/' folder has been left in place so you can retry after fixing the issue.", e);
        }

        plugin.getLogger().info("Legacy contract import complete: " + imported + " row(s) imported.");

        if (ok) {
            renameFolder(contractsFolder, "contracts.imported");
        }
    }

    private void renameFolder(File folder, String newName) {
        File renamed = new File(folder.getParentFile(), newName);
        if (folder.renameTo(renamed)) {
            plugin.getLogger().info("Renamed '" + folder.getName() + "' to '" + renamed.getName()
                    + "' so it won't be imported again.");
        } else {
            plugin.getLogger().warning("Import of '" + folder.getName() + "' succeeded but the folder could not be "
                    + "renamed. Please rename or remove it manually to prevent re-importing on next restart.");
        }
    }
}

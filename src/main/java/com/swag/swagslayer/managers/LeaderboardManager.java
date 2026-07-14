package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.database.DatabaseManager;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

/**
 * Builds and caches top-10 leaderboards per SlayerType and an overall board.
 *
 * Refresh strategy:
 *   - Queries DatabaseManager.loadAllXp() on an async thread. JDBC calls against SwagAPI's
 *     pooled HikariCP connection are safe off the main thread, unlike direct Bukkit API access.
 *   - Once sorted, hands the resulting lists back to the main thread via runTask
 *     so that reads from the GUI (main thread) never race with the async write.
 *
 * The cached lists are replaced atomically (reference swap) so a GUI reading
 * a list mid-render always sees a consistent snapshot even if a refresh fires
 * concurrently.
 *
 * MIGRATED: this used to scan every .yml file in plugins/SwagSlayer/data/ directly, bypassing
 * DataManager's cache entirely. It now reads the same slayer_profiles table DataManager writes
 * to, via DatabaseManager.loadAllXp().
 */
public class LeaderboardManager {

    private static final int TOP_SIZE = 10;

    /**
     * Represents a single ranked entry on a leaderboard.
     */
    public static class LeaderboardEntry {
        private final UUID uuid;
        private final String playerName;
        private final SlayerType slayerType; // null = overall entry
        private final int xpValue;

        public LeaderboardEntry(UUID uuid, String playerName, SlayerType slayerType, int xpValue) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.slayerType = slayerType;
            this.xpValue = xpValue;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        /** Null for overall leaderboard entries. */
        public SlayerType getSlayerType() {
            return slayerType;
        }

        public int getXpValue() {
            return xpValue;
        }
    }

    // -------------------------------------------------------------------------

    private final SwagSlayer plugin;
    private final DatabaseManager databaseManager;

    /**
     * Cached boards. Both reads and writes happen exclusively on the main server
     * thread (reads via GUI events, writes via runTask callback after async scan),
     * so no concurrent access or volatile semantics are needed.
     */
    private List<LeaderboardEntry> overallBoard = Collections.emptyList();
    private final Map<SlayerType, List<LeaderboardEntry>> typeBoards = new HashMap<>();

    public LeaderboardManager(SwagSlayer plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;

        // Initialise type boards to empty so getTopForType never returns null.
        for (SlayerType type : SlayerType.values()) {
            typeBoards.put(type, Collections.emptyList());
        }

        refreshLeaderboards();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a copy of the top {@code count} entries for the given SlayerType.
     * Safe to call from the main thread at any time.
     */
    public List<LeaderboardEntry> getTopForType(SlayerType type, int count) {
        List<LeaderboardEntry> board = typeBoards.getOrDefault(type, Collections.emptyList());
        return board.subList(0, Math.min(count, board.size()));
    }

    /**
     * Returns a copy of the top {@code count} overall entries (sum of all type XP).
     * Safe to call from the main thread at any time.
     */
    public List<LeaderboardEntry> getTopOverall(int count) {
        List<LeaderboardEntry> board = overallBoard;
        return board.subList(0, Math.min(count, board.size()));
    }

    /**
     * Asynchronously scans the data directory and rebuilds all leaderboards.
     * The cached lists are replaced on the main thread once scanning is done.
     * Called on enable and after /slayadmin reload.
     */
    public void refreshLeaderboards() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // ---- async: query slayer_profiles via the shared HikariCP pool ----
            List<DatabaseManager.XpRow> rows = databaseManager.loadAllXp();
            if (rows.isEmpty()) {
                // Nothing to rank — clear boards on main thread.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    overallBoard = Collections.emptyList();
                    for (SlayerType type : SlayerType.values()) {
                        typeBoards.put(type, Collections.emptyList());
                    }
                });
                return;
            }

            // Map from UUID -> XP per type, plus overall sum.
            // We accumulate raw data first, then resolve names and sort after.
            Map<UUID, Map<SlayerType, Integer>> rawData = new LinkedHashMap<>();
            for (DatabaseManager.XpRow row : rows) {
                rawData.computeIfAbsent(row.uuid, k -> new EnumMap<>(SlayerType.class)).put(row.type, row.xp);
            }

            // Build sorted per-type lists (top TOP_SIZE).
            Map<SlayerType, List<LeaderboardEntry>> newTypeBoards = new EnumMap<>(SlayerType.class);
            for (SlayerType type : SlayerType.values()) {
                List<LeaderboardEntry> entries = new ArrayList<>();
                for (Map.Entry<UUID, Map<SlayerType, Integer>> playerEntry : rawData.entrySet()) {
                    UUID uuid = playerEntry.getKey();
                    int xp = playerEntry.getValue().getOrDefault(type, 0);
                    if (xp <= 0) continue;
                    String name = resolveName(uuid);
                    entries.add(new LeaderboardEntry(uuid, name, type, xp));
                }
                entries.sort((a, b) -> Integer.compare(b.getXpValue(), a.getXpValue()));
                if (entries.size() > TOP_SIZE) {
                    entries = entries.subList(0, TOP_SIZE);
                }
                newTypeBoards.put(type, Collections.unmodifiableList(entries));
            }

            // Build sorted overall list (sum of all type XP).
            List<LeaderboardEntry> newOverall = new ArrayList<>();
            for (Map.Entry<UUID, Map<SlayerType, Integer>> playerEntry : rawData.entrySet()) {
                UUID uuid = playerEntry.getKey();
                int totalXp = 0;
                for (int xp : playerEntry.getValue().values()) {
                    totalXp += xp;
                }
                if (totalXp <= 0) continue;
                String name = resolveName(uuid);
                newOverall.add(new LeaderboardEntry(uuid, name, null, totalXp));
            }
            newOverall.sort((a, b) -> Integer.compare(b.getXpValue(), a.getXpValue()));
            if (newOverall.size() > TOP_SIZE) {
                newOverall = newOverall.subList(0, TOP_SIZE);
            }
            final List<LeaderboardEntry> finalOverall = Collections.unmodifiableList(newOverall);

            // ---- sync: swap cached references on main thread ----
            Bukkit.getScheduler().runTask(plugin, () -> {
                overallBoard = finalOverall;
                typeBoards.putAll(newTypeBoards);
                plugin.getLogger().info("Leaderboards refreshed. Overall entries: " + finalOverall.size());
            });
        });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a display name for a UUID. Falls back to a shortened UUID string
     * if the player has never joined this server (getName() returns null).
     *
     * Called from the async scan thread. Bukkit.getOfflinePlayer(UUID) performs
     * a user-cache file lookup and does not touch Bukkit's live player list,
     * making it safe in practice on Paper 1.21.1, though not formally documented
     * as async-safe.
     */
    private String resolveName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        if (name == null || name.isEmpty()) {
            // Shorten to first segment of UUID as a readable fallback.
            name = uuid.toString().split("-")[0];
        }
        return name;
    }
}

package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.Contract;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages daily and weekly slayer contracts per player.
 *
 * Daily contracts: 30–60 kills, 500 XP reward, resets at UTC midnight.
 * Weekly contracts: 200–400 kills, 3000 XP reward, resets Monday UTC midnight.
 *
 * Contracts are persisted in plugins/SwagSlayer/contracts/<uuid>.yml.
 *
 * Kill routing always calls getDailyContract/getWeeklyContract (which
 * auto-refresh expired contracts) rather than reading the raw cache directly.
 * This ensures that kills are never silently dropped when a contract expires
 * mid-session without the player opening the contracts GUI.
 *
 * XP rewards are awarded via SlayerManager.awardXp() so level-up feedback
 * is never silently dropped.
 */
public class ContractManager {

    private final SwagSlayer plugin;
    private final DataManager dataManager;
    private final File contractsFolder;
    private final Random random = new Random();

    private SlayerManager slayerManager; // setter-injected

    private final Map<UUID, Contract> dailyContracts  = new HashMap<>();
    private final Map<UUID, Contract> weeklyContracts = new HashMap<>();

    public ContractManager(SwagSlayer plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.contractsFolder = new File(plugin.getDataFolder(), "contracts");
        if (!contractsFolder.exists() && !contractsFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create contracts directory.");
        }
    }

    public void setSlayerManager(SlayerManager slayerManager) {
        this.slayerManager = slayerManager;
    }

    // -------------------------------------------------------------------------
    // Contract access — always auto-refreshes expired contracts
    // -------------------------------------------------------------------------

    public Contract getDailyContract(UUID uuid) {
        Contract existing = dailyContracts.get(uuid);
        if (existing != null && !existing.isExpired()) return existing;
        Contract fresh = new Contract(randomType(), 30 + random.nextInt(31),
                500, false, nextMidnightUTC());
        dailyContracts.put(uuid, fresh);
        return fresh;
    }

    public Contract getWeeklyContract(UUID uuid) {
        Contract existing = weeklyContracts.get(uuid);
        if (existing != null && !existing.isExpired()) return existing;
        Contract fresh = new Contract(randomType(), 200 + random.nextInt(201),
                3000, true, nextMondayMidnightUTC());
        weeklyContracts.put(uuid, fresh);
        return fresh;
    }

    // -------------------------------------------------------------------------
    // Kill routing
    // -------------------------------------------------------------------------

    /**
     * Routes a kill to both contracts. Uses getDailyContract/getWeeklyContract
     * (not raw map access) so expired contracts are replaced before ticking —
     * kills are never silently dropped when a contract resets mid-session.
     */
    public void handleKill(Player player, SlayerType type) {
        UUID uuid = player.getUniqueId();
        tickContract(player, getDailyContract(uuid), type);
        tickContract(player, getWeeklyContract(uuid), type);
    }

    private void tickContract(Player player, Contract contract, SlayerType type) {
        if (contract == null || contract.isExpired() || contract.isCompleted()) return;
        if (contract.getType() != type) return;

        contract.incrementKills();

        if (contract.isComplete()) {
            contract.setCompleted(true);
            String label = contract.isWeekly() ? "Weekly" : "Daily";
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + label
                    + " Contract Complete! " + ChatColor.RESET + ChatColor.YELLOW
                    + "Earned " + ChatColor.GOLD + contract.getXpReward()
                    + " XP" + ChatColor.YELLOW + "!");

            // Award via SlayerManager so level-up messages and perk notifications fire.
            if (slayerManager != null) {
                slayerManager.awardXp(player, type, contract.getXpReward());
            } else {
                // Fallback if slayerManager is not yet wired (should not happen at runtime).
                dataManager.getProfile(player.getUniqueId()).addXp(type, contract.getXpReward());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void onPlayerJoin(UUID uuid) {
        loadContracts(uuid);
    }

    public void onPlayerQuit(UUID uuid) {
        saveContracts(uuid);
        dailyContracts.remove(uuid);
        weeklyContracts.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private void saveContracts(UUID uuid) {
        File file = new File(contractsFolder, uuid + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        writeContract(yaml, "daily",  dailyContracts.get(uuid));
        writeContract(yaml, "weekly", weeklyContracts.get(uuid));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save contracts for " + uuid, e);
        }
    }

    private void loadContracts(UUID uuid) {
        File file = new File(contractsFolder, uuid + ".yml");
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Contract daily = readContract(yaml, "daily");
        if (daily != null && !daily.isExpired()) dailyContracts.put(uuid, daily);
        Contract weekly = readContract(yaml, "weekly");
        if (weekly != null && !weekly.isExpired()) weeklyContracts.put(uuid, weekly);
    }

    private void writeContract(YamlConfiguration yaml, String key, Contract c) {
        if (c == null) return;
        yaml.set(key + ".type",            c.getType().name());
        yaml.set(key + ".goal",            c.getKillGoal());
        yaml.set(key + ".xp_reward",       c.getXpReward());
        yaml.set(key + ".weekly",          c.isWeekly());
        yaml.set(key + ".expires_at",      c.getExpiresAt());
        yaml.set(key + ".kills_completed", c.getKillsCompleted());
        yaml.set(key + ".completed",       c.isCompleted());
    }

    private Contract readContract(YamlConfiguration yaml, String key) {
        if (!yaml.isConfigurationSection(key)) return null;
        String typeName = yaml.getString(key + ".type", "");
        SlayerType type = SlayerType.fromName(typeName);
        if (type == null) return null;
        Contract c = new Contract(
                type,
                yaml.getInt(key + ".goal"),
                yaml.getInt(key + ".xp_reward"),
                yaml.getBoolean(key + ".weekly"),
                yaml.getLong(key + ".expires_at")
        );
        c.setKillsCompleted(yaml.getInt(key + ".kills_completed", 0));
        c.setCompleted(yaml.getBoolean(key + ".completed", false));
        return c;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SlayerType randomType() {
        SlayerType[] types = SlayerType.values();
        return types[random.nextInt(types.length)];
    }

    private long nextMidnightUTC() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS)
                .plusDays(1)
                .toInstant().toEpochMilli();
    }

    private long nextMondayMidnightUTC() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
        int daysUntil = (8 - now.getDayOfWeek().getValue()) % 7;
        if (daysUntil == 0) daysUntil = 7;
        return now.plusDays(daysUntil).toInstant().toEpochMilli();
    }
}

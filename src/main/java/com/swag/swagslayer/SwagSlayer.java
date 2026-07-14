package com.swag.swagslayer;

import com.swag.swagslayer.commands.SlayAdminCommand;
import com.swag.swagslayer.commands.SlayerCommand;
import com.swag.swagslayer.database.DatabaseManager;
import com.swag.swagslayer.listeners.GUIListener;
import com.swag.swagslayer.listeners.PerkListener;
import com.swag.swagslayer.listeners.SlayerListener;
import com.swag.swagslayer.managers.BestiaryManager;
import com.swag.swagslayer.managers.BossManager;
import com.swag.swagslayer.managers.ContractManager;
import com.swag.swagslayer.managers.CosmeticsManager;
import com.swag.swagslayer.managers.DataManager;
import com.swag.swagslayer.managers.GUIManager;
import com.swag.swagslayer.managers.LeaderboardManager;
import com.swag.swagslayer.managers.PerkManager;
import com.swag.swagslayer.managers.SlayerManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for SwagSlayer.
 *
 * Initialization order:
 *   1. saveDefaultConfig()
 *   2. hookSwagAPI() — hard dependency; disables the plugin if SwagAPI's IDatabaseService
 *      isn't registered (see hookSwagAPI() below)
 *   3. DatabaseManager (connects to SwagAPI's shared pool, creates tables, runs the
 *      one-time legacy-YAML import)
 *   4. DataManager (in-memory profile cache over DatabaseManager)
 *   5. SlayerManager (reads config into SlayerType constants)
 *   6. LeaderboardManager
 *   7. CosmeticsManager
 *   8. PerkManager
 *   9. BossManager
 *  10. BestiaryManager
 *  11. ContractManager
 *  12. GUIManager + setter injections
 *  13. Listeners
 *  14. Commands
 *
 * MIGRATED: SwagSlayer used to own its persistence entirely (flat YAML files under
 * plugins/SwagSlayer/data/ and plugins/SwagSlayer/contracts/). It now stores all player data
 * in SwagAPI's shared database via DatabaseManager — see that class for the schema and the
 * one-time import of any pre-existing YAML files.
 */
public class SwagSlayer extends JavaPlugin {

    private static SwagSlayer instance;

    // SwagAPI service references.
    private com.SwagDev.SwagAPI.api.IDatabaseService dbService;
    private com.SwagDev.SwagAPI.api.IEventBusService busService;

    private DatabaseManager databaseManager;
    private DataManager dataManager;
    private SlayerManager slayerManager;
    private LeaderboardManager leaderboardManager;
    private CosmeticsManager cosmeticsManager;
    private PerkManager perkManager;
    private BossManager bossManager;
    private BestiaryManager bestiaryManager;
    private ContractManager contractManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // ── Step 1: Hook SwagAPI (must be first — hard dependency) ─────────────
        if (!hookSwagAPI()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Core managers.
        databaseManager     = new DatabaseManager(this, dbService);
        databaseManager.connect();

        dataManager         = new DataManager(this, databaseManager);
        slayerManager       = new SlayerManager(this, dataManager);
        leaderboardManager  = new LeaderboardManager(this, databaseManager);
        cosmeticsManager    = new CosmeticsManager();
        perkManager         = new PerkManager(dataManager);
        bossManager         = new BossManager(this, dataManager);
        bestiaryManager     = new BestiaryManager();
        contractManager     = new ContractManager(this, dataManager, databaseManager);

        // Wire optional dependencies via setters.
        slayerManager.setCosmeticsManager(cosmeticsManager);
        slayerManager.setPerkManager(perkManager);
        bossManager.setCosmeticsManager(cosmeticsManager);
        bossManager.setSlayerManager(slayerManager);
        contractManager.setSlayerManager(slayerManager);

        // GUI — constructed last so all managers are available for setters.
        guiManager = new GUIManager(this, dataManager, leaderboardManager);
        guiManager.setSlayerManager(slayerManager);
        guiManager.setBossManager(bossManager);
        guiManager.setBestiaryManager(bestiaryManager);
        guiManager.setContractManager(contractManager);
        guiManager.setPerkManager(perkManager);

        // Listeners.
        getServer().getPluginManager().registerEvents(
                new SlayerListener(this, slayerManager, dataManager, bossManager, contractManager), this);
        getServer().getPluginManager().registerEvents(
                new GUIListener(guiManager, slayerManager, bossManager), this);
        getServer().getPluginManager().registerEvents(
                new PerkListener(perkManager), this);

        // Commands.
        registerCommands();

        getLogger().info("SwagSlayer enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (busService != null) {
            busService.unsubscribeAll(this);
        }
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("SwagSlayer disabled — all profiles saved.");
    }

    // -------------------------------------------------------------------------
    // Command registration
    // -------------------------------------------------------------------------

    private void registerCommands() {
        SlayerCommand slayerCmd = new SlayerCommand(this, dataManager, slayerManager, guiManager);
        SlayAdminCommand adminCmd = new SlayAdminCommand(this, dataManager, slayerManager, leaderboardManager);

        PluginCommand slayer = getCommand("slayer");
        if (slayer != null) {
            slayer.setExecutor(slayerCmd);
            slayer.setTabCompleter(slayerCmd);
        } else {
            getLogger().warning("Command 'slayer' not found in plugin.yml!");
        }

        PluginCommand slayadmin = getCommand("slayadmin");
        if (slayadmin != null) {
            slayadmin.setExecutor(adminCmd);
            slayadmin.setTabCompleter(adminCmd);
        } else {
            getLogger().warning("Command 'slayadmin' not found in plugin.yml!");
        }
    }

    // -------------------------------------------------------------------------
    // SwagAPI hookup
    // -------------------------------------------------------------------------

    /**
     * Hooks SwagAPI's shared services. IDatabaseService is a hard requirement — SwagSlayer
     * no longer owns any local YAML/SQLite persistence, so if SwagAPI isn't loaded (or
     * hasn't registered the service yet), there is nowhere to store player data and the
     * plugin disables itself rather than silently falling back to something else.
     * IEventBusService is soft — used only for the optional boss-kill Discord
     * announcement in BossManager; SwagSlayer runs fine without it.
     */
    private boolean hookSwagAPI() {
        ServicesManager sm = getServer().getServicesManager();

        RegisteredServiceProvider<com.SwagDev.SwagAPI.api.IDatabaseService> dbProv =
                sm.getRegistration(com.SwagDev.SwagAPI.api.IDatabaseService.class);
        if (dbProv == null) {
            getLogger().severe("SwagAPI IDatabaseService not found! Is SwagAPI installed and " +
                    "loaded before SwagSlayer? Disabling.");
            return false;
        }
        dbService = dbProv.getProvider();
        getLogger().info("Hooked SwagAPI IDatabaseService.");

        RegisteredServiceProvider<com.SwagDev.SwagAPI.api.IEventBusService> busProv =
                sm.getRegistration(com.SwagDev.SwagAPI.api.IEventBusService.class);
        if (busProv != null) {
            busService = busProv.getProvider();
            getLogger().info("Hooked SwagAPI IEventBusService.");
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public static SwagSlayer getInstance()              { return instance; }
    public com.SwagDev.SwagAPI.api.IDatabaseService getDbService() { return dbService; }
    public com.SwagDev.SwagAPI.api.IEventBusService getBusService() { return busService; }
    public DatabaseManager getDatabaseManager()         { return databaseManager; }
    public DataManager getDataManager()                 { return dataManager; }
    public SlayerManager getSlayerManager()             { return slayerManager; }
    public LeaderboardManager getLeaderboardManager()   { return leaderboardManager; }
    public CosmeticsManager getCosmeticsManager()       { return cosmeticsManager; }
    public PerkManager getPerkManager()                 { return perkManager; }
    public BossManager getBossManager()                 { return bossManager; }
    public BestiaryManager getBestiaryManager()         { return bestiaryManager; }
    public ContractManager getContractManager()         { return contractManager; }
    public GUIManager getGUIManager()                   { return guiManager; }
}

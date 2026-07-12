package com.swag.swagslayer;

import com.swag.swagslayer.commands.SlayAdminCommand;
import com.swag.swagslayer.commands.SlayerCommand;
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
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for SwagSlayer.
 *
 * Initialization order:
 *   1. saveDefaultConfig()
 *   2. DataManager
 *   3. SlayerManager (reads config into SlayerType constants)
 *   4. LeaderboardManager
 *   5. CosmeticsManager
 *   6. PerkManager
 *   7. BossManager
 *   8. BestiaryManager
 *   9. ContractManager
 *  10. GUIManager + setter injections
 *  11. Listeners
 *  12. Commands
 */
public class SwagSlayer extends JavaPlugin {

    private static SwagSlayer instance;

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

        // Core managers.
        dataManager        = new DataManager(this);
        slayerManager      = new SlayerManager(this, dataManager);
        leaderboardManager = new LeaderboardManager(this, dataManager);
        cosmeticsManager   = new CosmeticsManager();
        perkManager        = new PerkManager(dataManager);
        bossManager        = new BossManager(this, dataManager);
        bestiaryManager    = new BestiaryManager();
        contractManager    = new ContractManager(this, dataManager);

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
    // Accessors
    // -------------------------------------------------------------------------

    public static SwagSlayer getInstance()              { return instance; }
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

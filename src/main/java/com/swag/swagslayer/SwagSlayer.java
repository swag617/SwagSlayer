package com.swag.swagslayer;

import com.swag.swagslayer.commands.SlayAdminCommand;
import com.swag.swagslayer.commands.SlayerCommand;
import com.swag.swagslayer.listeners.GUIListener;
import com.swag.swagslayer.listeners.SlayerListener;
import com.swag.swagslayer.managers.DataManager;
import com.swag.swagslayer.managers.GUIManager;
import com.swag.swagslayer.managers.LeaderboardManager;
import com.swag.swagslayer.managers.SlayerManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for SwagSlayer.
 *
 * Initialization order:
 *   1. saveDefaultConfig() — writes config.yml from jar if absent.
 *   2. DataManager — sets up data directory.
 *   3. SlayerManager — reads config into SlayerType constants.
 *   4. Listeners — registered against the PluginManager.
 *   5. Commands — executors and tab completers bound.
 */
public class SwagSlayer extends JavaPlugin {

    private static SwagSlayer instance;

    private DataManager dataManager;
    private SlayerManager slayerManager;
    private LeaderboardManager leaderboardManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure config.yml is present in the data folder.
        saveDefaultConfig();

        // Managers.
        dataManager = new DataManager(this);
        slayerManager = new SlayerManager(this, dataManager);
        leaderboardManager = new LeaderboardManager(this, dataManager);
        guiManager = new GUIManager(this, dataManager, leaderboardManager);
        guiManager.setSlayerManager(slayerManager);

        // Listeners.
        getServer().getPluginManager().registerEvents(
                new SlayerListener(this, slayerManager, dataManager), this);
        getServer().getPluginManager().registerEvents(
                new GUIListener(guiManager, slayerManager), this);

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

    public static SwagSlayer getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SlayerManager getSlayerManager() {
        return slayerManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }
}

package com.swag.swagslayer.commands;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.managers.DataManager;
import com.swag.swagslayer.managers.GUIManager;
import com.swag.swagslayer.managers.SlayerManager;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the /slayer command.
 *
 * Usage:
 *   /slayer           — shows the executing player's slayer stats.
 *   /slayer <player>  — shows another player's stats (requires swagslayer.admin).
 */
public class SlayerCommand implements CommandExecutor, TabCompleter {

    private static final String HEADER_LINE =
            ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------------------";

    private final SwagSlayer plugin;
    private final DataManager dataManager;
    private final SlayerManager slayerManager;
    private final GUIManager guiManager;

    public SlayerCommand(SwagSlayer plugin, DataManager dataManager, SlayerManager slayerManager,
                         GUIManager guiManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.slayerManager = slayerManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("swagslayer.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Player target;

        if (args.length == 0) {
            // No args from a player → open the GUI.
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /slayer <player>");
                return true;
            }
            guiManager.openMainMenu((Player) sender);
            return true;
        } else {
            // Lookup by name — admin-only.
            if (!sender.hasPermission("swagslayer.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to view other players' stats.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
                return true;
            }
        }

        sendStatsDisplay(sender, target);
        return true;
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    private void sendStatsDisplay(CommandSender sender, Player target) {
        SlayerProfile profile = dataManager.getProfile(target.getUniqueId());
        int maxLevel = plugin.getConfig().getInt("general.max_level", 5);

        sender.sendMessage(HEADER_LINE);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "  Slayer Stats"
                + (target.equals(sender) ? "" : " — " + target.getName()));
        sender.sendMessage(HEADER_LINE);

        for (SlayerType type : SlayerType.values()) {
            int level    = profile.getLevel(type);
            int xp       = profile.getXp(type);
            int kills    = profile.getKillCount(type);
            float progress = profile.getLevelProgress(type);
            int killsLeft  = profile.getKillsForNextLevel(type);

            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + type.getDisplayName()
                    + ChatColor.RESET + ChatColor.GRAY + "  (Level " + level + "/" + maxLevel + ")");

            // XP line.
            sender.sendMessage(ChatColor.GRAY + "  XP: " + ChatColor.WHITE + xp
                    + (level < maxLevel
                    ? ChatColor.GRAY + "  |  Next level in: " + ChatColor.WHITE + killsLeft + " kills"
                    : ChatColor.GOLD + "  (MAX LEVEL)"));

            // Kills line.
            sender.sendMessage(ChatColor.GRAY + "  Total kills: " + ChatColor.WHITE + kills);

            // Progress bar.
            sender.sendMessage(ChatColor.GRAY + "  Progress: " + buildProgressBar(progress));
        }

        sender.sendMessage("");
        sender.sendMessage(HEADER_LINE);
    }

    /**
     * Builds a 20-character progress bar using filled/empty block characters.
     * Example: [##########----------]  50%
     */
    private String buildProgressBar(float progress) {
        int totalBars = 20;
        int filled = Math.round(progress * totalBars);
        filled = Math.max(0, Math.min(totalBars, filled));

        StringBuilder bar = new StringBuilder(ChatColor.DARK_GRAY + "[");
        bar.append(ChatColor.GREEN);
        bar.append("|".repeat(filled));
        bar.append(ChatColor.DARK_GREEN);
        bar.append("|".repeat(totalBars - filled));
        bar.append(ChatColor.DARK_GRAY + "] ");
        bar.append(ChatColor.WHITE);
        bar.append((int) (progress * 100)).append("%");
        return bar.toString();
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("swagslayer.admin")) {
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}

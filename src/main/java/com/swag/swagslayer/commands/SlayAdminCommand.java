package com.swag.swagslayer.commands;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.managers.DataManager;
import com.swag.swagslayer.managers.LeaderboardManager;
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
import java.util.Arrays;
import java.util.List;

/**
 * Handles the /slayadmin command.
 *
 * Sub-commands:
 *   /slayadmin reload
 *   /slayadmin setlevel <player> <type> <level>
 *   /slayadmin addxp    <player> <type> <amount>
 *   /slayadmin reset    <player>
 */
public class SlayAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("reload", "setlevel", "addxp", "reset");

    private final SwagSlayer plugin;
    private final DataManager dataManager;
    private final SlayerManager slayerManager;
    private final LeaderboardManager leaderboardManager;

    public SlayAdminCommand(SwagSlayer plugin, DataManager dataManager, SlayerManager slayerManager,
                            LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.slayerManager = slayerManager;
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("swagslayer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "setlevel" -> handleSetLevel(sender, args);
            case "addxp" -> handleAddXp(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown sub-command. " + ChatColor.GRAY + "Use /slayadmin for help.");
                sendUsage(sender);
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Sub-command handlers
    // -------------------------------------------------------------------------

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        slayerManager.applyConfigToTypes();
        leaderboardManager.refreshLeaderboards();
        sender.sendMessage(ChatColor.GREEN + "SwagSlayer config reloaded.");
    }

    /**
     * /slayadmin setlevel <player> <type> <level>
     */
    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /slayadmin setlevel <player> <type> <level>");
            return;
        }

        Player target = resolveOnlinePlayer(sender, args[1]);
        if (target == null) return;

        SlayerType type = resolveSlayerType(sender, args[2]);
        if (type == null) return;

        int level = parseInt(sender, args[3]);
        if (level < 0) return;

        int maxLevel = plugin.getConfig().getInt("general.max_level", 5);
        if (level < 1 || level > maxLevel) {
            sender.sendMessage(ChatColor.RED + "Level must be between 1 and " + maxLevel + ".");
            return;
        }

        SlayerProfile profile = dataManager.getProfile(target.getUniqueId());
        profile.setLevel(type, level);
        dataManager.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s "
                + type.getDisplayName() + " level to " + level + ".");
        target.sendMessage(ChatColor.GOLD + "An admin set your " + type.getDisplayName()
                + " level to " + level + ".");
    }

    /**
     * /slayadmin addxp <player> <type> <amount>
     */
    private void handleAddXp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /slayadmin addxp <player> <type> <amount>");
            return;
        }

        Player target = resolveOnlinePlayer(sender, args[1]);
        if (target == null) return;

        SlayerType type = resolveSlayerType(sender, args[2]);
        if (type == null) return;

        int amount = parseInt(sender, args[3]);
        if (amount < 0) return;

        if (amount == 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
            return;
        }

        SlayerProfile profile = dataManager.getProfile(target.getUniqueId());
        boolean leveledUp = profile.addXp(type, amount);
        dataManager.saveProfile(profile);

        sender.sendMessage(ChatColor.GREEN + "Added " + amount + " " + type.getDisplayName()
                + " XP to " + target.getName() + "."
                + (leveledUp ? ChatColor.YELLOW + " (Level up!)" : ""));
        target.sendMessage(ChatColor.GOLD + "An admin gave you " + amount + " "
                + type.getDisplayName() + " XP!"
                + (leveledUp ? ChatColor.GREEN + " You leveled up!" : ""));
    }

    /**
     * /slayadmin reset <player>
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /slayadmin reset <player>");
            return;
        }

        Player target = resolveOnlinePlayer(sender, args[1]);
        if (target == null) return;

        // Delete the file and evict from cache so a fresh profile is created next access.
        dataManager.deleteProfile(target.getUniqueId());
        // Pre-populate cache with a fresh profile so the player doesn't hit disk mid-game.
        dataManager.getProfile(target.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "Reset all slayer data for " + target.getName() + ".");
        target.sendMessage(ChatColor.RED + "Your slayer data has been reset by an admin.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- SwagSlayer Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/slayadmin reload");
        sender.sendMessage(ChatColor.YELLOW + "/slayadmin setlevel <player> <type> <level>");
        sender.sendMessage(ChatColor.YELLOW + "/slayadmin addxp <player> <type> <amount>");
        sender.sendMessage(ChatColor.YELLOW + "/slayadmin reset <player>");
        sender.sendMessage(ChatColor.GRAY + "Types: ZOMBIE, SPIDER, SKELETON, CREEPER");
    }

    /**
     * Returns the online Player or sends an error and returns null.
     */
    private Player resolveOnlinePlayer(CommandSender sender, String name) {
        Player p = Bukkit.getPlayer(name);
        if (p == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + name + "' is not online.");
        }
        return p;
    }

    /**
     * Returns the SlayerType or sends an error and returns null.
     */
    private SlayerType resolveSlayerType(CommandSender sender, String name) {
        SlayerType type = SlayerType.fromName(name);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown slayer type '" + name
                    + "'. Valid types: ZOMBIE, SPIDER, SKELETON, CREEPER");
        }
        return type;
    }

    /**
     * Parses a positive integer or sends an error and returns -1.
     */
    private int parseInt(CommandSender sender, String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "'" + s + "' is not a valid integer.");
            return -1;
        }
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("swagslayer.admin")) return List.of();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(partial)) completions.add(sub);
            }
            return completions;
        }

        String sub = args[0].toLowerCase();

        // arg index 1 = player name for setlevel, addxp, reset
        if (args.length == 2 && (sub.equals("setlevel") || sub.equals("addxp") || sub.equals("reset"))) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
            }
            return completions;
        }

        // arg index 2 = slayer type for setlevel, addxp
        if (args.length == 3 && (sub.equals("setlevel") || sub.equals("addxp"))) {
            String partial = args[2].toUpperCase();
            for (SlayerType type : SlayerType.values()) {
                if (type.name().startsWith(partial)) completions.add(type.name());
            }
            return completions;
        }

        // arg index 3 = level hint for setlevel
        if (args.length == 4 && sub.equals("setlevel")) {
            int maxLevel = plugin.getConfig().getInt("general.max_level", 5);
            for (int i = 1; i <= maxLevel; i++) completions.add(String.valueOf(i));
            return completions;
        }

        return completions;
    }
}

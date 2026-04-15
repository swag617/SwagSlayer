package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerTask;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Creates, tracks, and closes all SwagSlayer GUI inventories.
 *
 * Tracking design:
 *   GUIManager holds two maps keyed by Inventory reference:
 *     - guiTypes:        Inventory -> GUIType  (what kind of screen is open)
 *     - guiContext:      Inventory -> SlayerType (which type a detail/leaderboard
 *                        screen is showing; null for main menu / overall leaderboard)
 *     - leaderboardBack: Inventory -> SlayerType (null = back goes to main menu,
 *                        non-null = back goes to that type's detail view)
 *
 *   Inventories are removed from both maps in removeTracking(), called by
 *   GUIListener.onInventoryClose().
 *
 * All GUI construction happens on the main thread (inventories are always
 * opened from command/click handlers which run on the main thread).
 */
public class GUIManager {

    // -------------------------------------------------------------------------
    // GUI type enum
    // -------------------------------------------------------------------------

    public enum GUIType {
        MAIN_MENU,
        TYPE_DETAIL,
        LEADERBOARD
    }

    // -------------------------------------------------------------------------
    // Material mapping for SlayerType representative items
    // -------------------------------------------------------------------------

    private static final Map<SlayerType, Material> TYPE_MATERIAL = new EnumMap<>(SlayerType.class);

    static {
        TYPE_MATERIAL.put(SlayerType.ZOMBIE,   Material.ROTTEN_FLESH);
        TYPE_MATERIAL.put(SlayerType.SPIDER,   Material.STRING);
        TYPE_MATERIAL.put(SlayerType.SKELETON, Material.BONE);
        TYPE_MATERIAL.put(SlayerType.CREEPER,  Material.GUNPOWDER);
    }

    // Slots in the main menu that correspond to each type (in enum declaration order).
    private static final int[] TYPE_SLOTS = {10, 12, 14, 16};
    // Ordered list matching TYPE_SLOTS index to SlayerType.
    private static final SlayerType[] ORDERED_TYPES = SlayerType.values();
    // Maximum leaderboard entries to display.
    private static final int TOP_SIZE = 10;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final SwagSlayer plugin;
    private final DataManager dataManager;
    private final LeaderboardManager leaderboardManager;
    private SlayerManager slayerManager; // set after construction to avoid circular dependency

    /** Maps each open SwagSlayer inventory to its type. */
    private final Map<Inventory, GUIType> guiTypes = new HashMap<>();
    /**
     * For TYPE_DETAIL and LEADERBOARD screens, stores which SlayerType is being
     * shown. Null entry = main menu or overall leaderboard.
     */
    private final Map<Inventory, SlayerType> guiContext = new HashMap<>();
    /**
     * For LEADERBOARD screens only: stores where the Back button should navigate.
     * Null = back to main menu. Non-null = back to that type's detail screen.
     */
    private final Map<Inventory, SlayerType> leaderboardBack = new HashMap<>();

    // -------------------------------------------------------------------------

    public GUIManager(SwagSlayer plugin, DataManager dataManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.leaderboardManager = leaderboardManager;
    }

    public void setSlayerManager(SlayerManager slayerManager) {
        this.slayerManager = slayerManager;
    }

    // -------------------------------------------------------------------------
    // Public API — open screens
    // -------------------------------------------------------------------------

    /**
     * Opens the main slayer menu for the player.
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD + "" + ChatColor.BOLD + "SwagSlayer Menu");
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());

        fillBorder(inv);

        // Type items at slots 10, 12, 14, 16.
        for (int i = 0; i < ORDERED_TYPES.length; i++) {
            SlayerType type = ORDERED_TYPES[i];
            int level  = profile.getLevel(type);
            int xp     = profile.getXp(type);
            int kills  = profile.getKillCount(type);

            ItemStack item = buildItem(
                    TYPE_MATERIAL.getOrDefault(type, Material.BARRIER),
                    ChatColor.YELLOW + type.getDisplayName(),
                    Arrays.asList(
                            ChatColor.GRAY + "Level: " + ChatColor.WHITE + level,
                            ChatColor.GRAY + "XP: "    + ChatColor.WHITE + xp,
                            ChatColor.GRAY + "Kills: " + ChatColor.WHITE + kills,
                            ChatColor.DARK_GRAY + "Click to view details"
                    )
            );
            inv.setItem(TYPE_SLOTS[i], item);
        }

        // Leaderboard button at slot 49.
        inv.setItem(49, buildItem(
                Material.NETHER_STAR,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboards",
                Collections.singletonList(ChatColor.GRAY + "Click to view overall leaderboard")
        ));

        track(inv, GUIType.MAIN_MENU, null);
        player.openInventory(inv);
    }

    /**
     * Opens the detail view for a single SlayerType.
     */
    public void openTypeMenu(Player player, SlayerType type) {
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + type.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());

        fillBorder(inv);

        int maxLevel = plugin.getConfig().getInt("general.max_level", 5);
        int level    = profile.getLevel(type);
        int xp       = profile.getXp(type);
        int kills    = profile.getKillCount(type);
        float progress = profile.getLevelProgress(type);
        int killsLeft  = profile.getKillsForNextLevel(type);

        // Progress bar: 20 chars, █ filled §a, ░ remainder §7.
        String progressBar = buildProgressBar(progress);

        String levelProgressLine = (level >= maxLevel)
                ? ChatColor.GREEN + "MAX LEVEL"
                : ChatColor.GRAY + "Kills to next level: " + ChatColor.WHITE + killsLeft;

        // Type icon at slot 13.
        ItemStack icon = buildItem(
                TYPE_MATERIAL.getOrDefault(type, Material.BARRIER),
                ChatColor.YELLOW + type.getDisplayName() + " " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Level " + level,
                Arrays.asList(
                        ChatColor.GRAY + "XP: " + ChatColor.WHITE + xp,
                        ChatColor.GRAY + "Progress: " + progressBar,
                        ChatColor.GRAY + "Total kills: " + ChatColor.WHITE + kills,
                        levelProgressLine
                )
        );
        inv.setItem(13, icon);

        // Task button at slot 29.
        SlayerTask activeTask = profile.getActiveTask(type);
        if (activeTask == null) {
            inv.setItem(29, buildItem(
                    Material.EMERALD,
                    ChatColor.GREEN + "Get Task",
                    Collections.singletonList(ChatColor.GRAY + "Click to receive a slayer task")
            ));
        } else {
            String taskProgress = ChatColor.WHITE + "" + activeTask.getKillsCompleted()
                    + ChatColor.GRAY + "/" + ChatColor.WHITE + activeTask.getKillGoal()
                    + ChatColor.GRAY + " kills";
            inv.setItem(29, buildItem(
                    Material.COMPASS,
                    ChatColor.YELLOW + type.getDisplayName() + " Task",
                    Arrays.asList(taskProgress, ChatColor.DARK_GRAY + "Kill mobs to progress")
            ));
        }

        // Leaderboard button at slot 33.
        inv.setItem(33, buildItem(
                Material.PAPER,
                ChatColor.AQUA + "Leaderboard",
                Collections.singletonList(ChatColor.GRAY + "Click to view " + type.getDisplayName() + " leaderboard")
        ));

        // Back button at slot 48.
        inv.setItem(48, buildItem(
                Material.ARROW,
                ChatColor.GRAY + "Back",
                Collections.singletonList(ChatColor.DARK_GRAY + "Return to main menu")
        ));

        track(inv, GUIType.TYPE_DETAIL, type);
        player.openInventory(inv);
    }

    /**
     * Opens a leaderboard view.
     *
     * @param type null for the overall leaderboard; non-null for a type-specific board.
     * @param backType where the Back button should navigate. Pass null if coming from
     *                 the main menu, or the SlayerType if coming from a detail screen.
     */
    public void openLeaderboard(Player player, SlayerType type) {
        // "Back" destination is null → main menu when type is null (overall),
        // and equals type → detail view when type is non-null.
        openLeaderboardWithBack(player, type, type);
    }

    /**
     * Internal open that lets callers specify the back-destination independently.
     * Exposed package-private so GUIListener can call it with the correct context.
     */
    public void openLeaderboardWithBack(Player player, SlayerType type, SlayerType backDest) {
        String boardLabel = (type == null) ? "Overall" : type.getDisplayName();
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Leaderboard"
                + ChatColor.RESET + " " + ChatColor.GRAY + "- " + ChatColor.WHITE + boardLabel;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorder(inv);

        // Fetch entries.
        List<LeaderboardManager.LeaderboardEntry> entries = (type == null)
                ? leaderboardManager.getTopOverall(TOP_SIZE)
                : leaderboardManager.getTopForType(type, TOP_SIZE);

        // Slot layout: entries 1-7 in slots 10-16, entries 8-10 in slots 19-21.
        int[] entrySlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};

        for (int i = 0; i < entries.size() && i < entrySlots.length; i++) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(i);
            int rank = i + 1;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) head.getItemMeta();
            if (skull != null) {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getUuid()));
                skull.setDisplayName(ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + entry.getPlayerName());

                String xpLabel = (type == null) ? "Total XP" : "XP";
                skull.setLore(Collections.singletonList(
                        ChatColor.GRAY + xpLabel + ": " + ChatColor.WHITE + entry.getXpValue()
                ));
                skull.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                head.setItemMeta(skull);
            }
            inv.setItem(entrySlots[i], head);
        }

        // Back button at slot 49.
        inv.setItem(49, buildItem(
                Material.ARROW,
                ChatColor.GRAY + "Back",
                Collections.singletonList(ChatColor.DARK_GRAY + "Return to previous menu")
        ));

        track(inv, GUIType.LEADERBOARD, type);
        leaderboardBack.put(inv, backDest);
        player.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // Tracking
    // -------------------------------------------------------------------------

    /** Returns true if the given inventory is a tracked SwagSlayer GUI. */
    public boolean isSwagSlayerGUI(Inventory inv) {
        return guiTypes.containsKey(inv);
    }

    /** Returns the GUIType for a tracked inventory, or null if not tracked. */
    public GUIType getGUIType(Inventory inv) {
        return guiTypes.get(inv);
    }

    /**
     * Returns the SlayerType context for a TYPE_DETAIL or LEADERBOARD inventory.
     * Null if the screen is for "main menu" or "overall leaderboard".
     */
    public SlayerType getGUIContext(Inventory inv) {
        return guiContext.get(inv);
    }

    /**
     * For a LEADERBOARD inventory, returns where the Back button should navigate.
     * Null = go to main menu. Non-null = go to that type's detail view.
     */
    public SlayerType getLeaderboardBack(Inventory inv) {
        return leaderboardBack.get(inv);
    }

    /**
     * Removes all tracking for the given inventory.
     * Called by GUIListener when any SwagSlayer inventory is closed.
     */
    public void removeTracking(Inventory inv) {
        guiTypes.remove(inv);
        guiContext.remove(inv);
        leaderboardBack.remove(inv);
    }

    // -------------------------------------------------------------------------
    // Helper: slot -> SlayerType for the main menu
    // -------------------------------------------------------------------------

    /**
     * Given a click slot in the main menu, returns the corresponding SlayerType,
     * or null if the slot is not a type icon.
     */
    public SlayerType getTypeForMainMenuSlot(int slot) {
        for (int i = 0; i < TYPE_SLOTS.length; i++) {
            if (TYPE_SLOTS[i] == slot) {
                return ORDERED_TYPES[i];
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void track(Inventory inv, GUIType type, SlayerType context) {
        guiTypes.put(inv, type);
        if (context != null) {
            guiContext.put(inv, context);
        }
    }

    /**
     * Fills the border slots of a 54-slot inventory with black glass panes.
     *
     * Border = top row (0-8), bottom row (45-53), left column (9,18,27,36),
     * right column (17,26,35,44).
     */
    private void fillBorder(Inventory inv) {
        ItemStack pane = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", Collections.emptyList());

        // Top row.
        for (int i = 0; i <= 8; i++) inv.setItem(i, pane);
        // Bottom row.
        for (int i = 45; i <= 53; i++) inv.setItem(i, pane);
        // Left column (excluding corners already set above).
        for (int i = 9; i <= 36; i += 9) inv.setItem(i, pane);
        // Right column (excluding corners).
        for (int i = 17; i <= 44; i += 9) inv.setItem(i, pane);
    }

    /**
     * Constructs a clean ItemStack with a display name, lore, and hidden attribute
     * / enchant flags.
     */
    private ItemStack buildItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds a 20-character XP progress bar.
     * Filled portion: §a█, empty portion: §7░.
     */
    private String buildProgressBar(float progress) {
        int total  = 20;
        int filled = Math.round(progress * total);
        filled = Math.max(0, Math.min(total, filled));

        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) bar.append('\u2588'); // █
        bar.append(ChatColor.GRAY);
        for (int i = filled; i < total; i++) bar.append('\u2591'); // ░
        return bar.toString();
    }
}

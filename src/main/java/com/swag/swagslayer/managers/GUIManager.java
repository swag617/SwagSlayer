package com.swag.swagslayer.managers;

import com.swag.swagslayer.SwagSlayer;
import com.swag.swagslayer.models.Contract;
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
 *   GUIManager holds maps keyed by Inventory reference:
 *     - guiTypes:        Inventory -> GUIType
 *     - guiContext:      Inventory -> SlayerType (which type a detail/leaderboard
 *                        screen is showing; null for main menu / overall leaderboard)
 *     - leaderboardBack: Inventory -> SlayerType (null = back goes to main menu,
 *                        non-null = back goes to that type's detail view)
 *
 *   Inventories are removed from all maps in removeTracking(), called by
 *   GUIListener.onInventoryClose().
 */
public class GUIManager {

    // -------------------------------------------------------------------------
    // GUI type enum
    // -------------------------------------------------------------------------

    public enum GUIType {
        MAIN_MENU,
        TYPE_DETAIL,
        LEADERBOARD,
        BESTIARY,
        CONTRACTS
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

    private static final int[] TYPE_SLOTS    = {10, 12, 14, 16};
    private static final SlayerType[] ORDERED_TYPES = SlayerType.values();
    // Number of types we can safely display — capped so adding a 5th SlayerType
    // before updating TYPE_SLOTS doesn't cause an ArrayIndexOutOfBoundsException.
    private static final int DISPLAYABLE_TYPES = Math.min(ORDERED_TYPES.length, TYPE_SLOTS.length);
    private static final int TOP_SIZE = 10;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final SwagSlayer plugin;
    private final DataManager dataManager;
    private final LeaderboardManager leaderboardManager;

    // Setter-injected to avoid circular dependency.
    private SlayerManager slayerManager;
    private BossManager bossManager;
    private BestiaryManager bestiaryManager;
    private ContractManager contractManager;
    private PerkManager perkManager;

    private final Map<Inventory, GUIType>    guiTypes        = new HashMap<>();
    private final Map<Inventory, SlayerType> guiContext      = new HashMap<>();
    private final Map<Inventory, SlayerType> leaderboardBack = new HashMap<>();

    // -------------------------------------------------------------------------

    public GUIManager(SwagSlayer plugin, DataManager dataManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.leaderboardManager = leaderboardManager;
    }

    public void setSlayerManager(SlayerManager slayerManager)       { this.slayerManager    = slayerManager; }
    public void setBossManager(BossManager bossManager)             { this.bossManager      = bossManager; }
    public void setBestiaryManager(BestiaryManager bestiaryManager) { this.bestiaryManager  = bestiaryManager; }
    public void setContractManager(ContractManager contractManager)  { this.contractManager  = contractManager; }
    public void setPerkManager(PerkManager perkManager)             { this.perkManager      = perkManager; }

    // -------------------------------------------------------------------------
    // Public API — open screens
    // -------------------------------------------------------------------------

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.GOLD + "" + ChatColor.BOLD + "SwagSlayer Menu");
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());

        fillBorder(inv);

        // Type icons at slots 10, 12, 14, 16.
        for (int i = 0; i < DISPLAYABLE_TYPES; i++) {
            SlayerType type = ORDERED_TYPES[i];
            int level = profile.getLevel(type);
            int xp    = profile.getXp(type);
            int kills = profile.getKillCount(type);

            inv.setItem(TYPE_SLOTS[i], buildItem(
                    TYPE_MATERIAL.getOrDefault(type, Material.BARRIER),
                    ChatColor.YELLOW + type.getDisplayName(),
                    Arrays.asList(
                            ChatColor.GRAY + "Level: " + ChatColor.WHITE + level,
                            ChatColor.GRAY + "XP: "    + ChatColor.WHITE + xp,
                            ChatColor.GRAY + "Kills: " + ChatColor.WHITE + kills,
                            ChatColor.DARK_GRAY + "Click to view details"
                    )
            ));
        }

        // Contracts button at slot 22.
        inv.setItem(22, buildItem(
                Material.CLOCK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Contracts",
                Collections.singletonList(ChatColor.GRAY + "View your daily & weekly contracts")
        ));

        // Bestiary button at slot 31.
        inv.setItem(31, buildItem(
                Material.BOOK,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Bestiary",
                Collections.singletonList(ChatColor.GRAY + "View your kill milestones & rewards")
        ));

        // Leaderboard button at slot 49.
        inv.setItem(49, buildItem(
                Material.NETHER_STAR,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Leaderboards",
                Collections.singletonList(ChatColor.GRAY + "Click to view overall leaderboard")
        ));

        track(inv, GUIType.MAIN_MENU, null);
        player.openInventory(inv);
    }

    public void openTypeMenu(Player player, SlayerType type) {
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + type.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());

        fillBorder(inv);

        int maxLevel    = plugin.getConfig().getInt("general.max_level", 5);
        int level       = profile.getLevel(type);
        int xp          = profile.getXp(type);
        int kills       = profile.getKillCount(type);
        float progress  = profile.getLevelProgress(type);
        int killsLeft   = profile.getKillsForNextLevel(type);

        String progressBar = buildProgressBar(progress);
        String levelProgressLine = (level >= maxLevel)
                ? ChatColor.GREEN + "MAX LEVEL"
                : ChatColor.GRAY + "Kills to next level: " + ChatColor.WHITE + killsLeft;

        // Build lore for type icon, appending any unlocked perks.
        List<String> iconLore = new ArrayList<>(Arrays.asList(
                ChatColor.GRAY + "XP: " + ChatColor.WHITE + xp,
                ChatColor.GRAY + "Progress: " + progressBar,
                ChatColor.GRAY + "Total kills: " + ChatColor.WHITE + kills,
                levelProgressLine
        ));
        if (perkManager != null) {
            for (int lvl = 2; lvl <= level; lvl++) {
                String desc = perkManager.getPerkDescription(type, lvl);
                if (desc != null) {
                    iconLore.add(ChatColor.GOLD + "Perk Lv" + lvl + ": " + ChatColor.YELLOW + desc);
                }
            }
        }

        // Type icon at slot 13.
        inv.setItem(13, buildItem(
                TYPE_MATERIAL.getOrDefault(type, Material.BARRIER),
                ChatColor.YELLOW + type.getDisplayName() + " " + ChatColor.GRAY + "- "
                        + ChatColor.WHITE + "Level " + level,
                iconLore
        ));

        // Boss summon button at slot 11.
        if (bossManager != null) {
            boolean hasActiveBoss = bossManager.isActiveBossForType(player.getUniqueId(), type);
            if (hasActiveBoss) {
                inv.setItem(11, buildItem(
                        Material.WITHER_SKELETON_SKULL,
                        ChatColor.RED + "Boss Active",
                        Collections.singletonList(ChatColor.GRAY + "Defeat your existing boss first!")
                ));
            } else if (level < BossManager.MIN_LEVEL_FOR_BOSS) {
                inv.setItem(11, buildItem(
                        Material.WITHER_SKELETON_SKULL,
                        ChatColor.DARK_GRAY + "Summon Boss",
                        Arrays.asList(
                                ChatColor.RED + "Requires " + type.getDisplayName()
                                        + " Level " + BossManager.MIN_LEVEL_FOR_BOSS,
                                ChatColor.DARK_GRAY + "Keep leveling up to unlock"
                        )
                ));
            } else {
                inv.setItem(11, buildItem(
                        Material.WITHER_SKELETON_SKULL,
                        ChatColor.RED + "" + ChatColor.BOLD + "Summon Boss",
                        Arrays.asList(
                                ChatColor.GRAY + "Spawn a scaled boss near you",
                                ChatColor.GRAY + "HP: " + ChatColor.WHITE + (40 * level),
                                ChatColor.GRAY + "Expires in " + ChatColor.WHITE + "5 minutes",
                                ChatColor.YELLOW + "Click to summon!"
                        )
                ));
            }
        }

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

    public void openLeaderboard(Player player, SlayerType type) {
        openLeaderboardWithBack(player, type, type);
    }

    public void openLeaderboardWithBack(Player player, SlayerType type, SlayerType backDest) {
        String boardLabel = (type == null) ? "Overall" : type.getDisplayName();
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Leaderboard"
                + ChatColor.RESET + " " + ChatColor.GRAY + "- " + ChatColor.WHITE + boardLabel;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorder(inv);

        List<LeaderboardManager.LeaderboardEntry> entries = (type == null)
                ? leaderboardManager.getTopOverall(TOP_SIZE)
                : leaderboardManager.getTopForType(type, TOP_SIZE);

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
                        ChatColor.GRAY + xpLabel + ": " + ChatColor.WHITE + entry.getXpValue()));
                skull.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                head.setItemMeta(skull);
            }
            inv.setItem(entrySlots[i], head);
        }

        inv.setItem(49, buildItem(Material.ARROW, ChatColor.GRAY + "Back",
                Collections.singletonList(ChatColor.DARK_GRAY + "Return to previous menu")));

        track(inv, GUIType.LEADERBOARD, type);
        leaderboardBack.put(inv, backDest);
        player.openInventory(inv);
    }

    /**
     * Opens the bestiary overview, showing kill milestones for each SlayerType.
     */
    public void openBestiary(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Bestiary");
        SlayerProfile profile = dataManager.getProfile(player.getUniqueId());

        fillBorder(inv);

        for (int i = 0; i < DISPLAYABLE_TYPES; i++) {
            SlayerType type = ORDERED_TYPES[i];
            List<String> lore = (bestiaryManager != null)
                    ? bestiaryManager.buildLore(profile, type)
                    : Collections.singletonList(ChatColor.RED + "Bestiary unavailable");

            inv.setItem(TYPE_SLOTS[i], buildItem(
                    TYPE_MATERIAL.getOrDefault(type, Material.BARRIER),
                    ChatColor.YELLOW + type.getDisplayName(),
                    lore
            ));
        }

        inv.setItem(49, buildItem(Material.ARROW, ChatColor.GRAY + "Back",
                Collections.singletonList(ChatColor.DARK_GRAY + "Return to main menu")));

        track(inv, GUIType.BESTIARY, null);
        player.openInventory(inv);
    }

    /**
     * Opens the contracts screen showing the player's daily and weekly contracts.
     */
    public void openContracts(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Contracts");

        fillBorder(inv);

        if (contractManager != null) {
            Contract daily  = contractManager.getDailyContract(player.getUniqueId());
            Contract weekly = contractManager.getWeeklyContract(player.getUniqueId());

            inv.setItem(20, buildContractItem(daily, false));
            inv.setItem(24, buildContractItem(weekly, true));
        } else {
            inv.setItem(22, buildItem(Material.BARRIER, ChatColor.RED + "Contracts unavailable",
                    Collections.emptyList()));
        }

        inv.setItem(49, buildItem(Material.ARROW, ChatColor.GRAY + "Back",
                Collections.singletonList(ChatColor.DARK_GRAY + "Return to main menu")));

        track(inv, GUIType.CONTRACTS, null);
        player.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // Tracking
    // -------------------------------------------------------------------------

    public boolean isSwagSlayerGUI(Inventory inv) {
        return guiTypes.containsKey(inv);
    }

    public GUIType getGUIType(Inventory inv) {
        return guiTypes.get(inv);
    }

    public SlayerType getGUIContext(Inventory inv) {
        return guiContext.get(inv);
    }

    public SlayerType getLeaderboardBack(Inventory inv) {
        return leaderboardBack.get(inv);
    }

    public void removeTracking(Inventory inv) {
        guiTypes.remove(inv);
        guiContext.remove(inv);
        leaderboardBack.remove(inv);
    }

    public SlayerType getTypeForMainMenuSlot(int slot) {
        for (int i = 0; i < TYPE_SLOTS.length; i++) {
            if (TYPE_SLOTS[i] == slot) return ORDERED_TYPES[i];
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void track(Inventory inv, GUIType type, SlayerType context) {
        guiTypes.put(inv, type);
        if (context != null) guiContext.put(inv, context);
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0;  i <= 8;  i++) inv.setItem(i, pane);
        for (int i = 45; i <= 53; i++) inv.setItem(i, pane);
        for (int i = 9;  i <= 36; i += 9) inv.setItem(i, pane);
        for (int i = 17; i <= 44; i += 9) inv.setItem(i, pane);
    }

    private ItemStack buildItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (!lore.isEmpty()) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildContractItem(Contract c, boolean weekly) {
        String label  = weekly ? "Weekly Contract" : "Daily Contract";
        Material mat  = weekly ? Material.AMETHYST_SHARD : Material.CLOCK;
        ChatColor col = weekly ? ChatColor.LIGHT_PURPLE : ChatColor.GREEN;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + c.getType().getDisplayName());
        lore.add(ChatColor.GRAY + "Goal: " + ChatColor.WHITE + c.getKillGoal() + " kills");
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + c.getKillsCompleted()
                + ChatColor.GRAY + "/" + ChatColor.WHITE + c.getKillGoal());
        lore.add(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + c.getXpReward() + " XP");

        if (c.isCompleted()) {
            lore.add(ChatColor.GREEN + "COMPLETED!");
        } else {
            lore.add(ChatColor.GRAY + "Resets in: " + ChatColor.WHITE + c.getTimeRemainingString());
        }

        return buildItem(mat, col + "" + ChatColor.BOLD + label, lore);
    }

    private String buildProgressBar(float progress) {
        int total  = 20;
        int filled = Math.max(0, Math.min(total, Math.round(progress * total)));
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filled;  i++) bar.append('\u2588');
        bar.append(ChatColor.GRAY);
        for (int i = filled; i < total; i++) bar.append('\u2591');
        return bar.toString();
    }
}

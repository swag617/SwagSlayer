package com.swag.swagslayer.managers;

import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Manages passive combat perks unlocked by levelling up each SlayerType.
 *
 * Level 2 of any type: +10% outgoing damage to that mob class.
 * Level 3 of any type: -10% incoming damage from that mob class.
 *
 * Applied live in PerkListener via EntityDamageByEntityEvent.
 */
public class PerkManager {

    private final DataManager dataManager;

    public PerkManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Returns a coloured description of the perk unlocked at the given level,
     * or null if no perk exists for that type+level combination.
     */
    public String getPerkDescription(SlayerType type, int level) {
        return switch (type) {
            case ZOMBIE -> switch (level) {
                case 2 -> "§a+10% damage to Zombies";
                case 3 -> "§aZombies may drop extra loot";
                default -> null;
            };
            case SPIDER -> switch (level) {
                case 2 -> "§a+10% damage to Spiders";
                case 3 -> "§aSpiders deal 10% less damage to you";
                default -> null;
            };
            case SKELETON -> switch (level) {
                case 2 -> "§a+10% damage to Skeletons";
                case 3 -> "§aSkeletons deal 10% less damage to you";
                default -> null;
            };
            case CREEPER -> switch (level) {
                case 2 -> "§a+10% damage to Creepers";
                case 3 -> "§aCreeper explosions deal 10% less damage";
                default -> null;
            };
        };
    }

    /**
     * Returns the outgoing damage multiplier for a player attacking the given entity type.
     * 1.1 if the player is level 2+ for that slayer type; 1.0 otherwise.
     */
    public double getDamageMultiplier(Player attacker, EntityType targetType) {
        SlayerType slayerType = SlayerType.fromEntityType(targetType);
        if (slayerType == null) return 1.0;
        SlayerProfile profile = dataManager.getProfile(attacker.getUniqueId());
        return profile.getLevel(slayerType) >= 2 ? 1.1 : 1.0;
    }

    /**
     * Returns the incoming damage multiplier when a player is attacked by the given entity type.
     * 0.9 if the player is level 3+ for that slayer type; 1.0 otherwise.
     */
    public double getReductionMultiplier(Player victim, EntityType attackerType) {
        SlayerType slayerType = SlayerType.fromEntityType(attackerType);
        if (slayerType == null) return 1.0;
        SlayerProfile profile = dataManager.getProfile(victim.getUniqueId());
        return profile.getLevel(slayerType) >= 3 ? 0.9 : 1.0;
    }

    /**
     * Sends a perk-unlocked message if the new level grants a perk.
     * Called from SlayerManager when a level-up occurs.
     */
    public void notifyLevelUp(Player player, SlayerType type, int newLevel) {
        String desc = getPerkDescription(type, newLevel);
        if (desc != null) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Perk Unlocked: "
                    + ChatColor.RESET + desc);
        }
    }
}

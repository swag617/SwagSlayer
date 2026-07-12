package com.swag.swagslayer.listeners;

import com.swag.swagslayer.managers.PerkManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Applies passive combat perks unlocked through slayer progression.
 *
 * Level 2 perk: +10% outgoing damage against the matching mob class.
 * Level 3 perk: -10% incoming damage from the matching mob class.
 *
 * Projectile damage (arrows from skeletons, etc.) is handled by resolving
 * the shooter of the Projectile entity.
 */
public class PerkListener implements Listener {

    private final PerkManager perkManager;

    public PerkListener(PerkManager perkManager) {
        this.perkManager = perkManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // --- Outgoing damage bonus (player hits a mob) ---
        if (event.getDamager() instanceof Player attacker
                && !(event.getEntity() instanceof Player)) {
            double mult = perkManager.getDamageMultiplier(attacker, event.getEntity().getType());
            if (mult != 1.0) {
                event.setDamage(event.getDamage() * mult);
            }
        }

        // --- Incoming damage reduction (player is hit by a mob) ---
        if (event.getEntity() instanceof Player victim) {
            org.bukkit.entity.EntityType attackerType = resolveAttackerType(event);
            if (attackerType != null) {
                double mult = perkManager.getReductionMultiplier(victim, attackerType);
                if (mult != 1.0) {
                    event.setDamage(event.getDamage() * mult);
                }
            }
        }
    }

    /**
     * Resolves the EntityType of the attacker, unwrapping Projectile shooters.
     * Returns null if the attacker is a Player (we don't reduce PvP damage).
     */
    private org.bukkit.entity.EntityType resolveAttackerType(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) return null;

        if (event.getDamager() instanceof Projectile proj) {
            if (proj.getShooter() instanceof LivingEntity shooter
                    && !(proj.getShooter() instanceof Player)) {
                return shooter.getType();
            }
            return null;
        }

        if (event.getDamager() instanceof LivingEntity) {
            return event.getDamager().getType();
        }

        return null;
    }
}

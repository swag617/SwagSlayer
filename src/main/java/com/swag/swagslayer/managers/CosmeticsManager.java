package com.swag.swagslayer.managers;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Handles visual and audio feedback for kill streaks and boss defeats.
 *
 * Combo action bar is shown from streak 2 onward.
 * Particle bursts fire at milestone streak counts (5, 10, 20+).
 * Boss kills trigger a firework burst and action bar announcement.
 */
public class CosmeticsManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    public void handleKillCosmetics(Player player, Location killedLoc, int streak, double multiplier) {
        if (streak >= 2) {
            String msg = "§6x" + streak + " Combo! §e(x" + String.format("%.1f", multiplier) + ")";
            player.sendActionBar(LEGACY.deserialize(msg));
        }

        if (killedLoc == null) return;
        World world = killedLoc.getWorld();
        if (world == null) return;

        if (streak == 5) {
            world.spawnParticle(Particle.FLAME, killedLoc, 30, 0.5, 0.5, 0.5, 0.1);
        } else if (streak == 10) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, killedLoc, 40, 0.5, 0.5, 0.5, 0.1);
        } else if (streak >= 20) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, killedLoc, 60, 0.5, 0.5, 0.5, 0.2);
        }
    }

    public void handleBossKillCosmetics(Player player, Location bossLoc) {
        if (bossLoc != null && bossLoc.getWorld() != null) {
            bossLoc.getWorld().spawnParticle(Particle.FIREWORK, bossLoc, 80, 1.0, 1.0, 1.0, 0.3);
        }
        player.sendActionBar(LEGACY.deserialize("§6§lBOSS DEFEATED!"));
    }
}

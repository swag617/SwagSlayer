package com.swag.swagslayer.managers;

import com.swag.swagslayer.models.SlayerProfile;
import com.swag.swagslayer.models.SlayerType;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds bestiary lore for each SlayerType, derived entirely from SlayerProfile.
 * No stored state — all data comes from the profile at call time.
 *
 * Milestones: 10, 50, 100, 500, 1000 kills.
 * Completion is non-linear: each milestone bracket maps to a percent band.
 */
public class BestiaryManager {

    private static final int[] MILESTONES         = {10, 50, 100, 500, 1000};
    private static final int[] MILESTONE_PERCENTS = {10, 25,  50,  75,  100};
    private static final int[] MILESTONE_REWARDS  = {50, 100, 200, 500, 1000};

    public int getCompletionPercent(int kills) {
        if (kills >= 1000) return 100;
        for (int i = MILESTONES.length - 1; i >= 0; i--) {
            if (kills >= MILESTONES[i]) {
                int lower = MILESTONES[i];
                int upper = (i < MILESTONES.length - 1) ? MILESTONES[i + 1] : 1000;
                int lPct  = MILESTONE_PERCENTS[i];
                int uPct  = (i < MILESTONE_PERCENTS.length - 1) ? MILESTONE_PERCENTS[i + 1] : 100;
                return lPct + (kills - lower) * (uPct - lPct) / (upper - lower);
            }
        }
        return kills; // 0–9 → 0–9%
    }

    public boolean isMilestone(int kills) {
        for (int m : MILESTONES) if (kills == m) return true;
        return false;
    }

    public int getMilestoneReward(int kills) {
        for (int i = 0; i < MILESTONES.length; i++) {
            if (kills == MILESTONES[i]) return MILESTONE_REWARDS[i];
        }
        return 0;
    }

    public List<String> buildLore(SlayerProfile profile, SlayerType type) {
        int kills      = profile.getKillCount(type);
        int bossKills  = profile.getBossKills(type);
        int completion = getCompletionPercent(kills);

        // 10-character progress bar.
        int filled = Math.min(10, completion / 10);
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) bar.append('█');
        bar.append(ChatColor.GRAY);
        for (int i = filled; i < 10; i++) bar.append('░');

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Total kills: "     + ChatColor.WHITE + kills);
        lore.add(ChatColor.GRAY + "Bosses defeated: " + ChatColor.WHITE + bossKills);
        lore.add(ChatColor.GRAY + "Completion: " + bar + ChatColor.GRAY + " " + completion + "%");

        // Show next milestone.
        for (int i = 0; i < MILESTONES.length; i++) {
            if (kills < MILESTONES[i]) {
                lore.add(ChatColor.GRAY + "Next milestone: " + ChatColor.WHITE + MILESTONES[i]
                        + ChatColor.GRAY + " kills (" + ChatColor.GOLD + MILESTONE_REWARDS[i]
                        + " XP" + ChatColor.GRAY + ")");
                break;
            }
        }
        return lore;
    }
}

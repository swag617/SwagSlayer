# ⚔️ Slayer System

The slayer system is the core of SwagSlayer. Players accumulate XP by killing tracked mobs, level up per mob type, and track their total kill counts — all independently per type.

## Slayer Types

There are four slayer types, each tied to a specific vanilla mob:

| Type | Mob | GUI Item |
|------|-----|---------|
| Zombie Slayer | Zombie | Rotten Flesh |
| Spider Slayer | Spider | String |
| Skeleton Slayer | Skeleton | Bone |
| Creeper Slayer | Creeper | Gunpowder |

Each type maintains its own **level**, **XP total**, **kill count**, and **active task** per player.

## Levels

Every type starts at **Level 1** and caps at **Level 5** (configurable). Levels are earned by accumulating enough XP through kills.

### Default Kill Thresholds

| Level Up | Kills Required | Cumulative Kills | XP Required |
|----------|---------------|-----------------|-------------|
| 1 → 2 | 20 | 20 | 200 |
| 2 → 3 | 50 | 70 | 700 |
| 3 → 4 | 100 | 170 | 1,700 |
| 4 → 5 | 200 | 370 | 3,700 |

> With default `xp_per_kill: 10`. Actual XP to level also depends on your combo multiplier.

## XP

Each kill on a tracked mob earns base XP (`xp_per_kill`) multiplied by your current combo streak multiplier.

```
earned_xp = xp_per_kill × combo_multiplier
```

XP is **cumulative** — it never resets on level-up. The level system checks your total XP against a running threshold to determine when you advance.

### Level-Up Notification

When a level-up occurs, the player receives a chat message:

```
LEVEL UP! Zombie Slayer is now level 2!
```

If multiple levels are earned from one large XP grant (e.g., via `/slayadmin addxp`), all of them trigger in the same tick.

## Kill Count

Kill count is tracked separately from XP. It is a purely informational counter — useful for display in the GUI and for personal stat tracking. Kill count is never reset by level-ups.

## At Max Level

Once a player reaches the maximum level for a type:
* Additional XP is still recorded (for display purposes)
* No further level-ups occur
* The GUI progress bar shows 100%
* The `/slayer` stats display shows `(MAX LEVEL)` next to the XP

## Viewing Stats

### GUI
Run `/slayer` to open the interactive GUI. Click any type icon to see the detail screen for that type, including:
* Current level / max level
* Total XP earned
* Total kills
* Progress bar toward next level
* Kills remaining to next level

### Chat Stats (Admin)
Admins can view any online player's stats in chat format:

```
/slayer <player>
```

Output example:
```
----------------------------
  Slayer Stats — Steve
----------------------------

Zombie Slayer  (Level 3/5)
  XP: 1700  |  Next level in: 200 kills
  Total kills: 174
  Progress: [██████████----------]  50%
```

## Related Pages

* [Slayer Tasks](tasks.md) — level-scaled kill tasks
* [Combo Streak](combo-streak.md) — XP multiplier system
* [Configuration Reference](../server-owners/configuration.md) — tuning thresholds and XP values

# 🚀 Quick Start

Get up and running with SwagSlayer in under 5 minutes.

## For Players

### Step 1: Open Your Slayer Menu

Run `/slayer` to open the main GUI. You'll see four type icons — one for each tracked mob:

* **Rotten Flesh** — Zombie Slayer
* **String** — Spider Slayer
* **Bone** — Skeleton Slayer
* **Gunpowder** — Creeper Slayer

Each shows your current level, XP, and total kills.

### Step 2: Get a Task

Click any type icon to open its detail screen, then click the **Get Task** button (green emerald). You'll be assigned a kill goal scaled to your current level.

> **Level 1 task example:** Kill 18 Zombies

### Step 3: Start Killing

Head out and kill the target mob type. Each kill:
1. Adds XP to your slayer total
2. Increments your combo streak (if within 10 seconds of the last kill)
3. Counts toward your active task

You'll see task progress messages in chat:

```
[Zombie Slayer Task] 7/18
```

### Step 4: Earn Your Bonus

When you complete the task:

```
Task complete! You finished your Zombie Slayer task and earned 100 XP!
```

The bonus XP is added immediately and can trigger a level-up.

### Step 5: Level Up

When you accumulate enough XP:

```
LEVEL UP! Zombie Slayer is now level 2!
```

Higher levels unlock harder — and more rewarding — tasks.

### Step 6: Check the Leaderboard

From the main menu, click the **Nether Star** (slot 49) to open the overall leaderboard. From a type detail screen, click the **Paper** icon to see the per-type board.

## Combo Streak Tips

The combo streak multiplies your XP for rapid kills:

| Streak | Multiplier |
|--------|-----------|
| 1 | 1.0× |
| 2 | 1.1× |
| 5 | 1.4× |
| 10 | 1.9× |
| 11+ | 2.0× (cap) |

* Kill mobs back-to-back — don't wait more than 10 seconds between kills
* Any tracked mob type keeps your streak alive (killing Spiders keeps a Zombie streak going)
* Check your current streak multiplier in chat — it's applied silently to every XP gain

## For Server Owners

### Verify Everything Works

1. Join the server
2. Run `/slayer` — GUI should open
3. Kill a zombie — check for XP gain in chat
4. Run `/slayadmin reload` — should report success

### First Tweaks

Edit `plugins/SwagSlayer/config.yml`:

```yaml
general:
  combo_timeout_seconds: 10   # increase for a more forgiving streak window
  combo_multiplier_per_streak: 0.1  # lower for slower streak growth
```

Apply with `/slayadmin reload`.

### Give a Player a Head Start

```
/slayadmin setlevel Steve ZOMBIE 3
/slayadmin addxp Steve ZOMBIE 500
```

## Next Steps

[Slayer System](../core-features/slayer-system.md)

[Slayer Tasks](../core-features/tasks.md)

[Combo Streak](../core-features/combo-streak.md)

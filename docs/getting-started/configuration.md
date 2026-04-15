# ⚙️ Configuration

SwagSlayer's configuration lives in `plugins/SwagSlayer/config.yml`. All values can be changed and applied live with `/slayadmin reload` — no server restart required.

## Full Default Config

```yaml
general:
  max_level: 5
  # How many seconds without a kill before the combo streak resets.
  combo_timeout_seconds: 10
  # Each streak kill adds this fraction to the XP multiplier (e.g. 0.1 = +10% per kill, capped at 2.0x).
  combo_multiplier_per_streak: 0.1

slayer_types:
  ZOMBIE:
    display_name: "Zombie Slayer"
    boss_mob: "ZOMBIE"
    kill_threshold_per_level:
      - 20
      - 50
      - 100
      - 200
      - 500
    xp_per_kill: 10
    boss_xp_reward: 100

  SPIDER:
    display_name: "Spider Slayer"
    boss_mob: "SPIDER"
    kill_threshold_per_level:
      - 20
      - 50
      - 100
      - 200
      - 500
    xp_per_kill: 10
    boss_xp_reward: 100

  SKELETON:
    display_name: "Skeleton Slayer"
    boss_mob: "SKELETON"
    kill_threshold_per_level:
      - 20
      - 50
      - 100
      - 200
      - 500
    xp_per_kill: 10
    boss_xp_reward: 100

  CREEPER:
    display_name: "Creeper Slayer"
    boss_mob: "CREEPER"
    kill_threshold_per_level:
      - 20
      - 50
      - 100
      - 200
      - 500
    xp_per_kill: 10
    boss_xp_reward: 100
```

## General Settings

| Key | Default | Description |
|-----|---------|-------------|
| `max_level` | `5` | Maximum level a player can reach per slayer type |
| `combo_timeout_seconds` | `10` | Seconds of inactivity before the combo streak resets to 1 |
| `combo_multiplier_per_streak` | `0.1` | XP multiplier bonus added per streak kill (capped at 2.0×) |

### Combo Formula

```
multiplier = 1.0 + (streak - 1) × combo_multiplier_per_streak
multiplier = min(multiplier, 2.0)
```

A streak of 1 (first kill or after timeout) always yields exactly `1.0×`. With the default `0.1` step, the multiplier hits `2.0×` at a streak of 11.

## Slayer Type Settings

Each type under `slayer_types` supports:

| Key | Description |
|-----|-------------|
| `display_name` | Name shown in chat, GUI, and task messages |
| `boss_mob` | Vanilla `EntityType` string this type tracks (e.g. `"ZOMBIE"`) |
| `kill_threshold_per_level` | List of kill counts required to advance each level. Length must equal `max_level`. |
| `xp_per_kill` | Base XP earned per qualifying kill (before combo multiplier) |
| `boss_xp_reward` | Bonus XP awarded when a kill task is completed |

### Kill Thresholds Explained

The `kill_threshold_per_level` list is **cumulative** — each entry is the total kills needed since level 1 to reach the next level:

```yaml
kill_threshold_per_level:
  - 20    # kills to reach Level 2  (total: 20)
  - 50    # kills to reach Level 3  (total: 70)
  - 100   # kills to reach Level 4  (total: 170)
  - 200   # kills to reach Level 5  (total: 370)
  - 500   # (unused at max_level=5, but kept for consistency)
```

With `xp_per_kill: 10`:
* Level 1 → 2: 200 XP
* Level 2 → 3: 500 XP
* Level 3 → 4: 1000 XP
* Level 4 → 5: 2000 XP

## Example: Harder Progression

To make leveling more grindy:

```yaml
general:
  max_level: 5
  combo_timeout_seconds: 8
  combo_multiplier_per_streak: 0.05

slayer_types:
  ZOMBIE:
    display_name: "Zombie Slayer"
    boss_mob: "ZOMBIE"
    kill_threshold_per_level:
      - 50
      - 150
      - 300
      - 600
      - 1000
    xp_per_kill: 10
    boss_xp_reward: 150
```

## Applying Changes

After editing `config.yml`, apply without restarting:

```
/slayadmin reload
```

This reloads the config, pushes updated values into all slayer types, and refreshes leaderboards.

> **Note:** Changes to `max_level` will not retroactively cap players who already exceeded it. Use `/slayadmin setlevel` to correct any players if needed.

## Related Pages

* [Admin Commands](../server-owners/admin-commands.md) — manage players and reload config
* [Slayer System](../core-features/slayer-system.md) — how XP and levels work

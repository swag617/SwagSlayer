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

# Published on SwagAPI's event bus (discordutils:notify channel) when a boss is slain.
# Requires DiscordUtils to be installed with a matching "webhooks.<webhook-name>" entry in
# ITS config.yml — no compile-time or reflection dependency on DiscordUtils from this plugin.
discord:
  enabled: true
  webhook-name: "slayer"

slayer_types:
  # --- Easiest --- Common overworld mob, quick grind, low XP reward.
  ZOMBIE:
    display_name: "Zombie Slayer"
    boss_mob: "ZOMBIE"
    kill_threshold_per_level:
      - 15
      - 40
      - 80
      - 150
      - 300
    xp_per_kill: 8
    boss_xp_reward: 80

  # --- Medium --- Cave and surface mob, moderate challenge.
  SPIDER:
    display_name: "Spider Slayer"
    boss_mob: "SPIDER"
    kill_threshold_per_level:
      - 20
      - 50
      - 100
      - 200
      - 400
    xp_per_kill: 10
    boss_xp_reward: 120

  # --- Harder --- Ranged attacker, higher XP reward for the risk.
  SKELETON:
    display_name: "Skeleton Slayer"
    boss_mob: "SKELETON"
    kill_threshold_per_level:
      - 25
      - 60
      - 120
      - 250
      - 500
    xp_per_kill: 12
    boss_xp_reward: 150

  # --- Hardest --- Explosive mob, highest XP but most dangerous grind.
  CREEPER:
    display_name: "Creeper Slayer"
    boss_mob: "CREEPER"
    kill_threshold_per_level:
      - 30
      - 70
      - 150
      - 300
      - 600
    xp_per_kill: 15
    boss_xp_reward: 200
```

## General Settings

| Key | Default | Description |
|-----|---------|-------------|
| `max_level` | `5` | Maximum level a player can reach per slayer type |
| `combo_timeout_seconds` | `10` | Seconds of inactivity before the combo streak resets to 1 |
| `combo_multiplier_per_streak` | `0.1` | XP multiplier bonus added per streak kill (capped at 2.0×) |

## Discord Settings

| Key | Default | Description |
|-----|---------|-------------|
| `discord.enabled` | `true` | Whether a boss-kill notification is published on SwagAPI's event bus (`discordutils:notify` channel) |
| `discord.webhook-name` | `"slayer"` | Must match a `webhooks.<name>` entry in [DiscordUtils](https://github.com/swag617/DiscordUtils)' own `config.yml`. SwagSlayer has no compile-time or reflection dependency on DiscordUtils — it only publishes to SwagAPI's shared event bus, so if DiscordUtils isn't installed the message is simply never delivered. |

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

Each entry in `kill_threshold_per_level` is the number of kills required to advance **from that level to the next** (not a running total). The plugin sums the entries internally to determine cumulative XP thresholds. Using the Zombie Slayer defaults:

```yaml
kill_threshold_per_level:
  - 15    # kills to reach Level 2  (cumulative: 15)
  - 40    # kills to reach Level 3  (cumulative: 55)
  - 80    # kills to reach Level 4  (cumulative: 135)
  - 150   # kills to reach Level 5  (cumulative: 285)
  - 300   # (unused at max_level=5, but kept for consistency)
```

With `xp_per_kill: 8`:
* Level 1 → 2: 120 XP
* Level 2 → 3: 320 XP
* Level 3 → 4: 640 XP
* Level 4 → 5: 1200 XP

## Example: Harder Progression

To make leveling more grindy, e.g. for Zombie Slayer:

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

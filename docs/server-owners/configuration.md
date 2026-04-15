# 📄 Configuration Reference

Full reference for `plugins/SwagSlayer/config.yml`. All settings can be applied live with `/slayadmin reload`.

## File Location

```
plugins/SwagSlayer/config.yml
```

If the file is missing, restart the server — it will be regenerated from the built-in defaults.

## `general` Section

Controls global plugin behavior.

```yaml
general:
  max_level: 5
  combo_timeout_seconds: 10
  combo_multiplier_per_streak: 0.1
```

### `max_level`

**Type:** Integer  
**Default:** `5`  
**Description:** The maximum level a player can reach for any slayer type. Kill thresholds must have at least this many entries.

> Changing `max_level` does not retroactively cap players. Use `/slayadmin setlevel` to correct any players above the new cap if needed.

### `combo_timeout_seconds`

**Type:** Integer (seconds)  
**Default:** `10`  
**Description:** How many seconds must pass without a qualifying kill before the combo streak resets to 1.

### `combo_multiplier_per_streak`

**Type:** Double  
**Default:** `0.1`  
**Description:** XP multiplier bonus added per streak kill. The total multiplier is `1.0 + (streak - 1) × value`, capped at `2.0`.

Examples:
* `0.1` → cap at streak 11
* `0.2` → cap at streak 6
* `0.05` → cap at streak 21

## `slayer_types` Section

One sub-section per slayer type. Each key must match a value in the `SlayerType` enum (`ZOMBIE`, `SPIDER`, `SKELETON`, `CREEPER`).

```yaml
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
```

### `display_name`

**Type:** String  
**Description:** The name shown in chat messages, GUI titles, and task notifications. Supports `&` color codes.

### `boss_mob`

**Type:** String (Bukkit `EntityType` name)  
**Default:** matches the type key (e.g. `"ZOMBIE"`)  
**Description:** The vanilla mob entity type that this slayer type tracks. Must be a valid Paper/Spigot `EntityType`. If an invalid value is provided, the previous value is retained and a warning is logged.

Valid values for current types:

| Type Key | Default boss_mob |
|----------|-----------------|
| `ZOMBIE` | `ZOMBIE` |
| `SPIDER` | `SPIDER` |
| `SKELETON` | `SKELETON` |
| `CREEPER` | `CREEPER` |

### `kill_threshold_per_level`

**Type:** List of Integers  
**Description:** Number of kills required (cumulative from level 1) to advance each level. The list must contain at least `max_level` entries.

The plugin reads entries at index `[0]` through `[max_level - 1]`. The threshold for level N → N+1 is the **sum** of entries `[0]` through `[N-1]`.

### `xp_per_kill`

**Type:** Integer  
**Default:** `10`  
**Description:** Base XP earned per qualifying kill before the combo multiplier is applied.

### `boss_xp_reward`

**Type:** Integer  
**Default:** `100`  
**Description:** Bonus XP awarded to the player when they complete a kill task for this type.

## Full Example

```yaml
general:
  max_level: 5
  combo_timeout_seconds: 10
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

## Related Pages

* [Admin Commands](admin-commands.md) — applying config changes with `/slayadmin reload`
* [Slayer System](../core-features/slayer-system.md) — how thresholds and XP interact
* [Combo Streak](../core-features/combo-streak.md) — combo multiplier behavior

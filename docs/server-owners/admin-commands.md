# 🛠️ Admin Commands

All administrative commands are under `/slayadmin`. This command requires the `swagslayer.admin` permission (default: op).

## Command Reference

| Command | Description |
|---------|-------------|
| `/slayadmin reload` | Reload `config.yml` and apply all changes live |
| `/slayadmin setlevel <player> <type> <level>` | Set a player's level for a specific slayer type |
| `/slayadmin addxp <player> <type> <amount>` | Add XP to a player for a specific slayer type |
| `/slayadmin reset <player>` | Reset all slayer data for a player |

All commands support tab-completion for player names, slayer types, and level values.

## Reload

```
/slayadmin reload
```

Reloads `config.yml` and immediately applies:
* Updated display names, kill thresholds, XP values, and boss XP rewards per type
* Updated combo settings (timeout and multiplier step)
* Refreshed leaderboard data

Does **not** require a server restart. Changes take effect for all players instantly.

## Set Level

```
/slayadmin setlevel <player> <type> <level>
```

Sets the specified online player's level for a slayer type directly, bypassing normal XP requirements.

**Arguments:**

| Argument | Values |
|----------|--------|
| `<player>` | Online player name |
| `<type>` | `ZOMBIE`, `SPIDER`, `SKELETON`, `CREEPER` |
| `<level>` | `1` through `max_level` (default 5) |

**Examples:**
```
/slayadmin setlevel Steve ZOMBIE 3
/slayadmin setlevel Alex SKELETON 5
```

**Responses:**

*To the admin:*
```
Set Steve's Zombie Slayer level to 3.
```

*To the target player:*
```
An admin set your Zombie Slayer level to 3.
```

> **Note:** `setlevel` adjusts the level directly but does not change the player's XP total. Their displayed XP may be inconsistent with their new level until they earn more kills. Use `addxp` afterward if needed.

## Add XP

```
/slayadmin addxp <player> <type> <amount>
```

Adds the specified amount of XP to a player for a slayer type. If the XP total crosses a level threshold, the player levels up automatically.

**Arguments:**

| Argument | Values |
|----------|--------|
| `<player>` | Online player name |
| `<type>` | `ZOMBIE`, `SPIDER`, `SKELETON`, `CREEPER` |
| `<amount>` | Any positive integer |

**Examples:**
```
/slayadmin addxp Steve ZOMBIE 500
/slayadmin addxp Alex CREEPER 1000
```

**Responses:**

*To the admin (no level-up):*
```
Added 500 Zombie Slayer XP to Steve.
```

*To the admin (level-up occurred):*
```
Added 500 Zombie Slayer XP to Steve. (Level up!)
```

*To the target player (level-up):*
```
An admin gave you 500 Zombie Slayer XP! You leveled up!
```

## Reset

```
/slayadmin reset <player>
```

Completely wipes all slayer data for the specified online player — levels, XP, kill counts, and active tasks across all types. A fresh profile is created immediately so the player doesn't experience any errors.

**Example:**
```
/slayadmin reset Steve
```

**Responses:**

*To the admin:*
```
Reset all slayer data for Steve.
```

*To the target player:*
```
Your slayer data has been reset by an admin.
```

> **This action is irreversible.** Make sure you have a backup of SwagAPI's shared database before resetting — the player's rows in `slayer_profiles` (and `slayer_contracts`, if applicable) are deleted immediately.

## Valid Slayer Types

| Type String | Mob |
|-------------|-----|
| `ZOMBIE` | Zombie |
| `SPIDER` | Spider |
| `SKELETON` | Skeleton |
| `CREEPER` | Creeper |

Type strings are case-insensitive — `zombie`, `Zombie`, and `ZOMBIE` all work.

## Player Stats (Admin View)

```
/slayer <player>
```

Admins with `swagslayer.admin` can view any online player's stats in chat format:

```
----------------------------
  Slayer Stats — Steve
----------------------------

Zombie Slayer  (Level 3/5)
  XP: 1700  |  Next level in: 200 kills
  Total kills: 174
  Progress: [██████████----------]  50%
...
```

## Related Pages

* [Permissions](permissions.md) — permission node reference
* [Configuration Reference](configuration.md) — what reload applies

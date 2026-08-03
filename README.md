# SwagSlayer

Custom mob-slayer progression system for Paper/Spigot 1.21+. Players level up per mob type, complete level-scaled kill tasks, build combo streaks for XP bonuses, and compete on type-specific leaderboards — all tracked persistently per player.

📖 **[Live Documentation](https://swag617.github.io/SwagSlayer/)** · 📦 **[Releases](https://github.com/swag617/SwagSlayer/releases)**

## Features

- **Per-type progression** — 4 independent slayer types (Zombie, Spider, Skeleton, Creeper), each with its own level, XP, and kill count
- **Kill tasks** — level-scaled kill goals with bonus XP on completion
- **Combo streak** — rapid kills build a multiplier up to 2.0x XP; decays after a configurable timeout
- **Boss encounters** — summon a boss version of a tracked mob from the GUI
- **Leaderboards** — overall (combined XP) and per-type leaderboard GUIs with player-head icons
- **Bestiary & cosmetics** — mob bestiary tracking and cosmetic rewards
- **Contracts** — daily/weekly contract system
- **Perks** — unlockable perks tied to progression
- **Admin tools** — `/slayadmin reload|setlevel|addxp|reset`, all hot-reloadable without a restart
- **Discord integration (optional)** — publishes a boss-kill event on SwagAPI's shared event bus for pickup by DiscordUtils

## Requirements

| Dependency | Required | Notes |
|---|---|---|
| Paper/Spigot 1.21+ | Yes | Built against Paper API `1.21.1-R0.1-SNAPSHOT`; `plugin.yml` declares `api-version: "1.21"` |
| Java 21 | Yes | |
| [SwagAPI](https://github.com/swag617/SwagAPI) | **Yes** | Hard dependency (`depend: [SwagAPI]`) — provides the shared database service (and optional event bus) SwagSlayer stores all player data in. SwagSlayer disables itself on startup if SwagAPI isn't loaded. |
| DiscordUtils | No | Only needed for the optional boss-kill Discord notification |

## Storage

All player data (levels, XP, kill counts, active tasks, contracts) is persisted through **SwagAPI's shared database service**, not local files. SwagSlayer stores data in two tables — `slayer_profiles` (one row per player per slayer type) and `slayer_contracts` (one row per player per contract period). If you're upgrading from a version that predates the SwagAPI migration, any pre-existing per-player YAML files are automatically imported into the shared database on first startup.

## Building from Source

### Prerequisites
- Java JDK 21
- Maven 3.6+

### Build Command

```bash
mvn clean package
```

This runs the shade plugin and outputs the compiled JAR to `target/`.

> `pom.xml` references `libs/SwagAPI-1.0.0.jar` as a system-scoped compile dependency — make sure that jar is present before building.

## Installation

1. Install [SwagAPI](https://github.com/swag617/SwagAPI) first — SwagSlayer will not enable without it.
2. Drop `SwagSlayer.jar` into your server's `plugins/` folder.
3. Start or restart the server, then edit `plugins/SwagSlayer/config.yml` to taste and run `/slayadmin reload`.

Full setup walkthrough: [Installation guide](https://swag617.github.io/SwagSlayer/#/getting-started/installation).

## Project Structure

```
SwagSlayer/
├── pom.xml                                          # Maven build configuration
├── src/main/
│   ├── java/com/swag/swagslayer/
│   │   ├── SwagSlayer.java                          # Main plugin class
│   │   ├── commands/
│   │   │   ├── SlayerCommand.java                   # /slayer command
│   │   │   └── SlayAdminCommand.java                # /slayadmin admin command
│   │   ├── database/
│   │   │   └── DatabaseManager.java                 # SwagAPI-backed persistence
│   │   ├── listeners/
│   │   │   ├── SlayerListener.java                  # Kill tracking, XP, tasks
│   │   │   ├── GUIListener.java                     # GUI click handling
│   │   │   └── PerkListener.java                    # Perk trigger handling
│   │   ├── managers/
│   │   │   ├── DataManager.java                     # In-memory profile cache
│   │   │   ├── SlayerManager.java                   # Slayer type config + progression
│   │   │   ├── LeaderboardManager.java               # Leaderboard computation
│   │   │   ├── CosmeticsManager.java                # Cosmetic rewards
│   │   │   ├── PerkManager.java                     # Perk unlocks
│   │   │   ├── BossManager.java                     # Boss spawning/tracking
│   │   │   ├── BestiaryManager.java                 # Mob bestiary
│   │   │   ├── ContractManager.java                 # Daily/weekly contracts
│   │   │   └── GUIManager.java                      # GUI construction/wiring
│   │   └── models/
│   │       ├── SlayerProfile.java                   # Player progression data
│   │       ├── SlayerType.java                      # Slayer type definitions
│   │       ├── SlayerTask.java                      # Kill task data
│   │       ├── BossSpawn.java                       # Active boss spawn data
│   │       └── Contract.java                        # Contract data model
│   └── resources/
│       ├── plugin.yml                               # Plugin metadata
│       └── config.yml                               # Main configuration
└── docs/                                            # Docsify documentation site
```

## Documentation

Full docs (installation, configuration, slayer system, tasks, combo streak, admin commands, permissions, FAQ) are published at **https://swag617.github.io/SwagSlayer/**.

## License

Proprietary — developed for Swag. All rights reserved.

---

**SwagSlayer** v1.1.0 · Built for Paper/Spigot 1.21+ · Java 21

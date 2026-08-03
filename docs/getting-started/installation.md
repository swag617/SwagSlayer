# 📦 Installation

## Requirements

Before installing SwagSlayer, ensure your server meets these requirements:

### Minimum Requirements
* **Minecraft:** 1.21+ (Paper or Spigot)
* **Java:** 17+
* **Permissions Plugin:** Any (LuckPerms recommended)

### Dependencies
SwagSlayer has a **hard dependency on [SwagAPI](https://github.com/swag617/SwagAPI)**, which provides the shared database service all player data is stored in. Install SwagAPI first — SwagSlayer disables itself on startup if SwagAPI's `IDatabaseService` isn't registered.

## Quick Installation

### Step 1: Download Plugin

1. Install [SwagAPI](https://github.com/swag617/SwagAPI) first — SwagSlayer will not enable without it.
2. Download the latest `SwagSlayer.jar` from the GitHub releases page
3. Verify the file is for your Minecraft version

### Step 2: Install Plugin

1. **Stop your server** (required)
2. Place `SwagSlayer.jar` in your `plugins/` folder
3. **Start your server**

The plugin will automatically:
* Generate `config.yml` with default values
* Connect to SwagAPI's shared database and create its tables (`slayer_profiles`, `slayer_contracts`)
* Load all four slayer types

### Step 3: Verify Installation

Run this command in-game as an admin:

```
/slayadmin reload
```

You should see:

```
SwagSlayer config reloaded.
```

Then open your slayer menu to confirm everything is working:

```
/slayer
```

A 54-slot GUI should open showing all four slayer types.

## File Structure

After installation, you'll find:

```
plugins/SwagSlayer/
└── config.yml           # Main configuration (XP, thresholds, combo settings)
```

Player data (levels, XP, kill counts, active tasks/contracts) is **not** stored as local files — it lives in SwagAPI's shared database, in the `slayer_profiles` and `slayer_contracts` tables. If you're upgrading from a version that predates the SwagAPI migration, any existing `plugins/SwagSlayer/data/*.yml` and `contracts/*.yml` files are automatically imported into the shared database the first time the new version starts.

## Updating

### From an Earlier Version

1. **Stop server**
2. **Back up your database** (SwagAPI's SQLite file, or your MySQL database, depending on how SwagAPI is configured)
3. Replace `SwagSlayer.jar`
4. **Start server**
5. Check console for any warnings

> **Always back up your database before updating!**

## Troubleshooting Installation

### Plugin Won't Load

**Issue:** Plugin appears red in `/plugins` list or fails to load

**Solutions:**
1. Check Java version: `java -version` (must be 17+)
2. Check server type: must be Paper or Spigot 1.21+
3. Check console for error output
4. Ensure no conflicting plugins register the same commands

### Commands Not Found

**Issue:** `/slayer` or `/slayadmin` return "Unknown command"

**Solutions:**
1. Confirm the plugin loaded: `/plugins` should show SwagSlayer in green
2. Reload with `/slayadmin reload`
3. Restart the server fully (not just reload)

### Player Data Not Saving

**Issue:** Player stats reset on relog

**Solutions:**
1. Confirm SwagAPI is installed and loaded **before** SwagSlayer — check the console for "Hooked SwagAPI IDatabaseService." on startup
2. Look for SQL exceptions in console during gameplay or at shutdown
3. Verify SwagAPI's own database configuration (SQLite file path or MySQL credentials) is correct and writable

## Next Steps

[Quick Start](quick-start.md)

[Configuration](configuration.md)

[Slayer System](../core-features/slayer-system.md)

# 📦 Installation

## Requirements

Before installing SwagSlayer, ensure your server meets these requirements:

### Minimum Requirements
* **Minecraft:** 1.21+ (Paper or Spigot)
* **Java:** 17+
* **Permissions Plugin:** Any (LuckPerms recommended)

### Dependencies
SwagSlayer has **no external plugin dependencies**. Drop the jar and go.

## Quick Installation

### Step 1: Download Plugin

1. Download the latest `SwagSlayer.jar` from the GitHub releases page
2. Verify the file is for your Minecraft version

### Step 2: Install Plugin

1. **Stop your server** (required)
2. Place `SwagSlayer.jar` in your `plugins/` folder
3. **Start your server**

The plugin will automatically:
* Generate `config.yml` with default values
* Create the player data directory
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
├── config.yml           # Main configuration (XP, thresholds, combo settings)
└── data/                # Per-player JSON profiles
    ├── <uuid>.json
    └── ...
```

## Updating

### From an Earlier Version

1. **Stop server**
2. **Back up your data folder:**
   ```
   plugins/SwagSlayer/data/
   ```
3. Replace `SwagSlayer.jar`
4. **Start server**
5. Check console for any warnings

> **Always back up player data before updating!**

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
1. Check that the `plugins/SwagSlayer/data/` directory exists and is writable
2. Look for `IOException` or `FileNotFoundException` in console at shutdown
3. Ensure the server has write permissions to the plugins folder

## Next Steps

[Quick Start](quick-start.md)

[Configuration](configuration.md)

[Slayer System](../core-features/slayer-system.md)

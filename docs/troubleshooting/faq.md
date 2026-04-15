# ❓ FAQ

Frequently asked questions about SwagSlayer.

## General

### Does SwagSlayer require any other plugins?

No. SwagSlayer has zero external dependencies. Drop the jar into your `plugins/` folder and start the server.

### What Minecraft versions are supported?

SwagSlayer targets **Paper/Spigot 1.21+**. It may work on earlier 1.20.x versions but is not tested or supported on them.

### Does SwagSlayer work with proxy setups (BungeeCord / Velocity)?

SwagSlayer runs purely on the backend (game) server and stores data locally in `plugins/SwagSlayer/data/`. If you run multiple backend servers, player data is **not shared** between them — each backend has its own profile store. Cross-server sync is not currently supported.

---

## Progression

### Why did my XP go up but my level didn't change?

Levels are determined by **cumulative XP thresholds**. Earning XP advances you toward the next level — you won't level up until your total XP meets the threshold. Check your progress with `/slayer` → click the type icon → see "Kills to next level."

### Can I lose XP or levels?

No. XP only increases (through kills and task completion). Levels only advance. The only way to lose progress is if an admin runs `/slayadmin reset <you>`.

### My kill counts in the GUI don't match my actual kills. Why?

Kill count is tracked from when SwagSlayer was first installed. Kills that happened before the plugin was installed, or while the plugin was unloaded, are not counted.

### Why does the progress bar show 100% but it says I'm not at max level?

This is a display edge case that can occur if your XP is very close to the threshold boundary. It resolves on the next XP gain.

---

## Tasks

### Can I cancel an active task?

There is no built-in cancel command. Once a task is assigned, it remains active until completed. Admins can use `/slayadmin reset <player>` to wipe all data including active tasks, but this resets everything.

### Do task kills count toward my XP even if I don't have a task?

Yes. XP is always earned from every qualifying kill regardless of whether a task is active. Tasks provide a structured goal and a bonus XP reward on completion — they don't gate regular progression.

### I completed my task but didn't get the bonus XP message. Did I get the XP?

If the task completion message appeared (`Task complete! You earned 100 XP!`) then the XP was awarded. If the server was under heavy load, the message may not have rendered but the XP was still applied. Check your totals with `/slayer`.

---

## Combo Streak

### Does the combo streak carry over between sessions?

No. Streaks are stored in memory only and reset when the player disconnects. You start each session at streak 1 (1.0× multiplier).

### Does killing the same mob type repeatedly count as a combo?

Yes. Any kill on any tracked mob type within the timeout window increments your streak.

### How do I know what my current streak is?

There is no in-game HUD for the streak value in the current version. The multiplier is applied silently to each kill. A streak display is planned for a future update.

---

## Server Owners

### Can I add more slayer types beyond the four built-in ones?

Not without modifying the source code. The `SlayerType` enum is defined in Java and new types require a code change and recompile. Custom mob type support is planned for a future release.

### How do I back up player data?

Copy the entire `plugins/SwagSlayer/data/` directory. Each player's profile is stored as `<uuid>.json` and can be restored by placing it back in the same directory.

### Does `/slayadmin reload` require a restart?

No. `/slayadmin reload` applies all `config.yml` changes live. The only thing that requires a full restart is changing the plugin JAR itself.

### Can I set a player's level from console?

Yes. All `/slayadmin` commands work from the server console. Example:

```
slayadmin setlevel Steve ZOMBIE 5
```

(No `/` prefix when running from console.)

---

## Related Pages

* [Support](support.md) — get additional help
* [Admin Commands](../server-owners/admin-commands.md) — command reference
* [Configuration Reference](../server-owners/configuration.md) — config options

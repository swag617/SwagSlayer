# 🖥️ GUI

SwagSlayer's GUI system provides interactive inventory menus for viewing slayer stats, managing tasks, and browsing leaderboards. All screens are 54-slot inventories with a black glass pane border.

## Opening the GUI

```
/slayer
```

Any player with `swagslayer.use` (default: all players) can open the main menu.

## Main Menu

**Title:** `SwagSlayer Menu`

The main menu shows all four slayer types at a glance and provides access to the overall leaderboard.

### Layout

```
[border border border border border border border border border]
[border] Zombie  [border] Spider  [border] Skeleton [border] Creeper [border]
[border border border border border border border border border]
[border border border border border border border border border]
[border border border border border border border border border]
[border border border border border border border] Leaderboard [border]
```

| Slot | Item | Action |
|------|------|--------|
| 10 | Rotten Flesh — Zombie Slayer | Opens Zombie detail screen |
| 12 | String — Spider Slayer | Opens Spider detail screen |
| 14 | Bone — Skeleton Slayer | Opens Skeleton detail screen |
| 16 | Gunpowder — Creeper Slayer | Opens Creeper detail screen |
| 49 | Nether Star — Leaderboards | Opens overall leaderboard |

### Type Icons

Each type icon shows:
* **Display name** (e.g., "Zombie Slayer")
* Level
* Total XP
* Total kills
* "Click to view details" hint

## Type Detail Screen

**Title:** The slayer type's display name (e.g., `Zombie Slayer`)

The detail screen gives a full breakdown of one type along with task management and leaderboard access.

### Layout

```
[border border border border border border border border border]
[border border border border  Icon  border border border border]
[border border border border border border border border border]
[border border border border border border border border border]
[border border border border border border border border border]
[border border border border border border border border border]
```

| Slot | Item | Description |
|------|------|-------------|
| 13 | Type material (e.g. Rotten Flesh) | Stats: level, XP, kills, progress bar, kills to next level |
| 29 | Emerald **or** Compass | Task button (see below) |
| 33 | Paper | Opens the type-specific leaderboard |
| 48 | Arrow | Back to main menu |

### Progress Bar

The detail icon lore includes a 20-character progress bar:
```
Progress: ████████░░░░░░░░░░░░
```
Green blocks `█` represent completed progress; gray blocks `░` represent remaining XP to the next level. At max level the bar is fully filled.

### Task Button

* **No active task** → green Emerald with "Get Task" label. Clicking assigns a new level-scaled task.
* **Active task** → Compass with kill progress (`7/18 kills`). Clicking does nothing (task shown for reference only).

## Leaderboard Screen

**Title:** `Leaderboard - <type name>` or `Leaderboard - Overall`

Displays the top 10 players by XP for a specific type or combined XP across all types.

### Layout

Player-head icons fill slots 10–16 and 19–21 (up to 10 entries):

| Rank | Slot |
|------|------|
| 1 | 10 |
| 2 | 11 |
| 3 | 12 |
| 4 | 13 |
| 5 | 14 |
| 6 | 15 |
| 7 | 16 |
| 8 | 19 |
| 9 | 20 |
| 10 | 21 |

Each player-head shows:
* `#1 PlayerName` as the display name
* `XP: 3700` in the lore

| Slot | Item | Action |
|------|------|--------|
| 49 | Arrow | Back (to type detail or main menu, depending on navigation path) |

## Navigation Flow

```
Main Menu
  └── Type Detail (Zombie / Spider / Skeleton / Creeper)
        └── Leaderboard (type-specific)
              └── Back → Type Detail
  └── Overall Leaderboard
        └── Back → Main Menu
```

The Back button always returns to the previous screen correctly, preserving context.

## Related Pages

* [Slayer Tasks](tasks.md) — assigning and completing tasks via GUI
* [Slayer System](slayer-system.md) — stats shown in the GUI
* [Permissions](../server-owners/permissions.md) — controlling GUI access

# Welcome to SwagSlayer

> **SwagSlayer** is a custom mob-slayer progression system for Minecraft servers running Paper/Spigot 1.21+

## What is SwagSlayer?

SwagSlayer turns mob combat into a full progression experience. Players level up per mob type, complete scaled kill tasks, build combo streaks for XP bonuses, and compete on type-specific leaderboards — all tracked persistently per player.

* **Slayer Progression** — 4 mob types (Zombie, Spider, Skeleton, Creeper), each with 5 independent levels
* **Kill Tasks** — Assigned tasks per type that scale with your current level; completion earns bonus XP
* **Combo Streak** — Kill mobs rapidly to earn up to 2× XP; streak decays after 10 seconds of inactivity
* **GUI** — Interactive inventory menus: main overview, per-type detail view, per-type and overall leaderboards
* **Admin Tools** — Full `/slayadmin` suite for managing player data, with hot-reloadable config

## Core Philosophy

### Per-Type Progression
Every mob type is tracked independently. A level 5 Zombie Slayer has no bearing on their Spider Slayer rank — every type must be leveled through its own kills. This keeps progression feeling meaningful across all content.

### Streaks Reward Aggression
Killing mobs rapidly rewards players with up to a 2× XP multiplier. Players who fight carefully but continuously are rewarded, while those who wait out long timers lose their streak. It creates natural tension in how players approach mob encounters.

### Configurable Without Recompiling
Kill thresholds, XP per kill, boss XP rewards, combo timeout, and multiplier step are all in `config.yml`. Server operators can tune everything on live servers with `/slayadmin reload`.

## Quick Links

| Feature | Description | Link |
|---------|-------------|------|
| **Installation** | Get running in 5 minutes | [Installation Guide](getting-started/installation.md) |
| **Slayer System** | How progression works | [Slayer System](core-features/slayer-system.md) |
| **Tasks** | Kill task mechanics | [Slayer Tasks](core-features/tasks.md) |
| **Combo Streak** | XP multiplier system | [Combo Streak](core-features/combo-streak.md) |
| **Admin Commands** | Server management | [Admin Commands](server-owners/admin-commands.md) |
| **Roadmap** | What's coming next | [Roadmap](resources/roadmap.md) |

## Development Status

> **Version:** 1.0.0 &nbsp;|&nbsp; **Minecraft:** 1.21+ (Paper/Spigot) &nbsp;|&nbsp; **Status:** Active Development

<div style="background:#161616;border:1px solid #2a2a2a;border-radius:14px;padding:28px 28px 22px;margin:8px 0 28px;font-family:inherit">
  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
    <span style="color:#c0c0c0;font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase">Development Progress</span>
    <span style="background:linear-gradient(135deg,#667eea,#764ba2);-webkit-background-clip:text;-webkit-text-fill-color:transparent;font-size:13px;font-weight:700">1 of 5 phases complete</span>
  </div>
  <div style="height:5px;background:#2a2a2a;border-radius:3px;margin-bottom:22px;overflow:hidden">
    <div style="height:100%;width:20%;background:linear-gradient(90deg,#667eea,#764ba2);border-radius:3px"></div>
  </div>
  <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(148px,1fr));gap:10px">
    <div style="background:#1a1230;border:1px solid #764ba2;border-radius:10px;padding:14px 12px;text-align:center">
      <div style="width:32px;height:32px;border-radius:50%;background:linear-gradient(135deg,#667eea,#764ba2);display:flex;align-items:center;justify-content:center;margin:0 auto 10px;font-size:15px;color:#fff;font-weight:700">✓</div>
      <div style="color:#e0e0e0;font-size:12px;font-weight:700;margin-bottom:3px">Phase 1</div>
      <div style="color:#9b8ec4;font-size:11px;line-height:1.4">Core Systems &amp; Admin Tools</div>
    </div>
    <div style="background:#1e1e1e;border:1px solid #383838;border-radius:10px;padding:14px 12px;text-align:center;opacity:.6">
      <div style="width:32px;height:32px;border-radius:50%;background:#2a2a2a;border:2px solid #444;display:flex;align-items:center;justify-content:center;margin:0 auto 10px;font-size:15px;color:#555">⬡</div>
      <div style="color:#777;font-size:12px;font-weight:700;margin-bottom:3px">Phase 2</div>
      <div style="color:#555;font-size:11px;line-height:1.4">Boss Encounters</div>
    </div>
    <div style="background:#1e1e1e;border:1px solid #383838;border-radius:10px;padding:14px 12px;text-align:center;opacity:.6">
      <div style="width:32px;height:32px;border-radius:50%;background:#2a2a2a;border:2px solid #444;display:flex;align-items:center;justify-content:center;margin:0 auto 10px;font-size:15px;color:#555">⬡</div>
      <div style="color:#777;font-size:12px;font-weight:700;margin-bottom:3px">Phase 3</div>
      <div style="color:#555;font-size:11px;line-height:1.4">Custom Rewards</div>
    </div>
    <div style="background:#1e1e1e;border:1px solid #383838;border-radius:10px;padding:14px 12px;text-align:center;opacity:.6">
      <div style="width:32px;height:32px;border-radius:50%;background:#2a2a2a;border:2px solid #444;display:flex;align-items:center;justify-content:center;margin:0 auto 10px;font-size:15px;color:#555">⬡</div>
      <div style="color:#777;font-size:12px;font-weight:700;margin-bottom:3px">Phase 4</div>
      <div style="color:#555;font-size:11px;line-height:1.4">Guilds &amp; Parties</div>
    </div>
    <div style="background:#1e1e1e;border:1px solid #383838;border-radius:10px;padding:14px 12px;text-align:center;opacity:.6">
      <div style="width:32px;height:32px;border-radius:50%;background:#2a2a2a;border:2px solid #444;display:flex;align-items:center;justify-content:center;margin:0 auto 10px;font-size:15px;color:#555">⬡</div>
      <div style="color:#777;font-size:12px;font-weight:700;margin-bottom:3px">Phase 5</div>
      <div style="color:#555;font-size:11px;line-height:1.4">Polish &amp; Achievements</div>
    </div>
  </div>
</div>

## Feature Highlights

### Slayer Types
Four independent progression tracks:
* **Zombie Slayer** — the entry-point type, great for new players
* **Spider Slayer** — precision fighting, small hitboxes
* **Skeleton Slayer** — ranged-mob hunting challenge
* **Creeper Slayer** — high-risk, high-patience encounters

### Kill Tasks
A dynamic task system that scales with level:
* Level 1 tasks: 15–25 kills
* Level 2 tasks: 30–50 kills
* Level 3 tasks: 60–100 kills
* Level 4 tasks: 120–200 kills
* Level 5 tasks: 200–350 kills

### Combo Streak
Rapid-kill XP multiplier:
* Each kill within 10 seconds increments the streak
* Multiplier formula: `1.0 + (streak - 1) × 0.10`, capped at `2.0×`
* Streak resets to 1 after the timeout window

### Leaderboard GUI
* Overall leaderboard ranked by combined XP across all types
* Per-type leaderboard for targeted competition
* Player-head icons for each top-10 entry

## Credits

**Developer:** Swag
**Built With:** Java 17+, Paper API

## License

SwagSlayer is proprietary software developed for Swag.
All rights reserved © 2026

---

> **Need Help?** Check out our [FAQ](troubleshooting/faq.md) or [Support](troubleshooting/support.md) page!

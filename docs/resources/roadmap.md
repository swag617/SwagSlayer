# 🗺️ Roadmap

SwagSlayer's development is organized into phases. Phase 1 is complete and shipped. The remaining phases represent planned features — order and scope are subject to change based on feedback.

## Progress Overview

> **1 of 5 phases complete**

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

---

## Phase 1 — Core Systems & Admin Tools ✓

**Status:** Complete

Everything needed for a fully functional slayer plugin:

* 4 slayer types (Zombie, Spider, Skeleton, Creeper)
* Per-type XP accumulation with configurable thresholds
* 5 levels per type with level-up notifications
* Kill count tracking (informational)
* Combo streak system with up to 2× XP multiplier
* Level-scaled kill tasks with bonus XP on completion
* Interactive 54-slot GUI: main menu, type detail, leaderboards
* Per-type and overall leaderboards with player-head icons
* Persistent player profiles (JSON, auto-save on shutdown)
* Full `/slayadmin` suite: `reload`, `setlevel`, `addxp`, `reset`
* Hot-reloadable config without server restart

---

## Phase 2 — Boss Encounters *(Planned)*

Summoning a boss mob as a climax to a completed task chain:

* After completing a certain number of tasks, unlock a boss encounter
* Boss mob spawns near the player (scaled stats based on level)
* Defeating the boss grants a large XP bonus and potentially cosmetic rewards
* Boss encounters are instanced — only the triggering player can deal progress damage

---

## Phase 3 — Custom Rewards *(Planned)*

Configurable rewards tied to level milestones:

* Server-defined reward commands run when a player reaches a specific level for a type
* Example: give a special item, grant a rank, or run any console command on level-up
* Rewards configured per type and per level in `config.yml`
* Broadcast optional announcement when a player hits a milestone

---

## Phase 4 — Guilds & Parties *(Planned)*

Social features for cooperative slaying:

* Party system: group XP sharing (split evenly among nearby party members)
* Guild system: guild-level leaderboards and shared kill tracking
* Guild challenges: complete a combined kill goal as a group for bonus rewards
* Guild management commands: invite, kick, leave

---

## Phase 5 — Polish & Achievements *(Planned)*

Quality-of-life improvements and long-term engagement features:

* Achievement system: milestone badges awarded for kill counts, level caps, task streaks
* PlaceholderAPI integration: expose slayer stats for scoreboards and other plugins
* Combo HUD: optional actionbar display showing current streak and multiplier
* Streak leaderboard: compete for the longest combo streak achieved
* Custom mob support: hook any mob EntityType to a configurable slayer category

---

## Feedback

Have a feature request or priority suggestion? Open an issue or reach out on Discord:

* [GitHub Issues](https://github.com/swag617/SwagSlayer/issues)
* [Discord](https://discord.gg/z5XZvPQUbG)

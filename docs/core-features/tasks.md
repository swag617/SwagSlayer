# 📋 Slayer Tasks

Slayer tasks are kill assignments tied to a specific mob type. Players request a task through the GUI, complete it by killing the target mob, and earn a bonus XP reward on completion.

## How Tasks Work

1. Open `/slayer` and click a type icon to go to the detail screen
2. Click the **Get Task** button (green emerald — slot 29)
3. A kill goal is randomly assigned based on your current level
4. Kill the target mob type until the goal is reached
5. On completion, bonus XP is awarded and the task clears

## Task Scaling

Kill goals are randomized within a range determined by your **current level** for that type:

| Level | Kill Goal Range |
|-------|----------------|
| 1 | 15 – 25 kills |
| 2 | 30 – 50 kills |
| 3 | 60 – 100 kills |
| 4 | 120 – 200 kills |
| 5 (max) | 200 – 350 kills |

The kill goal is randomly chosen within the range at the moment you request the task.

## One Task Per Type

A player can have **one active task per slayer type** at a time. Attempting to get a second task for the same type while one is active shows:

```
You already have an active Zombie Slayer task!
```

Tasks for different types are completely independent — you can work on a Spider task and a Skeleton task simultaneously.

## Task Progress

Every qualifying kill increments your active task and displays progress in chat:

```
[Zombie Slayer Task] 7/18
```

The progress message appears on every kill while a task is active.

## Task Completion

When kills completed equals the kill goal:

```
Task complete! You finished your Zombie Slayer task and earned 100 XP!
```

The bonus XP (`boss_xp_reward` in config, default `100`) is added immediately and can trigger a level-up.

## Task Display in GUI

The type detail screen (slot 29) shows your task status:

**No active task:**
> **Get Task** *(green emerald)*
> Click to receive a slayer task

**Active task:**
> **Zombie Slayer Task** *(compass)*
> `7/18 kills`
> Kill mobs to progress

## Configuration

Task kill goals are scaled by level in code. The bonus XP on completion is configurable per type:

```yaml
slayer_types:
  ZOMBIE:
    boss_xp_reward: 100   # XP awarded on task completion
```

## Tips

* Higher levels give harder tasks but the same `boss_xp_reward` — the value of tasks comes from the XP you earn during completion, not just the reward
* Combo streaks apply to every kill during a task — chain kills together for bonus XP
* Any kill on the correct mob type counts, regardless of location or difficulty

## Related Pages

* [Slayer System](slayer-system.md) — XP and leveling
* [Combo Streak](combo-streak.md) — boost XP while completing tasks
* [GUI](gui.md) — how to navigate the task interface

# 🔥 Combo Streak

The combo streak system rewards players who kill mobs rapidly with an XP multiplier. The longer the streak, the more XP each kill earns — up to a maximum of **2×**.

## How It Works

Each time a player kills a tracked mob (any slayer type), the system checks when their last kill occurred:

* If the gap is **within the timeout window** (default 10 seconds): streak increments by 1
* If the gap **exceeds the timeout**: streak resets to 1

The streak count is then used to calculate an XP multiplier applied to the kill.

## Multiplier Formula

```
multiplier = 1.0 + (streak - 1) × combo_multiplier_per_streak
multiplier = min(multiplier, 2.0)
```

With the default `combo_multiplier_per_streak: 0.1`:

| Streak | Multiplier |
|--------|-----------|
| 1 | 1.0× *(baseline — no bonus)* |
| 2 | 1.1× |
| 3 | 1.2× |
| 5 | 1.4× |
| 7 | 1.6× |
| 10 | 1.9× |
| 11 | 2.0× *(cap)* |
| 12+ | 2.0× *(stays capped)* |

## XP Calculation

The multiplier is applied to the base XP per kill:

```
earned_xp = round(xp_per_kill × multiplier)
```

With `xp_per_kill: 10` and a streak of 5:
```
earned_xp = round(10 × 1.4) = 14 XP
```

At the 2× cap (streak ≥ 11):
```
earned_xp = round(10 × 2.0) = 20 XP
```

## Cross-Type Streaks

The streak is **not type-specific**. Any kill on any tracked mob type refreshes the timer and increments the same streak counter. This means:

* Killing a Zombie then a Spider within 10 seconds keeps your streak alive
* You can maintain a high streak while working on different types
* The multiplier applies to whichever type's kill just occurred

## Streak Reset

The streak resets to 1 when:
* More than `combo_timeout_seconds` pass without a qualifying kill
* The player disconnects (streak state is not persisted between sessions)

On reset, the very next kill starts a new streak at 1 (1.0× multiplier).

## Configuration

```yaml
general:
  combo_timeout_seconds: 10          # timeout window in seconds
  combo_multiplier_per_streak: 0.1   # XP bonus per streak kill
```

| Setting | Effect |
|---------|--------|
| Increase `combo_timeout_seconds` | Easier to maintain a streak — more forgiving |
| Decrease `combo_timeout_seconds` | Harder — players must kill rapidly |
| Increase `combo_multiplier_per_streak` | Faster multiplier growth (e.g. `0.2` caps at streak 6) |
| Decrease `combo_multiplier_per_streak` | Slower growth, more sustained benefit |

> With `combo_multiplier_per_streak: 0.2`, the 2× cap is hit at a streak of 6.
> With `combo_multiplier_per_streak: 0.05`, the 2× cap requires a streak of 21.

## Tips for Players

* Fight densely packed mobs (dungeons, mob farms) to maintain a streak
* Don't stop to loot mid-fight — pick up items after the kill chain
* Killing any tracked mob type resets the timer, so switch types freely during a grind session

## Related Pages

* [Slayer System](slayer-system.md) — XP accumulation and leveling
* [Slayer Tasks](tasks.md) — combo applies during task progress
* [Configuration Reference](../server-owners/configuration.md) — tune the combo settings

# Rogue Mode Plan

## Overview

A rogue-like game mode for Pairs (连连看) where the player is given a total time budget (10 minutes) and must clear as many levels as possible before time runs out. Score, remaining time, and unused items persist across levels.

## Game Flow

```
MainMenu ──▶ StagePage ──▶ PlayPage ──▶ ShopPage ──▶ StagePage ──▶ ...
                      ▲                    │
                      └──── (loop) ────────┘
                                           └── (time out) ──▶ ResultPage
```

1. **StagePage** — previews the upcoming level (difficulty, round number). Placeholder: just Start and Quit buttons.
2. **PlayPage** — the actual 连连看 gameplay using the existing LevelComponent.
3. **ShopPage** — spend score on items between rounds. Placeholder: just a Continue button.
4. **ResultPage** — final stats when time runs out (total score, rounds survived, time survived).

A TransitionPage (animated transition between levels) slot is reserved for future implementation.

## Key Design Decisions

### Shared Session Object

A `RogueSession` object holds all cross-level state:
- Total remaining time (starts at 10:00)
- Accumulated score
- Current round number
- Unused item counts

Every page in the rogue flow receives the same session instance. This avoids global state while keeping data accessible across page transitions.

### LevelComponent Integration (Minimal)

LevelComponent already has a flexible timer system. The rogue mode reuses it by:

- Adding a new constructor overload that accepts `RogueSession` (reading `session.remainingMs` as the initial countdown value).
- Exposing `isCleared()` and `isTimedOut()` observation methods — no callbacks.

PlayPage polls these flags in its own `update()` loop. When it detects level completion, it synchronizes state back to the session (remaining time, score delta, item counts) and navigates to the next page.

### State Synchronization Boundary

Session state is synced at three points:
1. **Level cleared** — PlayPage polls `isCleared()`, syncs, then transitions to ShopPage after a brief delay.
2. **Time out** — PlayPage polls `isTimedOut()`, syncs, then navigates directly to ResultPage.
3. **PlayPage exit** — `onExit()` syncs as a safety net (e.g., player clicks Home mid-level).

A `synced` flag prevents double-sync if multiple sync paths trigger.

### Timer Behavior During Transitions

Each level's `LevelComponent` is initialized with `session.remainingMs` and runs its own `CountdownState`. When the level is cleared, the `cleared` flag freezes the internal timer automatically. The gap between clear and the next level's start (overlay + page switch + entry animation) is effectively paused since no active timer is ticking. The next level's LevelComponent picks up from the synced `session.remainingMs`.

### Item Persistence

Items persist across levels by copying `RogueSession`'s `ItemCountMap` into each new `GameState` at construction time, and writing the level-end counts back on sync.

### Difficulty Progression

Phase 1 uses the "easy" preset for all levels. Phase 3 replaces this with a dynamic difficulty system driven by a point-based formula (see Phase 3).

### Page 3 Slot

A `TransitionPage` is reserved as a placeholder in the flow (PlayPage → TransitionPage → ShopPage). No implementation yet; PlayPage currently navigates directly to ShopPage on clear.

## Files to Create

- `model/RogueSession.java` — cross-level state
- `router/RogueStagePage.java` — level preview (placeholder)
- `router/RoguePlayPage.java` — gameplay host, owns LevelComponent + session sync
- `router/RogueShopPage.java` — inter-level shop (placeholder)
- `router/RogueResultPage.java` — final stats screen

## Files to Modify

- `view/LevelComponent.java` — add `RogueSession` constructor overload + `isCleared()`/`isTimedOut()` getters
- `view/MainMenuComponent.java` — add Rogue button
- `router/MainMenuPage.java` — add `startRogue()` handler

## Out of Scope (Not Changed)

`GameState`, `CountdownState`, `ItemCountMap`, `DifficultyPage`, `GameStatus`, `Router`, `ItemType`

## Acceptance Criteria (Phase 1)

1. Starting rogue mode from the main menu creates a session with 10:00 on the clock.
2. Each level is a playable easy-preset 连连看 board with a fresh random seed.
3. Items (Auto Solver, TNT) are carried over between levels; unused items from a previous level are available in the next.
4. Score accumulates across levels — the total is shown on the result page.
5. When a level is cleared, the remaining time carries over to the next level.
6. When the total time reaches 0, the game ends and the result page shows the correct final score, rounds survived, and time survived.
7. Exiting rogue mode mid-level (Home button, Quit on StagePage) discards the session.
8. The result page displays total score, rounds survived, and time played.
9. The StagePage and ShopPage have functional Continue/Start buttons and serve as valid navigation waypoints.

---

# Phase 2: Shop

## Overview

Phase 2 replaces the placeholder ShopPage with a functioning shop where the player spends score to buy items between rounds. Two score variables are tracked to support spending while preserving the total earned for the final tally.

## Score Model Change

The session's single `totalScore` is split into two variables:

- **Spendable score** — earned from level clears, decreased by shop purchases, carried to the result page as the unspent portion.
- **Cumulative spent** — the sum of all score ever spent in shops across the entire run.

At the result page, `totalScore = spendable + cumulativeSpent` reconstructs the full earnings. The play page syncs the level's delta into `spendableScore` only.

## Item Pricing

Each item type has a base cost and a growth rate. The price of a given item increases each time it is purchased:

```
cost = baseCost × growthRate^purchaseCount
```

Where `purchaseCount` is the number of times that item type has been bought so far in the current run (0-indexed, so first purchase costs `baseCost × growthRate⁰ = baseCost`).

### Default Parameters

| Item | Base Cost | Growth Rate |
|------|-----------|-------------|
| Auto Solver | 2,000 | ×1.5 |
| TNT | 3,000 | ×1.5 |
| +30s Time | 5,000 | ×1.5 |

These values are tunable constants in `RogueSession`. The growth rate applies per type independently, so buying many Auto Solvers does not make TNT or Time more expensive.

## Session Changes

`RogueSession` gains:
- `int spendableScore` (renamed from `totalScore`)
- `int cumulativeSpent`
- `Map<ItemType, Integer> purchaseCounts` — tracks how many times each item type has been bought
- `int timePurchases` — count of time purchases made (for pricing)
- `int getItemCost(ItemType type)` — computes current price from base, growth rate, and purchase count
- `int getTimeCost()` — computes current time price from base, growth rate, and `timePurchases`
- `void purchaseTime()` — adds 30s to `remainingMs`, deducts cost, increments `cumulativeSpent` and `timePurchases`
- `int getTotalEarned()` — returns `spendableScore + cumulativeSpent`
- Static constants for base costs and growth rate

Item persistence across levels is unchanged; the `items` map still holds the player's inventory.

## Shop UI

The ShopPage shows:

1. **Score display** — current spendable score prominently at the top.
2. **Purchaseable rows** — one per item/time option, each showing:
   - Icon (reuse existing icon system for items; clock icon for time)
   - Name / description ("Auto Solver", "TNT", "+30s Time")
   - Current price
   - Buy button (disabled if spendable score is insufficient)
3. **Continue button** — advances to the next round.

Time purchase does not interact with the item inventory. It adds 30 seconds (30,000ms) to `session.remainingMs` directly. Its purchase count is tracked separately from item purchase counts.

The shop layout is implemented as a new `view/ShopComponent` widget (following the existing component pattern), or built directly into the page if the layout is simple enough. Purchasing triggers an immediate deduction from `spendableScore`, increments `purchaseCount` for that row, and applies the effect (item grant or time addition).

## Pricing Visibility

The current price is computed from the purchase count, not from the player's inventory. This means:
- Even if you already have 3 Auto Solvers, buying another costs the same as if you had 0.
- The cost depends only on how many you have already bought during this run, not how many you currently hold.

This avoids exploits where a player could horde items to keep prices low.

## Files to Create (Phase 2)

- `view/ShopComponent.java` — shop UI widget

## Files to Modify (Phase 2)

- `model/RogueSession.java` — split score variables, add purchase tracking and cost computation
- `router/RogueShopPage.java` — from placeholder to full implementation
- `router/RoguePlayPage.java` — sync to `spendableScore` instead of `totalScore`
- `router/RogueResultPage.java` — compute final total from `getTotalEarned()`

## Acceptance Criteria (Phase 2)

1. The shop page displays the player's current spendable score.
2. The shop offers Auto Solver, TNT, and +30s Time for purchase, each showing their current price.
3. Prices increase exponentially with each purchase of that type (×1.5 per purchase).
4. Buying an item deducts its cost from spendable score and adds one to the player's inventory.
5. Buying time deducts its cost from spendable score and adds 30 seconds to the remaining time.
6. The buy button is disabled / non-functional when spendable score is below the item's price.
7. Purchases in one shop are reflected in the item inventory and remaining time of subsequent levels.
8. The result page displays total score as `spendableScore + cumulativeSpent`, which equals the sum of all score ever earned during the run.
9. The Continue button advances to the next round (StagePage).

---

# Phase 3: Dynamic Difficulty

## Overview

Phase 3 replaces the static "easy" preset with a procedural difficulty system. Each level's parameters (grid size, type count, visual complexity, pairing strategy) are randomly generated from a target difficulty point that increases with the round number. This creates a natural difficulty curve without manual preset configuration.

## Motivation

A fixed preset is repetitive. By procedurally generating level parameters from a point budget, every run can feel different while still scaling challenge predictably. The same system also powers the stage preview page's info display.

## Difficulty Point Formula

Each level has four independent sub-scores that sum to a total difficulty point value:

### Size Points

Calculated from the number of non-blocked cells in the grid (tile count), which depends on both grid dimensions and whether a shape mask is used.

```
sizePoints = round(tileCount / 10)
```

If shape is not set (i.e., the grid is fully dense with no blocked cells), an additional +1 point is added, since full grids offer more tiles than shaped layouts of the same dimensions.

### Type Variety Points

Based on the number of distinct tile types used on the board:

| Types Range | Points |
|-------------|--------|
| [0, 8]      | 0      |
| [9, 12]     | 1      |
| [13, 16]    | 2      |
| [17, 20]    | 3      |

Maximum of 20 types.

### Visual Complexity Points

Combines slab inclusion and tile spread strategy:

| Factor | Condition | Points |
|--------|-----------|--------|
| Slabs  | includeSlabs = true | +2 |
| Spread | NO_DUPLICATES | 0 |
|        | FREE | +3 |
|        | PREFER_DUPLICATES | +6 |

### Pairing Difficulty Points

| Strategy | Points |
|----------|--------|
| base     | 0      |
| nonAdjacent | 1   |
| distant  | 2      |

### Total

```
totalPoints = sizePoints + typePoints + visualPoints + pairingPoints
```

Maximum: 34 points.

## Target Curve Per Level

The target difficulty points increase linearly with the round number:

```
targetPoints = min(3 + (level - 1) × 3, 34)
```

This produces:

| Level | Target Points | Difficulty Tier |
|-------|---------------|-----------------|
| 1     | 3             | easy            |
| 2     | 6             | normal          |
| 3     | 9             | normal          |
| 4     | 12            | normal          |
| 5     | 15            | hard            |
| 6     | 18            | hard            |
| 7     | 21            | hard            |
| 8     | 24            | hard            |
| 9     | 27            | extreme         |
| 10    | 30            | extreme         |
| 11    | 33            | extreme         |
| 12+   | 34            | extreme         |

## Difficulty Tier Mapping (Scoring Multiplier)

The existing `Tilemap.Difficulty` enum controls the scoring multiplier. The tier is determined by totalPoints range:

| Tier | Points Range | Score Multiplier |
|------|--------------|------------------|
| EASY   | [1, 5]    | ×1               |
| NORMAL | [6, 14]   | ×1 (default)     |
| HARD   | [15, 24]  | ×2               |
| EXTREME| [25, 34]  | ×3               |

This matches the existing `GameState.eliminate()` scoring logic without modification.

## Reference: Existing Presets

For calibration, the current presets map to:

| Preset | Points | Tier   |
|--------|--------|--------|
| easy   | 3      | EASY   |
| medium | 13     | NORMAL |
| hard   | 22     | HARD   |
| extreme| 28     | EXTREME|

## Parameter Generation Strategy

Given a target point value for a level, the generator randomly selects parameters whose computed total points are close to the target. Since all four sub-scores are discrete and the search space is small, the generator accepts slight deviations from the target rather than requiring an exact match.

The generation process:

1. Choose grid size (square, width = height, range [6, 14]).
2. Optionally apply a shape mask (reduces tile count, increases difficulty per dimension).
3. Choose type count from the valid ranges for each point bracket.
4. Choose slab inclusion and spread strategy.
5. Choose pairing strategy.
6. Compute total points; if within acceptable tolerance of the target, accept; otherwise re-roll constrained parameters.

Grid dimensions are always square. Min size 6, max size 14.

## Impact on Existing Flow

The session's `getPresetName()` string-based approach is replaced by a programmatic `TilemapFactory` builder. `RoguePlayPage` calls the difficulty generator each time it creates a level, passing the current `session.level` to compute the target. RogueSession no longer refers to preset names.

The stage preview page (`RogueStagePage`) can use the generated parameters to display difficulty info (points breakdown, tier label, grid size) before the player clicks Start.

## Files to Create (Phase 3)

- `map/RogueDifficultyGenerator.java` — takes a level number, produces a set of board parameters (size, types, slabs, spread, pairing) with a difficulty point value matching the target curve. Also assigns the correct `Tilemap.Difficulty` tier.

## Files to Modify (Phase 3)

- `model/RogueSession.java` — remove `getPresetName()`, add `getTargetPoints()` that computes target from level number
- `router/RoguePlayPage.java` — use `RogueDifficultyGenerator` instead of preset-name-based `TilemapFactory.fromPreset()`
- `router/RogueStagePage.java` — optionally display difficulty info for the upcoming level
- `map/TilemapFactory.java` — may need additional factory methods if `CustomizedTilemapFactory` + `SubsetTilemapFactory` composition is insufficient for all generated parameter combinations

## Acceptance Criteria (Phase 3)

1. Each level's board parameters (grid size, type count, slab inclusion, spread, pairing strategy) are randomly generated, not loaded from a preset JSON.
2. Grid size is always square, minimum 6, maximum 14.
3. The first level has approximately 3 difficulty points (easy tier).
4. Each subsequent level targets approximately +3 points over the previous, up to the cap of 34.
5. The difficulty tier (EASY/NORMAL/HARD/EXTREME) is correctly assigned per the points-to-tier mapping.
6. Score multiplier matches the difficulty tier (same as existing per-preset behavior).
7. The generated board is always solvable (the existing `TilemapGeneratorCore` guarantee is preserved).
8. Deviations from the exact target point are acceptable as long as the upward trend is perceptible across rounds.

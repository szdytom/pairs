# Score System

## Difficulty multiplier

Each eliminated pair awards `SCORE_PER_PAIR = 500` points, scaled by the
difficulty stored on the `Tilemap`:

| `Tilemap.Difficulty` | Multiplier | Points per pair |
|----------------------|-----------|-----------------|
| `EASY`               | ×1        | 500             |
| `HARD`               | ×2        | 1 000           |
| `EXTREME`            | ×3        | 1 500           |
| `NORMAL` (default)   | ×1        | 500             |

Undoing a move deducts the same amount.

The difficulty is set on the `Tilemap` during map generation (see
`TilemapFactory.fromPreset`). It is not mutated afterwards.

## Front-end interface

Call `GameState.getScore()` to read the current score at any time — it
delegates to the internal `GameStatus` and reflects all operated/undone moves.

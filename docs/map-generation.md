# Map Generation — Implementation Overview

## Layer Structure

Map generation is split into four layers, each with a single responsibility:

```
GameState  (logic facade — the only entry point for the frontend)
  └─ MapInitializer  (wires difficulty policy + factory)
       └─ SubsetTilemapFactory  (decorator: remaps type indices → registry IDs)
            └─ PresetTilemapFactory / CustomizedTilemapFactory  (grid layout)
                 └─ TilemapGeneratorCore  (core solvable-pair algorithm)
```

---

## Classes

### `TilemapFactory` (interface)
Single method: `generate() → Tilemap`. Every factory implements this.

---

### `TilemapGeneratorCore`
The core algorithm. Fills a grid of fillable cells with solvable pairs.

1. Collect all zero (fillable) cells, shuffle them randomly.
2. For each candidate pair, verify connectivity via `TileTransition` (straight or L-shaped path).
3. Assign a sequential pair index (1…N) to each matched pair.
4. Build a random mapping from pair index → tile type (1…`types`), guaranteeing every type appears at least once.
5. Return a `Tilemap` with type IDs in each cell.

All randomness comes from an injected `Random` (always a seeded `Xoroshiro128PP`).

---

### `PresetTilemapFactory`
Layout factory for fixed difficulty levels (easy / hard / extreme). Reads a `TilemapPreset` from `assets/manifest.json` (width, height, blocked-cell mask), then calls `TilemapGeneratorCore`.

---

### `CustomizedTilemapFactory`
Layout factory for arbitrary dimensions. Builds an all-zero grid of `width × height` and delegates to `TilemapGeneratorCore`. Used for custom games and unit tests (no asset dependency).

---

### `SubsetTilemapFactory` (decorator)
Wraps any `TilemapFactory`. After the inner factory produces a tilemap with generic type indices (1…N), it remaps every non-zero cell:

```
raw_type  →  subset[raw_type]   (1-based index into a pre-selected palette)
```

This separates *how many* types appear from *which* visual tiles represent them.

---

### `TileSelectionPolicy`
Selects the palette (`int[]` of registry IDs) that `SubsetTilemapFactory` will use.
Configurable by two axes:

| Field | Values |
|---|---|
| `includeSlabs` | `true` / `false` — whether slab-shaped tiles are eligible |
| `spread` | `FREE`, `NO_DUPLICATES`, `PREFER_DUPLICATES` |

**Spread strategies** (all draw from `TileGroupRegistry` similarity groups):

- `FREE` — uniform random draw from all eligible tiles.
- `NO_DUPLICATES` — at most one tile from each visual group → maximally varied palette (easy).
- `PREFER_DUPLICATES` — exhausts groups before moving on → palette full of look-alikes (extreme).

Preset policies:

| Difficulty | `includeSlabs` | `spread` |
|---|---|---|
| easy | false | `NO_DUPLICATES` |
| hard | false | `FREE` |
| extreme | false | `PREFER_DUPLICATES` |

---

### `TileGroup` / `TileGroupRegistry`
`TileGroup` is an immutable record: a list of visually similar tile string IDs and a `slab` flag.
`TileGroupRegistry` is a pure data container. Its contents are loaded from [assets/tile-groups.json](../assets/tile-groups.json) by `TileGroupsOperation` and registered in `AssetManager` under the id `tile-groups/default`.

---

### `MapInitializer`
Facade that combines a layout factory with a palette policy into a ready-to-use `TilemapFactory`.

```java
MapInitializer.hard(seed)
// → loads "tilemap/hard" preset (12×12, 20 types)
// → builds TileSelectionPolicy.hard() (FREE spread)
// → picks 20-tile palette with Xoroshiro128PP(seed)
// → returns SubsetTilemapFactory(PresetTilemapFactory(preset, seed), palette)
```

All public methods come in two forms: no-arg (draws a fresh seed) and `(…, Seed)` for replay.

---

### `GameState` (frontend facade)
The sole entry point for the view layer. Stores `Tilemap`, `OpLogs`, and `Seed`.

```java
GameState.easy()               // random seed
GameState.easy(seed)           // deterministic replay
GameState.hard(seed)
GameState.extreme(seed)
GameState.customized(w, h, t)
GameState.customized(w, h, t, seed)
GameState.customized(w, h, t, includeSlabs, spread)
GameState.customized(w, h, t, includeSlabs, spread, seed)

state.getSeed()                // retrieve seed for replay
```

`customized(w, h, t [, seed])` uses `CustomizedTilemapFactory` directly (no asset loading), so it works in unit tests without `AssetManager`.

---

## Data Flow: `GameState.hard(seed)`

```
GameState.hard(seed)
  → MapInitializer.hard(seed)
      → TileRegistry (from AssetManager: 100 typed tiles)
      → TileGroupRegistry (from AssetManager: "tile-groups/default")
      → TileSelectionPolicy.hard().selectFor(registry, groups, 20, rng)
            [rng = new Xoroshiro128PP(seed)]
            → picks 20 registry IDs  (int[] palette)
      → TilemapPreset "tilemap/hard" (12×12, all-zero grid)
      → PresetTilemapFactory(preset, seed)
      → SubsetTilemapFactory(inner, palette)
  → build(factory, seed)
      → factory.generate()
            → PresetTilemapFactory.generate()
                  → TilemapGeneratorCore.generate(grid, 20, rng)
                        → solvable pair assignment
                  → raw Tilemap (types 1–20)
            → SubsetTilemapFactory remaps types → palette IDs
            → final Tilemap
      → new GameState(tilemap, seed)
```

---

## Seed & Reproducibility

`Seed` is an immutable 128-bit value (`long s0, s1`). `Xoroshiro128PP` extends `java.util.Random` and is seeded from it.

Both palette selection and layout generation use `new Xoroshiro128PP(seed)` independently — two separate RNG streams from the same seed. Calling `GameState.hard(sameSeed)` twice produces identical tilemaps.

`Seed.deviceRandom()` samples from `SecureRandom` for fresh games; `Seed.fromString(hex)` parses a previously stored seed for replay.

---

## Asset Presets (`assets/manifest.json`)

| ID | Type | Notes |
|---|---|---|
| `tilemap/easy` | `tilemap-preset` | 9 × 9, 6 types, L-shaped quadrants (sparse) |
| `tilemap/hard` | `tilemap-preset` | 12 × 12, 20 types, all-zero (dense) |
| `tilemap/extreme` | `tilemap-preset` | 12 × 12, 20 types, all-zero (dense) + `PREFER_DUPLICATES` palette |
| `tile-groups/default` | `tile-groups` | Loaded from [assets/tile-groups.json](../assets/tile-groups.json); similarity catalog |

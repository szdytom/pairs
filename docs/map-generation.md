# Map Generation

## How it works

A map is generated in two phases: **palette selection** (which tile types appear) and **layout** (where they go). Both phases are driven by a deterministic **seed**, so the same seed always reproduces the same map.

```
GameState  ← the only entry point for the frontend
  └─ MapInitializer  ← wires difficulty policy + factory
       └─ SubsetTilemapFactory  ← remaps generic type indices → registry IDs
            └─ PresetTilemapFactory / CustomizedTilemapFactory  ← grid layout
                 └─ TilemapGeneratorCore  ← core solvable-pair algorithm
```

---

## Seed

Every map starts from a `Seed` — an immutable 128-bit value (`long s0, s1`).

```java
Seed.deviceRandom()          // fresh game (OS entropy via SecureRandom)
Seed.fromString("my-puzzle") // deterministic, hashed with SHA-256
new Seed(0xDEADBEEFL, 0xCAFEBABEL) // explicit
```

The PRNG is **Xoroshiro128++** (`app.pairs.utils.Xoroshiro128PP`), which extends `java.util.Random`. Palette selection and layout each get their own `new Xoroshiro128PP(seed)` — two independent streams from the same seed — so calling `GameState.hard(seed)` twice always gives identical results.

**Replay:**
```java
GameState first = GameState.hard();
Seed seed = first.getSeed();       // save this
GameState replay = GameState.hard(seed); // identical map
```

---

## Palette Selection — `TileSelectionPolicy`

Picks `N` tile types from the registry. Two knobs control the character of the palette:

| Knob | Options |
|---|---|
| `includeSlabs` | whether slab-shaped tiles are eligible |
| `spread` | `FREE` · `NO_DUPLICATES` · `PREFER_DUPLICATES` |

Slabs are always drawn separately (up to `MAX_SLABS = 3`) and never bucket into spread groups — a handful of slabs adds spice without flooding the palette.

**Spread strategies** (applied to non-slab tiles, using `TileGroupRegistry` similarity groups):

| Spread | Effect | Used by |
|---|---|---|
| `NO_DUPLICATES` | at most one tile per visual group — maximally varied | easy |
| `FREE` | uniform random draw | hard |
| `PREFER_DUPLICATES` | drains whole groups first — lots of look-alikes | extreme |

**Difficulty presets:**

| | easy | hard | extreme |
|---|---|---|---|
| slabs | ✗ | ✗ | ✓ (≤ 3) |
| spread | `NO_DUPLICATES` | `FREE` | `PREFER_DUPLICATES` |

Similarity groups (`TileGroup` records with a `slab` flag) live in [assets/tile-groups.json](../assets/tile-groups.json), loaded into `AssetManager` as `tile-groups/default`.

---

## Layout — `TilemapGeneratorCore`

Given a grid of fillable cells and a type count, fills the grid with solvable pairs:

1. Shuffle all fillable cells.
2. Greedily pair cells that can connect via a straight or L-shaped path (`TileTransition`).
3. Assign pair indices 1…N.
4. Build a random type mapping (pair → tile type), guaranteeing every type appears at least once.
5. Return a `Tilemap` with type IDs per cell.

**Factories on top of the core:**

- `PresetTilemapFactory` — reads a `TilemapPreset` from `assets/manifest.json` (width, height, blocked-cell mask).
- `CustomizedTilemapFactory` — arbitrary `width × height`, all-zero grid. No asset dependency → usable in unit tests.
- `SubsetTilemapFactory` *(decorator)* — wraps either factory and remaps generic type indices to the chosen palette IDs: `raw_type → subset[raw_type]`.

---

## `GameState` API

The sole entry point for the view layer. Stores `Tilemap`, `OpLogs`, and `Seed`.

```java
// preset difficulties
GameState.easy()   /  GameState.easy(seed)
GameState.hard()   /  GameState.hard(seed)
GameState.extreme() / GameState.extreme(seed)

// custom
GameState.customized(w, h, types)
GameState.customized(w, h, types, seed)
GameState.customized(w, h, types, includeSlabs, spread)
GameState.customized(w, h, types, includeSlabs, spread, seed)

state.getSeed()  // retrieve seed for replay
```

`customized(w, h, t [, seed])` skips asset loading, so it works in unit tests.

---

## Asset presets (`assets/manifest.json`)

| ID | Grid | Types | Notes |
|---|---|---|---|
| `tilemap/easy` | 9 × 9 | 6 | L-shaped quadrants (sparse) |
| `tilemap/hard` | 12 × 12 | 20 | all-zero (dense) |
| `tilemap/extreme` | 12 × 12 | 20 | all-zero (dense), `PREFER_DUPLICATES` palette |
| `tile-groups/default` | — | — | similarity catalog from [tile-groups.json](../assets/tile-groups.json) |

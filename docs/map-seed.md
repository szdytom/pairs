# Seeded Map Generation

Every tilemap is generated from a **128-bit seed** (`app.pairs.utils.Seed`). The same seed always produces the same map, so you can save a seed to share or replay a specific layout.

## Getting a Seed

```java
// 1. Random (default) — from OS entropy
Seed s = Seed.deviceRandom();

// 2. From a string — hashed with SHA-256
Seed s = Seed.fromString("my-level-name");

// 3. Explicit 128-bit value
Seed s = new Seed(0xDEADBEEFL, 0xCAFEBABEL);
```

## Using a Seed with Factories

Both `CustomizedTilemapFactory` and `PresetTilemapFactory` accept an optional seed.
When no seed is given, one is drawn from the OS automatically.

```java
// CustomizedTilemapFactory
Tilemap m = new CustomizedTilemapFactory("daily-puzzle")
    .setWidth(10).setHeight(10).setTypes(8)
    .generate();

// PresetTilemapFactory
Tilemap m = new PresetTilemapFactory(preset, "daily-puzzle").generate();
```

### Replaying a Map

```java
var factory = new CustomizedTilemapFactory(); // random seed chosen here
Tilemap first  = factory.generate();
Seed   used    = factory.getSeed();           // save it

Tilemap replay = new CustomizedTilemapFactory(used).generate(); // identical
```

## The PRNG

Under the hood the generator is **Xoroshiro128++** (`app.pairs.utils.Xoroshiro128PP`), a fast, high-quality 64-bit PRNG by Blackman & Vigna. It extends `java.util.Random` so it works transparently with `Collections.shuffle` and similar standard-library methods.

Four places inside `TilemapGeneratorCore` consume the stream:

| Step | What is randomized |
|------|--------------------|
| Shuffle initial tiles | `Collections.shuffle` on candidate list |
| Pick pair partner | Random choice from connectable candidates |
| Assign extra types | `nextInt(typeCount)` for pairs beyond `typeCount` |
| Shuffle type mapping | Fisher-Yates over the `pairToType` array |

`Xoroshiro128PP` also exposes `jumpShort()` (advances 2⁶⁴ steps) and `jumpLong()` (advances 2⁹⁶ steps) for generating non-overlapping independent streams.

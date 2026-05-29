# Code Skills Summary

| Technique | Where | Why |
|----------|-------|-----|
| Ray-traversal (0-1 BFS) | `Path` | Min-turns pathfinding for lianliankan rules |
| Zobrist hashing | `Solver` | Transposition table for DFS state dedup |
| Forced-move propagation | `Solver` | Prune branches: tiles with only 2 left must be paired |
| Most-constrained-first ordering | `Solver` | Explore scarce tile types first to find solutions faster |
| Reverse-generation for solvability | `TilemapGeneratorCore` | Fill grid then find elimination order backwards |
| Xoroshiro128++ PRNG | `Seed` | Deterministic 128-bit RNG for reproducible boards |
| Precomputed clearance radii | `ClrRadius` | O(1) lookup for 4-directional clearance in elimination checks |
| Depth sorting by row+col | `IsometricGridView` | Back-to-front render order without a z-buffer |
| Double-buffered transitions | `Router` | currentPage/pendingPage avoids mid-frame state mutation |
| Parent-chain walking | `Blackboard` | Walk widget tree upward for shared state — no global registry |
| Event consumption flag | `Event` | stopPropagation-style: consumed events stop bubbling |
| Logical-pixel coordinates | `ScaleManager` | Divide all coords by scale before dispatch; window resize is transparent |
| Property bag | `Widget` | Map<String,Object> props drive layout without subclass explosion |
| Lazy texture creation | `TileRegistry` | Surface→Texture only on first render, non-blocking startup |
| Snapshot records | `model/*Snapshot` | Immutable records decouple serialization from mutable runtime state |
| Soft-delete | `Database` | is_deleted flag instead of real deletion |
| Auto-detect JAR vs filesystem | `AssetLoaderInstance` | Seamless dev (filesystem) vs release (classpath) loading |
| Channel-pooled audio | `AudioManager` | 7 SFX slots + 1 music slot, reused not reallocated |
| Per-frame fade | `AudioManager.update(deltaMs)` | Volume interpolation per frame, non-blocking |
| Delta-time wall clock | `Main` | Frame-rate independent timing |
| EnumMap for counters | `ItemCountMap` | Faster and leaner than HashMap for enum keys |
| Flyweight glyphs | `BitmapFont` | Shared glyph data, textures created on demand |
| Data-driven configuration | `manifest.json` / presets / mappings | Asset pipeline, levels, and audio all driven by JSON data, not hardcoded |

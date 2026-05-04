# Asset Loading System

## Overview

The asset loading system is a manifest-driven pipeline. A JSON manifest defines an ordered sequence of **operations** — each takes input from previous steps, processes it, and stores the result by ID for later consumption.

The pipeline supports two execution environments transparently: the **classpath** (when running from a JAR) and the **filesystem** (during development), auto-detected at runtime.

---

## manifest.json Format

```json
{
  "format": 2,
  "sequence": [ ... ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `format` | int | Manifest schema version. Currently `2`. |
| `sequence` | array[object] | Ordered list of operations to execute sequentially. |

### Operation Entry Fields

Each entry in `sequence` has:

| Field | Type | Required | Applies To | Description |
|-------|------|----------|------------|-------------|
| `id` | string | yes | all | Unique identifier for the result. Other operations reference it via `input`. |
| `type` | string | yes | all | Operation type name (see registered types below). |
| `input` | string | conditional | non-`image`, non-`bitmap-font`, non-`mapping` | ID of a previous operation's result to use as input. |
| `file` | string | conditional | `image`, `bitmap-font` | File path relative to the asset root. |
| `description` | string | no | all | Human-readable label printed to stdout during loading. |
| `tileWidth`, `tileHeight` | int | conditional | `crop-tiles` | Pixel dimensions of each tile. |
| `columns`, `rows` | int | conditional | `crop-tiles` | Grid dimensions in the sprite sheet. |
| `mapping` | string[][] | conditional | `tile-type-mapping` *(deprecated)* | 2D array mapping each grid cell to a string type ID (or `null` for empty). Dimensions must match `columns` × `rows`. |
| `mapping-id` | string | conditional | `tile-type-mapping` | ID of a `mapping` operation result to use instead of inline `mapping`. |
| `value` | string[][] | conditional | `mapping` | 2D string array to store as a reusable mapping. |
| `width`, `height`, `types` | int | conditional | `tilemap-preset` | Tilemap dimensions and number of distinct tile types to generate. |
| `initial` | int[][] | optional | `tilemap-preset` | Seed grid: `0` = fillable cell, `-1` = blocked cell. Dimensions must match `height` × `width`. Omit for an all-fillable grid. |

### Registered Operation Types

| Type String | Class | Input | Output | Purpose |
|-------------|-------|-------|--------|---------|
| `image` | `ImageOperation` | — (uses `file`) | `SDL_Surface` | Load a PNG and produce an ABGR8888 surface. |
| `crop-tiles` | `CropTilesOperation` | `SDL_Surface` | `TileRegistry` | Split a spritesheet surface into individual tile surfaces stored in a registry. |
| `mapping` | `MappingOperation` | — (uses `value`) | `String[][]` | Store a 2D string array as a named mapping for later reuse. |
| `tile-type-mapping` | `TileTypeMappingOperation` | `TileRegistry` | `TileRegistry` | Overlay a 2D string-ID mapping on a registry (same object, new ID). Accepts `mapping-id` to reference a `mapping` operation. |
| `tilemap-preset` | `TilemapPresetOperation` | — (uses `width`/`height`/`types`/`initial`) | `TilemapPreset` | Define a tilemap configuration consumed by `CustomizedTilemapFactory.fromPreset`. |
| `create-texture` | `CreateTextureOperation` | `SDL_Surface` | `SDL_Texture` | Upload a surface as an SDL texture for GPU rendering. |
| `bitmap-font` | `BitmapFontOperation` | — (uses `file`) | `BitmapFont` | Load a JSON bitmap font definition and pre-build glyph textures. |

### Example (current manifest)

The project's `assets/manifest.json` defines a 10-step pipeline:

1. **image** → loads `tinyblocks.png` (180×180 px), stores as `tinyblocks/raw`
2. **crop-tiles** → crops `tinyblocks/raw` into 10×10 = 100 tiles, each 18×18 px, stored in a `TileRegistry` at `tiles/raw`
3. **mapping** → stores the 10×10 string ID grid as a reusable mapping at `tile-mapping`
4. **tile-type-mapping** → applies `tile-mapping` to `tiles/raw`, stored at `tiles/typed`
5. **create-texture** → uploads `tinyblocks/raw` surface to an SDL texture, stored at `tinyblocks/texture`
6. **image** → loads `tinyblocks-hl.png` (highlighted variant), stored as `hl-tinyblocks/raw`
7. **crop-tiles** → crops `hl-tinyblocks/raw` into tiles, stored at `hl-tiles/raw`
8. **tile-type-mapping** → reuses `tile-mapping` on highlighted tiles, stored at `hl-tiles/typed`
9. **create-texture** → uploads `hl-tinyblocks/raw` surface to a texture, stored at `hl-tinyblocks/texture`
10. **bitmap-font** → loads `monogram-bitmap.json`, pre-builds glyph textures, stored at `monogram/font`

The key improvement: step 3 extracts the mapping into its own operation, so steps 4 and 8 can reference the same `tile-mapping` by ID instead of duplicating it inline.

Consumers retrieve assets by ID, e.g.:

```java
TileRegistry tiles = AssetManager.instance().get("tiles/typed");
BitmapFont font = AssetManager.instance().get("monogram/font");
```

---

## AssetOperation

`AssetOperation` is the core interface each pipeline step implements:

```java
public interface AssetOperation {
    String type();
    void configure(JsonObject item);
    void process(Context ctx) throws Exception;

    public interface Context {
        String id();                         // target ID for this operation
        AssetLoader loader();                // for reading raw file data
        Registry registry();                 // operation-type factory registry
        void put(String id, Object asset);   // store result by ID
        <T> T getInput(String ref);          // retrieve previous result by ID
    }

    public interface Registry {
        void register(String type, OperationFactory factory);
        OperationFactory get(String type);
    }
}
```

### Lifecycle

1. **`configure(JsonObject)`** — called once before processing; extracts parameters from the manifest entry (type-specific fields like `file`, `tileWidth`, `mapping`, etc.)
2. **`process(Context)`** — performs the actual work: reads input via `ctx.getInput()`, uses `ctx.loader()` for file I/O, stores result via `ctx.put()`

The `Context` also provides access to the `Registry`, enabling operations to introspect available types.

### Operation Details

#### ImageOperation
- Reads a PNG via `AssetLoader` → `BufferedImage` (Java ImageIO)
- Converts ARGB pixel array to RGBA byte array
- Creates an `SDL_Surface` in `ABGR8888` format and writes pixels into it
- Stores the surface under the configured `id`

#### CropTilesOperation
- Takes an input `SDL_Surface` (the spritesheet)
- Iterates row-major over a `columns` × `rows` grid, blitting each tile region onto a new `SDL_Surface`
- Wraps all tile surfaces in a `TileRegistry`
- Stores the registry under the configured `id`

#### TileTypeMappingOperation
- Takes an input `TileRegistry`
- Resolves the mapping from either inline `mapping` (deprecated) or a `mapping-id` referencing a previous `mapping` operation
- Calls `registry.setTypeMapping(resolved)` on the existing registry
- Stores the **same** registry object under a new `id`
- Using `mapping-id` is preferred: it avoids duplicating large mapping arrays and allows reuse across multiple registries (e.g., normal and highlighted tile variants)

#### MappingOperation
- No input; reads a `value` 2D string array directly from the manifest entry
- Stores the `String[][]` array in the asset map under the configured `id`
- Used as a data source for `tile-type-mapping` via the `mapping-id` field
- Enables reuse: the same mapping can be shared across multiple `tile-type-mapping` operations without duplication

#### BitmapFontOperation
- Reads a JSON font definition file via `AssetLoader`
- Parses glyph data into a `Map<Integer, int[]>` (code point → 12 row bitmasks)
- Constructs a `BitmapFont` instance and calls `prebuildTextures()` to create an `SDL_Texture` for each non-empty glyph
- Stores the `BitmapFont` under the configured `id`

  > See [`docs/bitmap-font.md`](bitmap-font.md) for the JSON format and pixel encoding scheme.

#### CreateTextureOperation
- Takes an input `SDL_Surface`
- Creates an `SDL_Texture` via `SDL_CreateTextureFromSurface` using the `SDL_Renderer` from `AssetManager.instance().renderer()`
- Stores the texture under the configured `id`

#### TilemapPresetOperation
- No input; reads `width`, `height`, `types`, and an optional `initial` 2D int array directly from the manifest entry
- Stores a `TilemapPreset` (record of `width`, `height`, `types`, `initial`) under the configured `id`
- Used by `CustomizedTilemapFactory.fromPreset` to generate `Tilemap`s for built-in difficulty levels (`tilemap/easy`, `tilemap/hard`)
- Decouples gameplay parameters and the easy-mode initial blocked-cell pattern from Java source — tweak the manifest to retune difficulty without recompiling

---

## AssetManager

Singleton that orchestrates the loading pipeline and provides runtime access to all loaded assets.

### Public API

```java
public class AssetManager {
    // Singleton access
    public static AssetManager instance();

    // Initialization (must call before loadManifest)
    public void init(SDL_Renderer renderer);

    // Accessors
    public SDL_Renderer renderer();

    // Load a manifest JSON file and execute its operation sequence
    public void loadManifest(String path) throws Exception;

    // Retrieval — throws if not found; casts to inferred type
    public <T> T get(String id);

    // Existence check
    public boolean has(String id);

    // Dispose — calls close() on all AutoCloseable assets
    public void dispose();
}
```

### Loading Flow

1. **`init(renderer)`** — stores the SDL renderer and selects an `AssetLoader` via `AssetLoaderInstance`
2. **`loadManifest(path)`** — reads the JSON manifest through the loader, iterates `sequence`, for each item:
   - Looks up the `OperationFactory` by type string
   - Creates a fresh `AssetOperation` instance
   - Calls `configure()` with the JSON entry
   - Calls `process()` with a `ContextImpl` wired to the asset map
3. All results are stored in an internal `Map<String, Object>` — retained for the JVM lifetime.

### Internal State

- `Map<String, Object> assets` — the flat ID-to-asset cache
- `AssetOperation.Registry` — maps type strings (`"image"`, `"crop-tiles"`, etc.) to `OperationFactory` instances

---

## AutoCloseable Support

Assets that hold native SDL resources (textures, surfaces) implement `AutoCloseable` to allow orderly cleanup.

### BitmapFont

`BitmapFont` implements `AutoCloseable`:

```java
public class BitmapFont implements AutoCloseable {
    // ...
    @Override
    public void close();
    public void dispose();
}
```

- `close()` (the `AutoCloseable` entry point) delegates to `dispose()`
- `dispose()` calls `SDL_DestroyTexture` on every pre-built glyph texture and clears the cache

### AssetManager.dispose()

`AssetManager.dispose()` iterates all loaded assets and calls `close()` on any that implement `AutoCloseable`:

```java
public void dispose() {
    for (Object asset : assets.values()) {
        if (asset instanceof AutoCloseable ac) {
            ac.close();
        }
    }
    assets.clear();
}
```

This is a safe-to-call pattern: non-`AutoCloseable` assets like `SDL_Surface` (used directly in rendering) and `TileRegistry` are simply skipped. Call `AssetManager.instance().dispose()` during application shutdown.

> **Note:** `TileRegistry` does not currently implement `AutoCloseable`. Its `dispose()` method must be called manually if cleanup is needed before JVM exit.

### Design Principle

Not all asset types need `AutoCloseable`. Only resources that wrap native SDL objects (textures, surfaces that must be freed via SDL APIs) implement it. Plain data objects (`String[][]`, collections) are GC'd normally.

---

## AssetLoader

Abstracts file I/O so the same manifest works in both development and production.

```java
public interface AssetLoader {
    InputStream load(String path) throws Exception;
}
```

### Implementations

| Class | Strategy | When Used |
|-------|----------|-----------|
| `FsAssetLoader` | Reads from file system, base path `"assets"` | During development (IDE run) |
| `ClspAssetLoader` | Reads from classpath via `ClassLoader.getResourceAsStream()` | When running from a JAR |

### Selection Logic (`AssetLoaderInstance`)

```
System.getProperty("app.pairs.assetLoader")
  ├─ "clsp"   → ClspAssetLoader
  ├─ "fs"     → FsAssetLoader("assets")
  └─ "auto"   → Detect:
                  ├─ Running from JAR → ClspAssetLoader
                  └─ Not a JAR        → FsAssetLoader("assets")
```

Override at runtime: `java -Dapp.pairs.assetLoader=fs -jar ...`

---

## TileRegistry

Holds tile surfaces and optional type mappings after a `crop-tiles` + `tile-type-mapping` pipeline.

```java
// Construction
new TileRegistry(int columns, int rows, int tileWidth, int tileHeight);

// Access
SDL_Surface getTile(int row, int col);   // 2D grid access
SDL_Surface getTile(int id);             // 1D row-major index
int getTileWidth();
int getTileHeight();
int getRows();
int getColumns();
int getTileCount();

// Type mapping (set by TileTypeMappingOperation)
void setTypeMapping(String[][] mapping);
String getTypeId(int row, int col);      // returns null if unmapped or null entry

// Cleanup
void dispose();                           // frees all SDL_Surfaces
```

- Tiles are stored in a flat `SDL_Surface[]`, index = `row * columns + col`.
- `getTile(int id)` returns `null` for out-of-range indices (no exception).
- `getTypeId()` returns `null` if no mapping has been set or the cell maps to `null`.
- **Lifetime:** allocated by the JVM, SDL surfaces must be freed via `dispose()`. Currently does not implement `AutoCloseable`, so disposal must be handled manually.


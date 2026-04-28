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
| `input` | string | conditional | non-`image` | ID of a previous operation's result to use as input. |
| `file` | string | conditional | `image` | Image file path relative to the asset root. |
| `description` | string | no | all | Human-readable label printed to stdout during loading. |
| `tileWidth`, `tileHeight` | int | conditional | `crop-tiles` | Pixel dimensions of each tile. |
| `columns`, `rows` | int | conditional | `crop-tiles` | Grid dimensions in the sprite sheet. |
| `mapping` | string[][] | conditional | `tile-type-mapping` | 2D array mapping each grid cell to a string type ID (or `null` for empty). Dimensions must match `columns` x `rows`. |

### Registered Operation Types

| Type String | Class | Input | Output | Purpose |
|-------------|-------|-------|--------|---------|
| `image` | `ImageOperation` | — (uses `file`) | `SDL_Surface` | Load a PNG and produce an ABGR8888 surface. |
| `crop-tiles` | `CropTilesOperation` | `SDL_Surface` | `TileRegistry` | Split a spritesheet surface into individual tile surfaces stored in a registry. |
| `tile-type-mapping` | `TileTypeMappingOperation` | `TileRegistry` | `TileRegistry` | Overlay a 2D string-ID mapping on a registry (same object, new ID). |
| `create-texture` | `CreateTextureOperation` | `SDL_Surface` | `SDL_Texture` | Upload a surface as an SDL texture for GPU rendering. |

### Example (current manifest)

The project's `assets/manifest.json` defines a 4-step pipeline:

1. **image** → loads `tinyblocks.png` (180×180 px), stores as `tinyblocks/raw`
2. **crop-tiles** → crops `tinyblocks/raw` into 10×10 = 100 tiles, each 18×18 px, stored in a `TileRegistry` at `tiles/raw`
3. **tile-type-mapping** → assigns each tile a string ID (`"0"`..`"99"`), stored at `tiles/typed`
4. **create-texture** → uploads `tinyblocks/raw` surface to an SDL texture, stored at `tinyblocks/texture`

Consumers retrieve assets by ID, e.g.:

```java
TileRegistry tiles = AssetManager.instance().get("tiles/typed");
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
- Parses the 2D `mapping` array from JSON (`null` entries become `null`)
- Calls `registry.setTypeMapping(mapping)` on the existing registry
- Stores the **same** registry object under a new `id` (re-keys it)

#### CreateTextureOperation
- Takes an input `SDL_Surface`
- Creates an `SDL_Texture` via `SDL_CreateTextureFromSurface` using the `SDL_Renderer` from `AssetManager.instance().renderer()`
- Stores the texture under the configured `id`

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
- **Lifetime:** allocated by the JVM, SDL surfaces must be freed via `dispose()`. Currently not called automatically — the registry lives until JVM exit.


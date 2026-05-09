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
| `input` | string | conditional | non-`image`, non-`bitmap-font`, non-`mapping`, non-`tile-groups`, non-`tilemap-preset` | ID of a previous operation's result to use as input. |
| `file` | string | conditional | `image`, `bitmap-font`, `mapping`, `tile-groups`, `tilemap-preset` | File path relative to the asset root. |
| `description` | string | no | all | Human-readable label printed to stdout during loading. |
| `tileWidth`, `tileHeight` | int | conditional | `crop-tiles` | Pixel dimensions of each tile. |
| `columns`, `rows` | int | conditional | `crop-tiles` | Grid dimensions in the sprite sheet. |
| `mapping-id` | string | conditional | `tile-type-mapping` | ID of a `mapping` operation result to use as the type map. |

### Registered Operation Types

| Type String | Class | Input | Output | Purpose |
|-------------|-------|-------|--------|---------|
| `image` | `ImageOperation` | — (uses `file`) | `SDL_Surface` | Load a PNG and produce an ABGR8888 surface. |
| `crop-tiles` | `CropTilesOperation` | `SDL_Surface` | `TileRegistry` | Split a spritesheet surface into individual tile surfaces stored in a registry. |
| `mapping` | `MappingOperation` | — (uses `file`) | `String[][]` | Load an external JSON file containing a `mapping` 2D string array and store it as a named mapping for later reuse. |
| `tile-type-mapping` | `TileTypeMappingOperation` | `TileRegistry` | `TileRegistry` | Overlay a 2D string-ID mapping on a registry (same object, new ID). Uses `mapping-id` to reference a `mapping` operation result. |
| `tilemap-preset` | `TilemapPresetOperation` | — (uses `file`) | `PresetConfig` | Load an external JSON file containing width/height/types/difficulty/includeSlabs/spread, and optional initial/pairStrategy. |
| `tile-groups` | `TileGroupsOperation` | — (uses `file`) | `TileGroupRegistry` | Load an external JSON file defining tile similarity groups for `TileSelectionPolicy`. |
| `create-texture` | `CreateTextureOperation` | `SDL_Surface` | `SDL_Texture` | Upload a surface as an SDL texture for GPU rendering. |
| `bitmap-font` | `BitmapFontOperation` | — (uses `file`) | `BitmapFont` | Load a JSON bitmap font definition and pre-build glyph textures. |

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

1. **`configure(JsonObject)`** — called once before processing; extracts parameters from the manifest entry (type-specific fields like `file`, `tileWidth`, `columns`, etc.)
2. **`process(Context)`** — performs the actual work: reads input via `ctx.getInput()`, uses `ctx.loader()` for file I/O, stores result via `ctx.put()`

---

## AssetManager

Singleton that orchestrates the loading pipeline and provides runtime access to all loaded assets.

```java
public class AssetManager {
    public static AssetManager instance();
    public void init(SDL_Renderer renderer);
    public SDL_Renderer renderer();
    public void loadManifest(String path) throws Exception;
    public <T> T get(String id);
    public boolean has(String id);
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

### Selection Logic

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

Stores a flat list of tile surfaces with a bidirectional int↔String type-ID mapping. Each entry gets a sequential numeric ID (starting at 1) for use as the game's tile type identifier. SDL_Textures can be created and retrieved by numeric ID.

```java
new TileRegistry(int tileWidth, int tileHeight,
                 SDL_Surface[] tiles, String[] stringIds);

int getTileWidth();
int getTileHeight();
int getTypeCount();

// Bidirectional int↔String mapping
int getNumericId(String stringId);   // returns -1 if unknown
String getStringId(int numericId);   // returns null if unknown

// Texture management
void createTextures(SDL_Renderer renderer);
SDL_Texture getTexture(int typeId);  // 1-based, null if not created

// Cleanup (implements AutoCloseable)
void dispose();
void close();
```

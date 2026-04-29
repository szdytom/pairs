# Tile ID System

TileRegistry maintains two kinds of identifiers for each tile:

- **String ID** — human-readable identifier from the asset manifest's tile-mapping. Used in manifest JSON and asset pipeline.
- **Numeric ID** — compact 1-based integer assigned sequentially by TileRegistry at build time. Used as the game's tile type identifier in `GameState`, `Tilemap`, and view rendering. Value `0` means empty/no tile.

## Conversion APIs (`TileRegistry`)

| Method | Direction |
|---|---|
| `getNumericId(String)` | String → Numeric, returns -1 if unknown |
| `getStringId(int)` | Numeric → String, returns null if unknown |
| `getTypeCount()` | Total registered tile types |

It is garanteed that all numeric IDs from 1 to `getTypeCount()` have a valid mapping to a string ID and texture.

## Rendering

`getTexture(int typeId)` directly returns the `SDL_Texture` for a numeric ID, skipping string-based lookup entirely.

## Example

```
Manifest mapping "gold_block" → Numeric ID 1
Manifest mapping "grass_block" → Numeric ID 2
(These mappings are generated at runtime, the above is just an example.)
...

GameState stores 1, 2, ... directly.
View calls tileRegistry.getTexture(1) → texture for tile type 1.
```

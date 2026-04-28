Isometric Projection
===================

The game renders a 2D grid of tiles using an isometric (orthographic) projection with a 2:1 pixel ratio.
This document describes the coordinate system, projection math, rendering pipeline, and hit-testing logic.

## Table of Contents

1. [Coordinate Systems](#coordinate-systems)
2. [Projection Formulas](#projection-formulas)
3. [Depth Sorting](#depth-sorting)
4. [Rendering Pipeline](#rendering-pipeline)
5. [Hit-Testing (Mouse Hover)](#hit-testing)
6. [API Reference](#api-reference)

---

## Coordinate Systems

Two coordinate spaces are bridged by `IsometricMapper`:

| Space | Axes | Range | Description |
|---|---|---|---|
| **Grid** `(row, col)` | `row` ↓, `col` → | integers, zero-based | Logical tile position in the 2D map array |
| **Screen** `(x, y)` | `x` →, `y` ↓ | pixel coordinates | Position on the SDL render target |

The grid origin `(0, 0)` maps to a fixed screen origin `(originX, originY)`, typically placed at the top-center of the window.

---

## Projection Formulas

The projection uses a 2:1 isometric ratio — the vertical step is half the horizontal step, producing diamond-shaped tile footprints that tile the plane without gaps.

### Grid → Screen (`gridToScreen`)

```java
stepX = tileWidth * scale / 2   // horizontal half-step
stepY = tileWidth * scale / 4   // vertical half-step

screenX = originX + (col - row) * stepX
screenY = originY + (col + row) * stepY
```

The returned point is the **center** of the tile's diamond footprint on screen.
The diamond vertices (extents from center) are:

```
top:      (center.x,            center.y - stepY)
right:    (center.x + stepX,    center.y)
bottom:   (center.x,            center.y + stepY)
left:     (center.x - stepX,    center.y)
```

### Screen → Grid (`screenToGrid`)

Invert the linear system to recover fractional `(col, row)`, then round to the nearest integer:

```java
relX = screenX - originX
relY = screenY - originY

colMinusRow = relX / stepX
colPlusRow  = relY / stepY

col = (colMinusRow + colPlusRow) / 2.0
row = colPlusRow - col

gridRow = round(row)
gridCol = round(col)
```

`Math.round` is the correct inverse of the diamond-grid forward projection.
The diamond footprint of tile `(r, c)` in `(col, row)` space is the axis-aligned unit square
`[c - 0.5, c + 0.5) × [r - 0.5, r + 0.5)`, so *round-to-nearest* — not floor — assigns
each screen point to the correct tile.

### Visual Diamond Containment (`contains`)

For hit-testing individual tiles, a diamond inequality with the sprite's visual extents is used:

```
halfW = tileWidth  * scale / 2
halfH = tileHeight * scale / 2

|dx| / halfW + |dy| / halfH ≤ 1
```

Implemented with integer-only arithmetic to avoid floating-point issues:

```java
dx * halfH + dy * halfW ≤ halfW * halfH
```

Note: the tile sprites fill most but not all of the source rectangle (corners may be transparent).
The actual hit-testing uses the full sprite rectangle (see [Hit-Testing](#hit-testing)).

### Tile Geometry

Each tile sprite is 16×16 source pixels (cropped from an 18×18 padded spritesheet cell).
At integer `scale`, the destination rectangle is `(16*scale)` × `(16*scale)` screen pixels,
centered at `gridToScreen(row, col)`.

```java
dstW = TILE_CONTENT_WIDTH  * scale   // 16 * scale
dstH = TILE_CONTENT_HEIGHT * scale   // 16 * scale
dstX = center.x - dstW / 2
dstY = center.y - dstH / 2
```

---

## Depth Sorting

Tiles are drawn in **painter's algorithm** order — back to front, by increasing
`row + col` sum. Tiles with smaller `row + col` are drawn first; tiles with larger
sums are drawn last and appear on top.

```java
int[][] getDepthSortedOrder(int gridRows, int gridCols) {
    int[][] order = new int[gridRows * gridCols][];
    int idx = 0;
    for (int sum = 0; sum < gridRows + gridCols - 1; sum++) {
        for (int row = 0; row < gridRows; row++) {
            int col = sum - row;
            if (col >= 0 && col < gridCols) {
                order[idx++] = new int[] {row, col};
            }
        }
    }
    return order;
}
```

This diagonal iteration ensures tiles at the same `row + col` sum (which do not
visually overlap) are drawn in any order within the group, then all tiles at the
next sum are drawn on top.

---

## Rendering Pipeline

Each frame, `IsometricGridView.render()` performs:

1. **Texture initialization** (first frame only) — create one `SDL_Texture` per unique
   tile type from the `TileRegistry` surfaces.

2. **Depth sort** — compute `depthOrder[]` via `getDepthSortedOrder`.

3. **Hover resolution** — iterate tiles in *reverse* depth order (front to back),
   checking each tile's sprite rectangle against the mouse coordinates.
   The first match is the frontmost visible tile under the cursor.
   See [Hit-Testing](#hit-testing).

4. **Draw loop** — iterate `depthOrder` back-to-front. For each non-empty tile:
   - Compute screen position via `gridToScreen(row, col, scale)`
   - Apply hover offset if this tile is the hovered one (lift by `TILE_CONTENT_HEIGHT * scale / 3` pixels upward)
   - Blit the texture via `SDL_RenderCopy`

```java
for (int[] pos : depthOrder) {
    int row = pos[0], col = pos[1];
    IsometricCoordinate center = mapper.gridToScreen(row, col, scale);
    dstRect.x = center.x - dstW / 2;
    dstRect.y = center.y - dstH / 2
        - ((row == hoveredRow && col == hoveredCol) ? hoverOffset : 0);
    dstRect.w = dstW;
    dstRect.h = dstH;
    SDL_RenderCopy(renderer, tex, SRC_RECT, dstRect);
}
```

### Sprite Source Rectangle

Tiles come from a spritesheet with 1px padding between cells. The source rectangle
crops this padding:

```java
SRC_RECT.x = 1;
SRC_RECT.y = 1;
SRC_RECT.w = TILE_CONTENT_WIDTH;   // 16
SRC_RECT.h = TILE_CONTENT_HEIGHT;  // 16
```

---

## Hit-Testing

Hit-testing resolves which tile is under the mouse cursor and handles the hover
visual feedback. The logic lives in `IsometricGridView.resolveHoveredCell()`.

### Algorithm

Tiles are tested **front-to-back** (reverse depth order: highest `row + col` first)
against their **sprite rectangles** (the `dstRect` used in rendering). The first
hit is the frontmost visible tile — this correctly handles occlusion because tiles
in front are tested first.

```
for each tile in reverse depth order (front → back):
    center = gridToScreen(row, col, scale)
    rect = [center.x - dstW/2, center.x + dstW/2) ×
           [center.y - dstH/2, center.y + dstH/2)
    if cursor in rect → this tile is hovered, stop
```

### Raised-Tile Hit-Testing

When the previously hovered tile is visually lifted (z-offset), its sprite moves
upward. To avoid the hover glitching off when the cursor is still over the raised
visual, the previously hovered tile is also tested at its *raised* position:

```
if tile was the previous hovered tile:
    raisedRect = [center.x - dstW/2, center.x + dstW/2) ×
                 [center.y - dstH/2 - hoverOffset, center.y + dstH/2 - hoverOffset)
    if cursor in raisedRect → this tile is still hovered, stop
```

The `hoverOffset` is `TILE_CONTENT_HEIGHT * scale / 3`.

### Design Notes

- **Reverse depth order** is essential: tiles in front (higher `row + col`) are
  drawn on top of tiles behind them. Testing front-to-back ensures the visible
  tile is selected.
- **Sprite rectangle** hit-testing is used instead of diamond-footprint containment
  because the tile sprites fill most of the 16×16 source area (with small corner
  cutouts) — a diamond inequality would reject valid hit points near the tile edges.
- The `mouseX`/`mouseY` fields are updated on `SDL_MOUSEMOTION` events and cleared
  to `(-1, -1)` on `SDL_WINDOWEVENT_LEAVE` (cursor exits the window).

---

## Hover Visual Effect

When a tile is hovered, its rendered Y position is offset upward by
`TILE_CONTENT_HEIGHT * scale / 3` pixels. This creates a "lifting" effect that
visually distinguishes the hovered tile.

The tile is still drawn at its natural position in the depth-sorted order, so:
- Tiles *behind* it (smaller `row + col`, drawn earlier) are correctly covered by
  the raised tile's sprite.
- Tiles *in front* (larger `row + col`, drawn later) correctly cover the raised
  tile's lower portion.

---

## API Reference

### `IsometricMapper`

| Method | Description |
|---|---|
| `gridToScreen(row, col, scale)` | Convert grid coords to screen pixel coords. Returns `IsometricCoordinate{x, y}` (diamond center). |
| `screenToGrid(screenX, screenY, scale)` | Convert screen pixel coords to grid coords. Returns `int[]{row, col}`. |
| `contains(screenX, screenY, row, col, scale)` | Test if a screen point is inside the visual diamond of a specific tile. |
| `getDepthSortedOrder(rows, cols)` | Return `{row, col}` pairs in painter's-algorithm order (back to front). |

### `IsometricMapper.IsometricCoordinate`

Immutable value object with `public final int x, y`.

### `IsometricGridView`

| Method / Field | Description |
|---|---|
| `render(renderer, scale)` | Main draw call. Initializes textures on first call, resolves hover, draws all tiles depth-sorted. |
| `setMousePosition(x, y)` | Update tracked mouse position (called from main event loop). Set to `(-1, -1)` to clear. |
| `setGrid(grid)` | Replace the displayed tile grid (`String[][]` of type IDs; `null`/empty = no tile). |
| `resolveHoveredCell(mx, my, depthOrder, scale)` | Internal: reverse-depth-order hit-test. Writes result to `hoveredRow`/`hoveredCol` fields. |
| `hoveredRow`, `hoveredCol` | Current frame's hovered tile (or `-1`). |
| `prevHoveredRow`, `prevHoveredCol` | Previous frame's hovered tile. Used to test raised position. |

### Integration with Main Loop

```java
// Construction
IsometricMapper mapper = new IsometricMapper(TILE_WIDTH, TILE_HEIGHT, originX, originY);
IsometricGridView gridView = new IsometricGridView(grid, tileRegistry, mapper);

// Event loop
case SDL_MOUSEMOTION:
    gridView.setMousePosition(evt.motion.x, evt.motion.y);
    break;
case SDL_WINDOWEVENT:
    if (evt.window.event == SDL_WINDOWEVENT_LEAVE)
        gridView.setMousePosition(-1, -1);
    break;

// Render (each frame)
gridView.update(deltaTime);
gridView.render(renderer, scale);
```

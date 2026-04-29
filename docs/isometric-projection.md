Isometric Projection
===================

The game renders a 2D grid of tiles using an isometric (orthographic) projection with a 2:1 pixel ratio.
This document describes the coordinate systems, projection math, rendering pipeline, and hit-testing logic.

## Table of Contents

1. [Coordinate Systems](#coordinate-systems)
2. [Projection Formulas](#projection-formulas)
3. [Scale Boundaries](#scale-boundaries)
4. [Depth Sorting](#depth-sorting)
5. [Rendering Pipeline](#rendering-pipeline)
6. [Hit-Testing (Mouse Hover)](#hit-testing)

---

## Coordinate Systems

Three coordinate spaces exist in the codebase:

| Space | Axes | Description |
|---|---|---|
| **Grid** | `(row, col)`, row ↓ col → | Logical tile position in the map array |
| **Logical pixel** | `(x, y)` | Orthographic projection output; scale-independent (scale = 1) |
| **Screen pixel** | `(sx, sy)` | Actual position on the SDL render target; scale = s |

The grid origin `(0, 0)` maps to a fixed screen-pixel origin `(originX, originY)`,
typically placed at the top-centre of the window.  `IsometricMapper` bridges
grid ⟷ logical pixel; scale is injected only at the outermost render boundary.

```
Grid ──gridToLogical──▶ Logical pixel ──× scale──▶ Screen pixel
```

---

## Projection Formulas

The projection uses a 2:1 isometric ratio — the vertical step is half the
horizontal step, producing diamond-shaped tile footprints that tile the plane
without gaps.

### Grid → Logical pixel

```
stepX  = tileWidth / 2
stepY  = tileWidth / 4
logicalX = originX + (col − row) × stepX
logicalY = originY + (col + row) × stepY
```

The result is the **centre** of the tile's diamond footprint.  Diamond
vertices extend stepX/stepY from the centre in each cardinal direction.

### Logical pixel → Grid

Invert the linear system, then round to the nearest integer.  The diamond
footprint of tile `(r, c)` in `(col, row)` space is the axis-aligned unit
square `[c − 0.5, c + 0.5) × [r − 0.5, r + 0.5)`, so *round-to-nearest* —
not floor — correctly assigns each point to a tile.

### Visual Diamond Containment

```
halfW = tileWidth / 2
halfH = tileHeight / 2
|dx| / halfW + |dy| / halfH ≤ 1
```

Implemented with integer-only arithmetic: `dx × halfH + dy × halfW ≤ halfW × halfH`.

### Tile Geometry

Each tile sprite is 16 × 16 source pixels (cropped from an 18 × 18 padded
spritesheet cell).  At scale `s`, the destination rectangle on screen is
`(16 × s)` × `(16 × s)` pixels, centred at the tile's screen-pixel centre.

The source rectangle crops the 1 px spritesheet padding: `(1, 1, 16, 16)`.

---

## Scale Boundaries

`scale` never enters `IsometricMapper`.  It appears only at two boundaries:

| Boundary | Operation |
|---|---|
| **Event entry** | Screen mouse coords → logical by `/ scale` |
| **Render exit**   | Logical pixel coords → screen by `× scale` |

### Logical → screen

```
screenX = originX + (logicalX − originX) × scale
screenY = originY + (logicalY − originY) × scale
```

This is equivalent to `originX + (col − row) × tileWidth × scale / 2` but
keeps the projection and scale concerns separate.

---

## Depth Sorting

Tiles are drawn in **painter's algorithm** order — back to front, by increasing
`row + col`.  Tiles at the same sum (which do not visually overlap) are drawn
in any order; tiles at the next sum are drawn on top.

---

## Rendering Pipeline

Each frame, `IsometricGridView.render()`:

1. **Initialise textures** (first frame only) — one `SDL_Texture` per tile type.
2. **Depth sort** — `getDepthSortedOrder(gridRows, gridCols)`.
3. **Draw loop** — iterate depth order back-to-front.  For each non-empty tile:
   - `logicalPos = mapper.gridToLogical(row, col)`
   - `screenPos = origin + (logicalPos − origin) × scale`
   - Apply hover lift if highlighted: `screenY −= TILE_CONTENT_HEIGHT × scale / 3`
   - Blit via `SDL_RenderCopy`

### Sprite source rectangle

```
SRC_RECT = (x=1, y=1, w=16, h=16)
```

---

## Hit-Testing

Hit-testing resolves which tile is under the mouse cursor.  The mouse
coordinates are already in logical-pixel space (divided by scale in the event
loop).  Tile positions are computed in logical-pixel space, so **no scale
is involved**.

### Algorithm

Tiles are tested **front-to-back** (reverse depth order, highest `row + col`
first) against their logical sprite rectangles (half-width 8, half-height 8).
The first hit is the frontmost visible tile — this correctly handles occlusion
since tiles in front are tested first.

```
logicalOriginX = originX / scale     (cached each frame in render)
logicalOriginY = originY / scale

cx = logicalOriginX + (col − row) × tileWidth / 2
cy = logicalOriginY + (col + row) × tileWidth / 4
rect = [cx − 8, cx + 8) × [cy − 8, cy + 8)
```

### Raised-tile fix

When the previously hovered tile is visually lifted, its sprite moves upward.
To avoid the hover glitching off, the previously hovered tile is also tested
at its raised position:

```
raisedRect = [cx − 8, cx + 8) × [cy − 8 − 5, cy + 8 − 5)
```

The logical hover offset `TILE_CONTENT_HEIGHT / 3 = 5` is an integer constant,
independent of scale.

### Design notes

- **Reverse depth order** ensures the frontmost visible tile is selected (it is
  drawn last, covering ones behind it).
- **Sprite rectangle** hit-testing is used instead of diamond containment
  because the tile sprites fill most of the 16 × 16 area — a diamond inequality
  would reject valid hits near corners.
- `mouseX`/`mouseY` store **logical-pixel** coordinates and are cleared on
  `SDL_WINDOWEVENT_LEAVE`.

---

## Hover Visual Effect

When a tile is highlighted, its rendered Y position is offset upward by
`TILE_CONTENT_HEIGHT × scale / 3` pixels, creating a "lifting" effect.
The tile is drawn at its natural depth-sorted order, so:
- Tiles *behind* it (smaller `row + col`) are correctly covered by the raised
  sprite.
- Tiles *in front* (larger `row + col`) correctly cover the raised tile's lower
  portion.

---

## `IsometricMapper` API

| Method | Description |
|---|---|
| `gridToLogical(row, col)` | Grid → logical-pixel coords. Returns `{x, y}` (diamond centre). |
| `logicalToGrid(x, y)` | Logical-pixel → grid coords. Returns `{row, col}`. |
| `contains(x, y, row, col)` | Test if a logical point is inside a tile's visual diamond. |
| `getDepthSortedOrder(rows, cols)` | Painter's-algorithm order (back to front). |
| `getOriginX()` / `getOriginY()` | Logical-pixel origin of tile `(0, 0)`. |
| `getTileWidth()` / `getTileHeight()` | Tile dimensions in logical pixels. |

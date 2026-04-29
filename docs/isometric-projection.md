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

Four coordinate spaces exist in the codebase — the last three apply to non-isometric 2D components as well:

| Space | Axes | Description |
|---|---|---|
| **Grid** | `(row, col)`, row ↓ col → | Logical tile position in the map array |
| **Local logical pixel** | `(x, y)` | Position relative to the parent component; set by `layout()`. |
| **Global logical pixel** | `(gx, gy)` | `parentGlobal + localLogical`; computed in `render()`. |
| **Screen pixel** | `(sx, sy)` | Actual position on the SDL render target; `globalLogical × scale`. |

The data flow:

```
Grid ──gridToLogical──▶ Local logical ──+ parent pos──▶ Global logical ──× scale──▶ Screen pixel
```

`IsometricMapper` bridges grid ⟷ local logical pixel. Scale and parent position
are injected only in `render()`.

Unlike the old system, the mapper no longer stores an origin — it returns offsets
relative to the grid's own origin. The renderer supplies the grid's global
position via the `(x, y)` parameter.

---

## Projection Formulas

The projection uses a 2:1 isometric ratio — the vertical step is half the
horizontal step, producing diamond-shaped tile footprints that tile the plane
without gaps.

### Grid → Local logical pixel

```
stepX  = tileWidth / 2
stepY  = tileWidth / 4
localX = (col − row) × stepX
localY = (col + row) × stepY
```

The result is an **offset from the grid origin** to the centre of the tile's
diamond footprint. The renderer then adds the grid's global position and
multiplies by scale to produce screen coordinates.

### Local logical pixel → Grid

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
| **Event entry** | Screen mouse coords → global logical by `/ scale` |
| **Render exit**   | Global logical coords → screen by `× scale` |

### Global logical → screen

```
screenX = globalLogicalX × scale
screenY = globalLogicalY × scale
```

This is equivalent to `(parentX + localX) × scale` and keeps the projection,
layout, and scale concerns separate.

---

## Depth Sorting

Tiles are drawn in **painter's algorithm** order — back to front, by increasing
`row + col`.  Tiles at the same sum (which do not visually overlap) are drawn
in any order; tiles at the next sum are drawn on top.

---

## Rendering Pipeline

Each frame follows the two-phase layout cycle before rendering:

1. **Measure** (only when layout is dirty) — `component.measure()` reports
   natural logical-pixel size bottom-up.
2. **Layout** (only when layout is dirty) — the root assigns rectangles
   top-down via `component.layout(x, y, w, h)`. Each component stores its
   local position relative to its parent.
3. **Render** — the root calls `component.render(renderer, 0, 0, scale)` and
   each component:
   - Computes its global position: `global = parentGlobal + localLayout`
   - Calls children with its own global position
   - Renders itself at the computed screen position

In `IsometricGridView.render()`:

1. **Initialise textures** (first frame only) — one `SDL_Texture` per tile type.
2. **Depth sort** — `getDepthSortedOrder(gridRows, gridCols)`.
3. **Draw loop** — iterate depth order back-to-front.  For each non-empty tile:
   - `localPos = mapper.gridToLogical(row, col)`
   - `screenPos = (gridGlobalPos + localPos) × scale`
   - Apply hover lift if highlighted: `screenY −= TILE_CONTENT_HEIGHT × scale / 3`
   - Blit via `SDL_RenderCopy`

### Sprite source rectangle

```
SRC_RECT = (x=1, y=1, w=16, h=16)
```

---

## Hit-Testing

Hit-testing resolves which tile is under the mouse cursor.  The mouse
coordinates are already in global logical-pixel space (divided by scale in the
event loop).  Tile positions are computed in the same space, using the grid's
global logical position cached in `render()`.

### Algorithm

Tiles are tested **front-to-back** (reverse depth order, highest `row + col`
first) against their logical sprite rectangles (half-width 8, half-height 8).
The first hit is the frontmost visible tile — this correctly handles occlusion
since tiles in front are tested first.

```
gridOriginGlobalX = levelGlobalX + gridLayoutX
gridOriginGlobalY = levelGlobalY + gridLayoutY

cx = gridOriginGlobalX + (col − row) × tileWidth / 2
cy = gridOriginGlobalY + (col + row) × tileWidth / 4
rect = [cx − 8, cx + 8) × [cy − 8, cy + 8)
```

Note the contrast with the old system: no division by scale is needed because
the grid origin is already stored in global logical-pixel space, matching the
mouse coordinates.

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
- `mouseX`/`mouseY` store **global logical-pixel** coordinates and are cleared on
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
| `gridToLogical(row, col)` | Grid → local logical-pixel coords (offset from grid origin). Returns `{x, y}` (diamond centre). |
| `logicalToGrid(x, y)` | Local logical-pixel → grid coords. Returns `{row, col}`. |
| `contains(x, y, row, col)` | Test if a local-logical point is inside a tile's visual diamond. |
| `getDepthSortedOrder(rows, cols)` | Painter's-algorithm order (back to front). |
| `getTileWidth()` / `getTileHeight()` | Tile dimensions in logical pixels. |

UI Layout System
================

This document describes the two-phase layout system used by the game, the
coordinate system it operates on, and the available layout helpers.

## Table of Contents

1. [Overview](#overview)
2. [Coordinate Systems](#coordinate-systems)
3. [ViewComponent API](#viewcomponent-api)
4. [Layout Lifecycle](#layout-lifecycle)
5. [TextComponent](#textcomponent)
6. [FlexLayout](#flexlayout)
7. [GridLayout](#gridlayout)
8. [Migration Guide](#migration-guide)

---

## Overview

The UI uses a **two-phase layout** cycle inspired by Android's `onMeasure` /
`onLayout` and the web's Flexbox/CSS Grid:

1. **Measure** — bottom-up. Each component reports its natural logical-pixel
   size via `measure()`, without knowing its final position.
2. **Layout** — top-down. The root assigns each component a rectangle
   (position relative to parent) via `layout(x, y, w, h)`. Components store
   their assigned position and use it during rendering.
3. **Render** — top-down. The root calls `render(renderer, 0, 0, scale)`.
   Each component computes its global logical position as
   `(parentGlobal + localLayout)`, renders itself, and passes its global
   position to children.

This separation allows containers (FlexLayout, GridLayout) to distribute
space predictably without child components knowing about each other.

---

## Coordinate Systems

All 2D components (non-isometric) use three coordinate spaces:

| Space | Description | Example |
|---|---|---|
| **Local logical pixel** | Position relative to parent, set by `layout()`. | `TextComponent` at `(1, 1)` means 1 px right and 1 px down from the parent's top-left. |
| **Global logical pixel** | `parentGlobal + localLogical`. Computed each frame in `render()`. | A component at local `(1, 1)` inside a root at `(0, 0)` has global `(1, 1)`. |
| **Screen pixel** | `globalLogical × scale`. | At scale 6, global `(1, 1)` → screen `(6, 6)`. |

Isometric components add a **grid** space above these three (see
[isometric-projection.md](isometric-projection.md)).

## ViewComponent API

```java
public interface ViewComponent {
    void update(long deltaTimeMs);

    /** Returns {width, height} in logical pixels. */
    default int[] measure() { return ZERO_SIZE; }

    /** Shared zero-size array for the default measure(). */
    int[] ZERO_SIZE = new int[2];

    /** Assigns local position (relative to parent) and allocated size. */
    default void layout(int x, int y, int w, int h) {}

    /** Renders at (parentX, parentY) — the parent's global logical position. */
    void render(SDL_Renderer renderer, int parentX, int parentY, int scale);

    default void destroy() {}
}
```

### `measure()`

- Called **bottom-up**: a container measures all children first, then
  combines their sizes with its own spacing rules.
- Returns `{width, height}` in **logical pixels** (before scale).
- Should be fast and idempotent — may be called multiple times by a parent
  during layout negotiation.
- A leaf component (e.g. `TextComponent`) calculates its size from content.

### `layout(x, y, w, h)`

- Called **top-down**: the parent calls `layout()` on each child after
  deciding the child's rectangle.
- `(x, y)` — position **relative to the parent** in logical pixels.
- `(w, h)` — the allocated space (may differ from `measure()` if the parent
  needs to stretch or shrink the child).
- The component should store `x` and `y` for use in `render()`.

### `render(renderer, parentX, parentY, scale)`

- `(parentX, parentY)` — the parent container's own **global logical
  position**. The root is called with `(0, 0)`.
- A leaf computes its global position:
  ```java
  int globalX = parentX + layoutX;
  int globalY = parentY + layoutY;
  ```
- A container passes its own global position to children:
  ```java
  int myGlobalX = parentX + layoutX;
  int myGlobalY = parentY + layoutY;
  child.render(renderer, myGlobalX, myGlobalY, scale);
  ```

## Layout Lifecycle

```
Main loop:
  ┌─ Event handling
  ├─ (optional) measure() + layout() — when scale changes
  ├─ update(deltaTimeMs)
  ├─ render(renderer, 0, 0, scale)  ──▶  root → children → grandchildren...
  └─ SDL_RenderPresent
```

`measure()` and `layout()` are called:
- Once after the component tree is created.
- Whenever scale changes (zoom in/out).

They are **not** called every frame — the component positions are stable until
the layout is invalidated (typically by scale change).

## TextComponent

A leaf component that renders a single line of text.

```java
// Construct with an explicit font:
TextComponent(BitmapFont font, String text, int size, int r, int g, int b)

// Convenience: uses AssetManager's "monogram/font":
TextComponent(String text, int size, int r, int g, int b)
```

- `measure()` returns the text's logical width/height via
  `BitmapFontRenderer.measureText()`.
- `render()` calls `BitmapFontRenderer.renderText()` at the computed global
  position.
- `setText(String)` updates the displayed text (e.g. scale indicator).

### Example

```java
TextComponent title = new TextComponent(font, "My Title", 1, 255, 255, 255);
title.measure();              // → {measureWidth, GLYPH_HEIGHT}
title.layout(10, 10, 0, 0);   // position at (10, 10) relative to parent
title.render(renderer, 0, 0, scale);  // renders at screen (10*scale, 10*scale)
```

---

## FlexLayout

A container that lays out children sequentially in a row or column with
uniform gap. Similar to a simplified CSS Flexbox with `flex-direction:
row | column` and `gap`.

```java
FlexLayout layout = new FlexLayout(FlexLayout.Direction.ROW, gapInPixels);
layout.addChild(child1);
layout.addChild(child2);
```

### `measure()`

Sums the main-axis size of all children, adds gaps between them, and uses
the maximum cross-axis size.

| Direction | Width | Height |
|---|---|---|
| `ROW` | `∑ child.width + gap × (n−1)` | `max child.height` |
| `COLUMN` | `max child.width` | `∑ child.height + gap × (n−1)` |

### `layout(x, y, w, h)`

Positions children sequentially along the main axis, each with their measured
size (the allocated cross-axis size is passed through for stretch-aware
children).

### Usage

```java
FlexLayout toolbar = new FlexLayout(FlexLayout.Direction.ROW, 4);
toolbar.addChild(new TextComponent(font, "File", 1, 200, 200, 200));
toolbar.addChild(new TextComponent(font, "Edit", 1, 200, 200, 200));
toolbar.addChild(new TextComponent(font, "Help", 1, 200, 200, 200));
toolbar.measure();
toolbar.layout(0, 0, toolbarWidth, toolbarHeight);
```

---

## GridLayout

A container that places children in a fixed-column grid with uniform
horizontal and vertical gaps. Similar to a simplified CSS Grid with
`grid-template-columns: repeat(N, 1fr)`.

```java
GridLayout grid = new GridLayout(columns, gapX, gapY);
grid.addChild(child1);
grid.addChild(child2);
```

### `measure()`

Finds the maximum child width and height across all children, then computes
the total grid size:

```
cellW = max(child[].width)
cellH = max(child[].height)
width  = columns × cellW + (columns − 1) × gapX
height = rows × cellH + (rows − 1) × gapY
```

### `layout(x, y, w, h)`

If `w > 0 && h > 0`, cells are sized to fit the allocated space
(`(w − gaps) / columns`). Otherwise each cell gets the measured max size.

### Usage

```java
GridLayout palette = new GridLayout(4, 2, 2);
palette.addChild(new TextComponent(font, "Red", 1, 255, 0, 0));
palette.addChild(new TextComponent(font, "Green", 1, 0, 255, 0));
palette.addChild(new TextComponent(font, "Blue", 1, 0, 0, 255));
palette.addChild(new TextComponent(font, "White", 1, 255, 255, 255));
palette.measure();
palette.layout(0, 0, 0, 0);
```

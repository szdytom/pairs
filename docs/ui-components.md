Component Reference
===================

This document describes the concrete classes that make up the UI widget
tree.  See [ui-system.md](ui-system.md) for the high-level architecture
and [ui-events.md](ui-events.md) for the event dispatch model.

---

## Widget (abstract)

Base class for all visual components.  Every widget in the tree extends
`Widget` directly or through a subclass.

### Fields

| Field | Access | Default | Description |
|---|---|---|---|
| `parent` | package | `null` | Parent widget, set by `Container.addChild()` |
| `visible` | package | `true` | Visibility flag |
| `layoutX, layoutY` | package | `0` | Position relative to parent (logical px) |
| `layoutW, layoutH` | package | `0` | Allocated size (logical px) |

### Methods

```java
// Visibility
void setVisible(boolean v);
boolean isVisible();

// Event handling — override to handle events
boolean onEvent(Event event);   // return true to stop propagation

// Event dispatch — entry point for the tree
boolean dispatchEvent(Event event, int myGlobalX, int myGlobalY);
```

### Inherited from ViewComponent

`Widget` provides default (no-op) implementations for `update()`,
`layout()`, and `destroy()`.  Subclasses override `measure()` and
`render()`.

Layout coordinates are stored automatically by Widget's default `layout()`
implementation.

---

## Container (abstract)

A widget that manages a list of child widgets.  Extends `Widget`.

```java
// Child management
void addChild(Widget child);     // sets child.parent = this
void removeChild(Widget child);  // sets child.parent = null
```

### Auto-Traversal

Container provides automatic top-down traversal for the core lifecycle
methods:

| Method | Behavior |
|---|---|
| `update(deltaTimeMs)` | Calls `child.update()` on each **visible** child |
| `render(renderer, parentX, parentY, scale)` | Computes global position, calls `child.render()` on each **visible** child |
| `destroy()` | Calls `child.destroy()` on **all** children, then clears child list |

`measure()` and `layout()` are **not** defaulted — each container subclass
implements its own layout strategy (see FlexLayout, GridLayout below).

### Event Dispatch

`Container.dispatchEvent()` overrides Widget's implementation to provide
hit-testing:

- **Mouse events**: walks children back-to-front (reverse insertion order),
  finds the deepest visible child under the cursor, dispatches to it.  If the
  child does not handle the event, the container's own `onEvent()` is called.
- **Non-mouse events** (keyboard): calls `onEvent()` directly on self.

---

## TextComponent

A leaf widget that renders a single line of text using a bitmap font.

```java
// Construct with an explicit font:
TextComponent(BitmapFont font, String text, int size, int r, int g, int b)

// Convenience: uses AssetManager's "monogram/font" singleton:
TextComponent(String text, int size, int r, int g, int b)

void setText(String text);
String text();
```

- `measure()` returns the text's logical width/height via
  `BitmapFontRenderer.measureText()`.
- `render()` calls `BitmapFontRenderer.renderText()` at the computed global
  position.
- `update()` is a no-op — text content changes are driven by `setText()` calls
  from the owning component.

---

## FlexLayout

A container that lays out children sequentially in a row or column with
uniform gap.  Similar to a simplified CSS Flexbox.

```java
FlexLayout layout = new FlexLayout(FlexLayout.Direction.ROW, gapInPixels);
layout.addChild(child1);
layout.addChild(child2);
```

### Direction

`FlexLayout.Direction.ROW` — children laid out horizontally.
`FlexLayout.Direction.COLUMN` — children laid out vertically.

### measure()

Sums the main-axis size of all children, adds gaps, and uses the maximum
cross-axis size.

| Direction | Measured width | Measured height |
|---|---|---|
| `ROW` | `Σ child.width + gap × (n − 1)` | `max child.height` |
| `COLUMN` | `max child.width` | `Σ child.height + gap × (n − 1)` |

### layout(x, y, w, h)

Positions children sequentially along the main axis.  Each child gets its
measured size on the main axis and the allocated size on the cross axis.

---

## GridLayout

A container that places children in a fixed-column grid with uniform gaps.
Similar to a simplified CSS Grid.

```java
GridLayout grid = new GridLayout(columns, gapX, gapY);
grid.addChild(child1);
grid.addChild(child2);
```

### measure()

Finds the maximum child width and height across all children, then computes
the total grid size:

```
cellW = max(child[].width)
cellH = max(child[].height)
width  = columns × cellW + (columns − 1) × gapX
height = rows × cellH + (rows − 1) × gapY
```

### layout(x, y, w, h)

If `w > 0 && h > 0`, cells are sized to fill the allocated space
(`(w − gaps) / columns`).  Otherwise each cell gets the measured max size.
Children are placed left-to-right, top-to-bottom.

UI System
=========

This document provides a high-level overview of the UI system architecture.
See the linked sub-documents for detailed API references.

- [Event System](ui-events.md) — event dispatch, hit testing, bubbling
- [Component Reference](ui-components.md) — Widget, Container, layout classes, TextComponent

Read [UI Guidelines](ui-guidelines.md) for best practices and conventions when working on UI code.

---

## Architecture Overview

The UI is a **retained-mode widget tree**: a hierarchy of components (widgets)
that own persistent state and manage their own lifecycle.  The tree is rooted
at a single component (typically `LevelComponent`) which is driven by the
application's main loop.

```
ViewComponent (interface)
    │
    ├── Widget (abstract class)
    │   │   - parent pointer, visible flag
    │   │   - layout position storage
    │   │   - event dispatch entry point
    │   │
    │   ├── Container (abstract class)
    │   │   │   - children list, addChild/removeChild
    │   │   │   - auto-traversal of update/render/destroy
    │   │   │   - event hit-test + bubble dispatch
    │   │   │
    │   │   ├── LevelComponent (game root)
    │   │   ├── FlexLayout (row/column box)
    │   │   └── GridLayout (fixed-column grid)
    │   │
    │   ├── TextComponent (text leaf)
    │   └── IsometricGridView (isometric tile leaf)
```

The key design split: **Container** handles traversal and event routing
automatically; leaf widgets (TextComponent, IsometricGridView) only implement
their own `render()` and `measure()`.

---

## Lifecycle

Each frame in the main loop:

```
Main loop:
  ├─ Event handling           — SDL events → dispatchEvent() on root
  ├─ (re-layout if needed)    — measure() + layout() on scale change
  ├─ update(deltaTimeMs)      — root.update() traverses tree
  ├─ render(renderer, ...)    — root.render() traverses tree
  └─ SDL_RenderPresent
```

### Per-component lifecycle

1. **`measure()`** — bottom-up. Each component reports its natural logical-pixel
   size without knowing its final position.  A container measures all children
   first, then combines their sizes.
2. **`layout(x, y, w, h)`** — top-down. The root assigns each component a
   rectangle.  A container computes positions for its children and calls
   `layout()` on each.
3. **`update(deltaTimeMs)`** — top-down per-frame state update.  Container
   automatically iterates visible children.
4. **`render(renderer, parentX, parentY, scale)`** — top-down per-frame
   rendering.  Container automatically iterates visible children.
5. **`destroy()`** — releases native resources (SDL textures, surfaces).
   Container automatically iterates all children, then clears the child list.

`measure()` and `layout()` are called once after construction and whenever the
layout is invalidated (typically by a scale change).  They are **not** called
every frame.

`update()` and `render()` are called every frame.  Container implementations
inherit automatic top-down traversal — only leaf widgets and components with
custom pre-render work (e.g., `LevelComponent` caching grid coordinates) need
to override `render()`.

---

## Coordinate Systems

All 2D components use three coordinate spaces:

| Space | Description | Example |
|---|---|---|
| **Local logical pixel** | Position relative to parent, set by `layout()`. | A `TextComponent` at `(1, 1)` means 1 px right of the parent's left edge. |
| **Global logical pixel** | `parentGlobal + localLogical`. Computed each frame. | A leaf at local `(1, 1)` inside a root at `(0, 0)` has global `(1, 1)`. |
| **Screen pixel** | `globalLogical × scale`. | At scale 6, global `(1, 1)` → screen `(6, 6)`. |

Isometric components add a **grid** space above these three (see
[isometric-projection.md](isometric-projection.md)).

The **scale factor** is never stored in components — it is passed as a
parameter to `render()` and used for on-the-fly conversion.  This allows
dynamic zoom without updating the entire tree.

---

## Visibility

Every widget has a `visible` flag (default `true`).  Container's `update()` and
`render()` skip invisible children automatically.  `destroy()` traverses all
children regardless of visibility.

Use `setVisible(false)` to temporarily hide a component without destroying it
(e.g., a "CLEARED!" message that appears on level completion).

---

## Event Dispatch

Mouse events use a **first-hit-wins** model (see [Event System](ui-events.md)):

1. SDL events are converted to `Event` objects and dispatched to the root
2. Mouse events: the container walks its visible children front-to-back
   (reverse insertion order).  The first child whose bounds contain the
   cursor receives the event.
3. If the child handles it (returns `true`), dispatch stops.  If the
   child returns `false`, the event bubbles immediately to the container's
   own `onEvent()` — siblings further back are not tried.
4. If no child's bounds contain the point, the container's `onEvent()` is
   called directly.
5. Keyboard events: dispatched directly to the root.

Event System
============

This document describes the UI event dispatch mechanism, including the event
class hierarchy, hit-testing rules, and how events flow from SDL through the
component tree.

---

## Class Hierarchy

```
Event (base)
  ├── MouseEvent   — carries x, y (root-relative logical pixels), button
  └── KeyEvent     — carries keycode (SDL_Keycode)
```

### Event.Type

All event types are defined in `Event.Type`:

| Constant | Applicable Event | Meaning |
|---|---|---|
| `MOUSE_MOVED` | MouseEvent | Cursor moved |
| `MOUSE_PRESSED` | MouseEvent | Mouse button pressed |
| `MOUSE_RELEASED` | MouseEvent | Mouse button released |
| `MOUSE_LEAVE` | MouseEvent | Cursor left the window |
| `KEY_PRESSED` | KeyEvent | Key pressed |
| `KEY_RELEASED` | KeyEvent | Key released |

---

## Dispatch Model: Target → Root Bubble

Events flow from the deepest target component upward:

```
SDL event in Main.java
  │
  └─► root.dispatchEvent(event, 0, 0)
        │
        ├── Mouse event: hit-test children front-to-back;
        │   the first visible child whose bounds contain the point receives
        │   the event.  If the child does not handle it (returns false),
        │   the event bubbles immediately to this container's own
        │   onEvent() — siblings further back are not tried.
        │
        └── Keyboard event: root.onEvent() directly
```

### Hit Testing

`Container.dispatchEvent()` walks children in reverse insertion order
(topmost first).  For each visible child, it checks whether the event
coordinates fall within the child's bounding box (in global logical pixels).
The first match receives the event.  If the child's `onEvent()` returns
`false` (unhandled), the event bubbles to this container's `onEvent()`
immediately — siblings further back are not tried.  If no child's bounds
contain the point, the container's `onEvent()` is also called.

### Stopping Propagation

Override `onEvent()` in your component:

```java
@Override
public boolean onEvent(Event event) {
    if (event instanceof MouseEvent me) {
        if (me.type() == Event.Type.MOUSE_PRESSED) {
            // handle click
            return true;  // event consumed — stops bubbling
        }
    }
    return false;  // not handled — bubble to parent
}
```

Alternatively, call `event.consume()` to mark the event as consumed.

---

## Input Flow from SDL

In `Main.java`, SDL events are converted to `Event` objects and dispatched:

```java
// Mouse motion
case SDL_MOUSEMOTION:
    level.dispatchEvent(
        new MouseEvent(Event.Type.MOUSE_MOVED,
            evt.motion.x / scale, evt.motion.y / scale, 0),
        0, 0
    );
    break;

// Mouse click
case SDL_MOUSEBUTTONDOWN:
    level.dispatchEvent(
        new MouseEvent(Event.Type.MOUSE_PRESSED,
            evt.button.x / scale, evt.button.y / scale, evt.button.button),
        0, 0
    );
    break;

// Keyboard
case SDL_KEYDOWN:
    if (evt.key.keysym.sym == SDLK_ESCAPE) {
        shouldRun = false;  // application-level: handled inline
    } else {
        level.dispatchEvent(
            new KeyEvent(Event.Type.KEY_PRESSED, evt.key.keysym.sym),
            0, 0
        );
    }
    break;
```

Application-level events (ESC to quit, ± to zoom) are handled inline in
`Main.java` and never reach the component tree.  Game-level events (mouse
clicks, SPACE to restart) are dispatched through the tree.

---

## Adding a New Event Type

1. Add a constant to `Event.Type`
2. Create a subclass of `Event` if the event carries new data
3. Dispatch it from `Main.java` (or wherever the source is)
4. Override `onEvent()` in the target component

---

## Custom Hit Testing

The default hit test uses axis-aligned bounding boxes (the widget's `layoutW`
× `layoutH`).  Components with irregular shapes (e.g., isometric tiles) use
their own hit-test logic inside `onEvent()` rather than relying on the
container's bounding-box dispatch.

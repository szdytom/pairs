# UI Guidelines

This note describes the general rules and best practices for UI development in the game. These instructions MUST be STRICTLY followed.

## General Instructions

- Never store the scale factor in a component. It should be used on-the-fly during `render()`, and then discarded. This allows the UI to be scaled dynamically without needing to update all components (IMPORTANT).
- Always use logical pixels for layout and rendering.
- Always use TextComponent for text. Never render text directly in a custom component.
- Avoid custom layout logic in components. Use a layout container (FlexLayout, GridLayout) to position children instead. These containers are currently of primitive functionality, extend them as needed in a reusable way.

## Lifecycle

- Expect `measure()` returns the same object reference on each call. Never store the return value of `measure()` in a field (clone it if you really need to store it).
- All SDL_Texture objects should be cleaned up in `destroy()`. Never rely on Java's garbage collection to clean up native resources.

## References

- [UI Layout](ui-layout.md)
- [Isometric Projection](isometric-projection.md)
- [SDL Related](libsdl4j.md)

Feel free to extract source from libsdl4j jar and read the JNI binding code for more details on how SDL functions work, if not documented in libsdl4j.md. Every time you did so, document your findings in libsdl4j.md for future reference.

## Performance Considerations

- Cache SDL_Textures if possible.
- Avoid allocating new objects during `update()`, `measure()`, `layout()`, or `render()`. These methods are called very frequently. Pre-allocate necessary objects. For example, the return value of `measure()` (`int[]`) should be pre-allocated, and the same array can be reused for multiple calls.

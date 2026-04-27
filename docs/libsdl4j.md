LibSDL4J
========

LibSDL4J is a mapping of SDL2 APIs to Java. There are two goals for LibSDL4J:

* Provide as direct of a mapping of SDL APIs as possible.<br/>
  It should be easy to just 1:1 translate C-style source code to Java.
* Provide reasonably performant mapping.

Because of these goals, there is a lot of room for Java niceties (enums, encapsulation, AutoClosable, type-safety, exceptions etc.)
which are intentionally avoided. These can be applied by wrapping the raw API,
but it is outside the scope of this project.

If you have LibSDL4J set up as a dependency of your project,
you can try to a sample program:

~~~
import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.video.SDL_Window;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_EVERYTHING;
import static io.github.libsdl4j.api.error.SdlError.SDL_GetError;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.render.SDL_RendererFlags.SDL_RENDERER_ACCELERATED;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_RESIZABLE;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_SHOWN;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_CreateWindow;
import static io.github.libsdl4j.api.video.SdlVideoConst.SDL_WINDOWPOS_CENTERED;

public class Demo {

    public static void main(String[] args) {
        // Initialize SDL
        int result = SDL_Init(SDL_INIT_EVERYTHING);
        if (result != 0) {
            throw new IllegalStateException("Unable to initialize SDL library (Error code " + result + "): " + SDL_GetError());
        }

        // Create and init the window
        SDL_Window window = SDL_CreateWindow("Demo SDL2", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, 1024, 768, SDL_WINDOW_SHOWN | SDL_WINDOW_RESIZABLE);
        if (window == null) {
            throw new IllegalStateException("Unable to create SDL window: " + SDL_GetError());
        }

        // Create and init the renderer
        SDL_Renderer renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);
        if (renderer == null) {
            throw new IllegalStateException("Unable to create SDL renderer: " + SDL_GetError());
        }

        // Set color of renderer to green
        SDL_SetRenderDrawColor(renderer, (byte) 0, (byte) 255, (byte) 0, (byte) 255);

        // Clear the window and make it all red
        SDL_RenderClear(renderer);

        // Render the changes above ( which up until now had just happened behind the scenes )
        SDL_RenderPresent(renderer);

        // Start an event loop and react to events
        SDL_Event evt = new SDL_Event();
        boolean shouldRun = true;
        while (shouldRun) {
            while (SDL_PollEvent(evt) != 0) {
                switch (evt.type) {
                    case SDL_QUIT:
                        shouldRun = false;
                        break;
                    case SDL_KEYDOWN:
                        if (evt.key.keysym.sym == SDLK_SPACE) {
                            System.out.println("SPACE pressed");
                        }
                        break;
                    case SDL_WINDOWEVENT:
                        System.out.println("Window event " + evt.window.event);
                    default:
                        break;
                }
            }
        }

        SDL_Quit();
    }
}
~~~

You should be able to follow any C-style tutorial,
just tweak it very slightly to make it working in Java.

## Common Issues

### SDL_BlitSurface

In C SDL2, `SDL_BlitSurface` is a direct function. In LibSDL4J:

- `SdlSurface.SDL_BlitSurface(src, srcRect, dst, dstRect)` - static method on `SdlSurface` class
- `SDL_blit` is a **functional interface**, not a utility class - don't try to call static methods on it

```java
// Correct:
SdlSurface.SDL_BlitSurface(source, srcRect, tileSurface, dstRect);

// Wrong (SDL_blit is an interface):
SDL_blit.SDL_BlitSurface(...);  // compile error
```

### Pixel Format for SDL_CreateRGBSurface

When creating surfaces, use these mask values in the correct order:

```java
SDL_CreateRGBSurface(0, width, height, 32,
    0x000000FF,  // R mask (actually first byte due to endianness)
    0x0000FF00,  // G mask
    0x00FF0000,  // B mask
    0xFF000000); // A mask
```

This creates an RGBA surface in memory order, which matches what `ImageOperation` produces when converting from ARGB BufferedImage.

### Pixel-Perfect Scaling with SDL_RenderCopy

For crisp pixel art, everything must stay in integers. Using `float` for scale factors and casting to `int` at the last moment causes truncation drift: a `1.1f` scale on a 16px tile gives `17` pixels, and half-steps like `17 / 2 = 8` accumulate error across the grid, making tiles visibly misalign toward the bottom-right.

Correct approach:
- Keep `scale` as `int`.
- Compute destination size as `dst.w = TILE_WIDTH * scale`.
- Compute destination position with pure integer math: `dst.x = screenX - dst.w / 2`.
- Pass `scale` directly into the per-frame `render()` call rather than caching it in fields. Cached scale risks desynchronizing the projection math from the actual render size.

```java
// Correct: integer scale, passed each frame
void render(SDL_Renderer renderer, int scale) {
    int dstW = TILE_WIDTH * scale;   // exact multiple
    int dstH = TILE_HEIGHT * scale;
    dst.x = screenX - dstW / 2;      // exact pixel position
    dst.y = screenY - dstH / 2;
    dst.w = dstW;
    dst.h = dstH;
    SDL_RenderCopy(renderer, tex, src, dst);
}
```

Set nearest-neighbor filtering before any textures are created:

```java
SdlHints.SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "nearest");
```

### SDL_RenderCopy Source Rectangle

When creating textures from cropped tile surfaces, the texture dimensions match the surface exactly. `SDL_RenderCopy` stretches the `src` rectangle to fit the `dst` rectangle. The stretch factor is `dst.w / src.w` and `dst.h / src.h`. If both are integer multiples of the original tile size, SDL produces clean pixel doubling with no blur.

If the sprite sheet has 1px padding between tiles, subtract it in the `src` rectangle, **not** in the destination rectangle, or the stretch ratio will be wrong:

```java
// Extract 16x16 content from an 18x18 cropped tile
SDL_Rect src = new SDL_Rect();
src.x = 1;  // skip left padding
src.y = 1;  // skip top padding
src.w = 16; // actual content width
src.h = 16; // actual content height

SDL_Rect dst = new SDL_Rect();
dst.w = 16 * scale;  // scale applied only here
dst.h = 16 * scale;
```

### Texture Lifecycle

`SDL_CreateTextureFromSurface` creates a GPU texture from a CPU surface. The surface can be freed after this call (use `SdlSurface.SDL_FreeSurface`), but the texture must eventually be freed with `SdlRender.SDL_DestroyTexture`. In this project textures are cached by type ID and persist for the application lifetime; for a production game, track them and clean up on shutdown.


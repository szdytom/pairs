package app.pairs;

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

import app.pairs.asset.AssetManager;

import com.sun.jna.ptr.IntByReference;

import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.video.SDL_Window;

public class Main {
	public static void main(String[] args) {
		// Initialize SDL
		int result = SDL_Init(SDL_INIT_EVERYTHING);
		if (result != 0) {
			throw new IllegalStateException(
				"Unable to initialize SDL library (Error code " + result
				+ "): " + SDL_GetError()
			);
		}

		// Create and init the window
		SDL_Window window = SDL_CreateWindow(
			"Demo SDL2", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, 1_024,
			768, SDL_WINDOW_SHOWN | SDL_WINDOW_RESIZABLE
		);
		if (window == null) {
			throw new IllegalStateException(
				"Unable to create SDL window: " + SDL_GetError()
			);
		}

		// Create and init the renderer
		SDL_Renderer renderer = SDL_CreateRenderer(
			window, -1, SDL_RENDERER_ACCELERATED
		);
		if (renderer == null) {
			throw new IllegalStateException(
				"Unable to create SDL renderer: " + SDL_GetError()
			);
		}

		// Initialize AssetManager with renderer
		AssetManager.instance().init(renderer);

		// Load assets
		try {
			AssetManager.instance().loadManifest("manifest.json");
		} catch (Exception e) {
			System.err.println("Failed to load assets: " + e.getMessage());
			e.printStackTrace();
			SDL_Quit();
			System.exit(1);
		}

		// Get the loaded texture
		SDL_Texture texture = AssetManager.instance().get("tinyblocks/texture");

		// Query texture dimensions
		IntByReference w = new IntByReference();
		IntByReference h = new IntByReference();
		SDL_QueryTexture(texture, null, null, w, h);

		// Calculate centered destination rect
		SDL_Rect dstRect = new SDL_Rect();
		dstRect.x = (1_024 - w.getValue()) / 2;
		dstRect.y = (768 - h.getValue()) / 2;
		dstRect.w = w.getValue();
		dstRect.h = h.getValue();

		// Set color of renderer to green
		SDL_SetRenderDrawColor(
			renderer, (byte)0, (byte)255, (byte)0, (byte)255
		);

		// Clear the window
		SDL_RenderClear(renderer);

		// Render the texture centered
		SDL_RenderCopy(renderer, texture, null, dstRect);

		// Render the changes
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

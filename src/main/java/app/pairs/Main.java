package app.pairs;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_EVERYTHING;
import static io.github.libsdl4j.api.error.SdlError.SDL_GetError;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_KEYDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEBUTTONDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEMOTION;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_QUIT;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_WINDOWEVENT;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.hints.SdlHintsConst.SDL_HINT_RENDER_SCALE_QUALITY;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_0;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_EQUALS;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_ESCAPE;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_MINUS;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.render.SDL_RendererFlags.SDL_RENDERER_ACCELERATED;
import static io.github.libsdl4j.api.render.SdlRender.SDL_CreateRenderer;
import static io.github.libsdl4j.api.render.SdlRender.SDL_DestroyRenderer;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderClear;
import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderPresent;
import static io.github.libsdl4j.api.render.SdlRender.SDL_SetRenderDrawColor;
import static io.github.libsdl4j.api.video.SDL_WindowEventID.SDL_WINDOWEVENT_LEAVE;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_RESIZABLE;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_SHOWN;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_CreateWindow;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_DestroyWindow;
import static io.github.libsdl4j.api.video.SdlVideoConst.SDL_WINDOWPOS_CENTERED;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.map.PresetTilemapFactory;
import app.pairs.map.TilemapPreset;
import app.pairs.view.IsometricMapper;
import app.pairs.view.LevelComponent;

import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.hints.SdlHints;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.video.SDL_Window;

public class Main {
	private static final int WINDOW_WIDTH = 1_024;
	private static final int WINDOW_HEIGHT = 768;
	private static final int TILE_WIDTH = 16;
	private static final int TILE_HEIGHT = 16;

	private static final int MIN_SCALE = 1;
	private static final int MAX_SCALE = 12;
	private static final int SCALE_STEP = 1;

	public static void main(String[] args) {
		int result = SDL_Init(SDL_INIT_EVERYTHING);
		if (result != 0) {
			throw new IllegalStateException(
				"Unable to initialize SDL: " + SDL_GetError()
			);
		}

		SDL_Window window = SDL_CreateWindow(
			"Pairs - Isometric View", SDL_WINDOWPOS_CENTERED,
			SDL_WINDOWPOS_CENTERED, WINDOW_WIDTH, WINDOW_HEIGHT,
			SDL_WINDOW_SHOWN | SDL_WINDOW_RESIZABLE
		);
		if (window == null) {
			throw new IllegalStateException(
				"Unable to create window: " + SDL_GetError()
			);
		}

		SDL_Renderer renderer = SDL_CreateRenderer(
			window, -1, SDL_RENDERER_ACCELERATED
		);
		if (renderer == null) {
			throw new IllegalStateException(
				"Unable to create renderer: " + SDL_GetError()
			);
		}

		SdlHints.SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "nearest");

		AssetManager.instance().init(renderer);

		try {
			AssetManager.instance().loadManifest("manifest.json");
		} catch (Exception e) {
			System.err.println("Failed to load assets: " + e.getMessage());
			e.printStackTrace();
			SDL_DestroyRenderer(renderer);
			SDL_DestroyWindow(window);
			SDL_Quit();
			System.exit(1);
		}

		TileRegistry tiles = AssetManager.instance().get("tiles/typed");
		TileRegistry hlTiles = AssetManager.instance().get("hl-tiles/typed");

		int scale = 6;

		IsometricMapper mapper = new IsometricMapper(TILE_WIDTH, TILE_HEIGHT);

		LevelComponent level = new LevelComponent(
			new PresetTilemapFactory(
				AssetManager.instance().<TilemapPreset>get("tilemap/hard")
			),
			tiles, hlTiles, mapper
		);
		relayout(level, scale);

		System.out.println(
			"Controls: +/- zoom | 0 reset scale | SPACE regenerate | ESC quit"
		);

		SDL_Event evt = new SDL_Event();
		boolean shouldRun = true;
		long lastTime = System.currentTimeMillis();

		while (shouldRun) {
			while (SDL_PollEvent(evt) != 0) {
				switch (evt.type) {
				case SDL_QUIT:
					shouldRun = false;
					break;
				case SDL_KEYDOWN:
					if (evt.key.keysym.sym == SDLK_ESCAPE) {
						shouldRun = false;
					} else if (evt.key.keysym.sym == SDLK_SPACE) {
						level.restart();
					} else if (evt.key.keysym.sym == SDLK_EQUALS) {
						scale = Math.min(MAX_SCALE, scale + SCALE_STEP);
						System.out.println("Scale: " + scale);
						relayout(level, scale);
					} else if (evt.key.keysym.sym == SDLK_MINUS) {
						scale = Math.max(MIN_SCALE, scale - SCALE_STEP);
						System.out.println("Scale: " + scale);
						relayout(level, scale);
					} else if (evt.key.keysym.sym == SDLK_0) {
						scale = 6;
						System.out.println("Scale reset to " + scale);
						relayout(level, scale);
					}
					break;
				case SDL_MOUSEMOTION:
					level.setMousePosition(
						evt.motion.x / scale, evt.motion.y / scale
					);
					break;
				case SDL_MOUSEBUTTONDOWN:
					level.handleClick();
					break;
				case SDL_WINDOWEVENT:
					if (evt.window.event == SDL_WINDOWEVENT_LEAVE)
						level.setMousePosition(-1, -1);
					break;
				}
			}

			long currentTime = System.currentTimeMillis();
			long deltaTime = currentTime - lastTime;
			lastTime = currentTime;

			SDL_SetRenderDrawColor(
				renderer, (byte)30, (byte)30, (byte)50, (byte)255
			);
			SDL_RenderClear(renderer);

			level.update(deltaTime);
			level.render(renderer, 0, 0, scale);

			SDL_RenderPresent(renderer);
		}

		level.destroy();
		AssetManager.instance().dispose();
		SDL_DestroyRenderer(renderer);
		SDL_DestroyWindow(window);
		SDL_Quit();
	}

	private static void relayout(LevelComponent level, int scale) {
		level.setScaleText(scale);
		level.measure();
		level.layout(0, 0, WINDOW_WIDTH / scale, WINDOW_HEIGHT / scale);
	}
}

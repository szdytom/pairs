package app.pairs;

import static io.github.libsdl4j.api.Sdl.*;
import static io.github.libsdl4j.api.SdlSubSystemConst.*;
import static io.github.libsdl4j.api.error.SdlError.*;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.*;
import static io.github.libsdl4j.api.hints.SdlHintsConst.*;
import static io.github.libsdl4j.api.render.SDL_RendererFlags.*;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.video.SDL_WindowEventID.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.*;

import app.pairs.asset.AssetManager;
import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.router.LevelPage;
import app.pairs.router.LoadPage;
import app.pairs.router.MainMenuPage;
import app.pairs.router.Router;
import app.pairs.router.TestPage;
import app.pairs.save.Database;
import app.pairs.user.User;
import app.pairs.user.UserManager;
import app.pairs.user.UserSession;
import app.pairs.view.Event;
import app.pairs.view.KeyEvent;
import app.pairs.view.MouseEvent;
import app.pairs.view.ScrollEvent;

import io.github.libsdl4j.api.event.*;
import io.github.libsdl4j.api.hints.*;
import io.github.libsdl4j.api.render.*;
import io.github.libsdl4j.api.video.*;

public class Main {
	public static void main(String[] args) {
		int result = SDL_Init(SDL_INIT_EVERYTHING);
		if (result != 0) {
			throw new IllegalStateException(
				"Unable to initialize SDL: " + SDL_GetError()
			);
		}

		SDL_Window window = SDL_CreateWindow(
			"Pairs - Isometric View", SDL_WINDOWPOS_CENTERED,
			SDL_WINDOWPOS_CENTERED, 1_024, 768,
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
			AudioManager.instance().init();
		} catch (Exception e) {
			System.err.println("Failed to load assets: " + e.getMessage());
			e.printStackTrace();
			SDL_DestroyRenderer(renderer);
			SDL_DestroyWindow(window);
			SDL_Quit();
			System.exit(1);
		}

		try {
			Database.instance();
		} catch (Exception e) {
			System.err.println(
				"Failed to initialize database: " + e.getMessage()
			);
			e.printStackTrace();
			SDL_DestroyRenderer(renderer);
			SDL_DestroyWindow(window);
			SDL_Quit();
			System.exit(1);
		}

		Router router = Router.instance();

		if (args != null && args.length > 0
		    && ("-t".equals(args[0]) || "--test".equals(args[0]))) {
			router.navigateTo(new TestPage());
		} else if (args == null || args.length == 0) {
			router.navigateTo(new MainMenuPage());
		} else if ("load".equals(args[0])) {
			seedAndNavigateToLoadPage();
		} else {
			GameState gameState = pickDifficulty(args);
			router.navigateTo(
				new LevelPage(gameState, LevelPage.DEFAULT_COUNTDOWN_MS)
			);
		}

		SDL_Event evt = new SDL_Event();
		boolean shouldRun = true;
		long lastTime = System.currentTimeMillis();

		while (shouldRun && !router.shouldQuit()) {
			while (SDL_PollEvent(evt) != 0) {
				switch (evt.type) {
				case SDL_QUIT:
					shouldRun = false;
					break;
				case SDL_KEYDOWN:
					router.onEvent(new KeyEvent(
						Event.Type.KEY_PRESSED, evt.key.keysym.sym,
						evt.key.keysym.mod
					));
					break;
				case SDL_MOUSEMOTION:
					router.onEvent(new MouseEvent(
						Event.Type.MOUSE_MOVED,
						evt.motion.x / router.getScale(),
						evt.motion.y / router.getScale(), 0
					));
					break;
				case SDL_MOUSEBUTTONDOWN:
					router.onEvent(new MouseEvent(
						Event.Type.MOUSE_PRESSED,
						evt.button.x / router.getScale(),
						evt.button.y / router.getScale(), evt.button.button
					));
					break;
				case SDL_MOUSEWHEEL:
					router.onEvent(new ScrollEvent(
						Event.Type.MOUSE_WHEEL,
						evt.wheel.mouseX / router.getScale(),
						evt.wheel.mouseY / router.getScale(), evt.wheel.y
					));
					break;
				case SDL_WINDOWEVENT:
					if (evt.window.event == SDL_WINDOWEVENT_SIZE_CHANGED) {
						router.windowResized(
							evt.window.data1, evt.window.data2
						);
					} else if (evt.window.event == SDL_WINDOWEVENT_LEAVE) {
						router.onEvent(
							new MouseEvent(Event.Type.MOUSE_LEAVE, -1, -1, 0)
						);
					}
					break;
				}
			}

			long currentTime = System.currentTimeMillis();
			long deltaTime = currentTime - lastTime;
			lastTime = currentTime;

			router.update(deltaTime);
			AudioManager.instance().update(deltaTime);
			router.render(renderer);

			SDL_RenderPresent(renderer);
		}

		router.shutdown();
		AudioManager.instance().close();
		AssetManager.instance().dispose();
		SDL_DestroyRenderer(renderer);
		SDL_DestroyWindow(window);
		SDL_Quit();
		Database.instance().close(); // close bd at last to ensure all pending
		                             // operations are completed
	}

	// Login as "abc" (creating the user on first run), seed a few saves if
	// the account has none yet, then jump straight to LoadPage.
	private static void seedAndNavigateToLoadPage() {
		User user = UserManager.login("abc", "abc")
						.or(() -> UserManager.register("abc", "abc"))
						.orElseThrow(
							()
								-> new IllegalStateException(
									"wrong password for user abc"
								)
						);
		UserSession.instance().setUser(user);

		String[] presets = {"easy", "medium", "hard", "extreme"};
		for (int i = 0; i < 10; i++) {
			user.saveGame(
				GameState.fromPreset("tilemap/" + presets[i % presets.length])
			);
		}

		Router.instance().navigateTo(new LoadPage());
	}

	/**
	 * Pick a {@link GameState} supplier from {@code args[0]} so the renderer
	 * can be launched against any difficulty. Defaults to {@code hard} when
	 * no arg is given.
	 */
	private static GameState pickDifficulty(String[] args) {
		String mode = args != null && args.length > 0
			? args[0].toLowerCase()
			: "hard";
		System.out.println("[Main] difficulty=" + mode);
		String id = "tilemap/" + mode;
		if (!AssetManager.instance().has(id)) {
			throw new IllegalArgumentException("unknown difficulty: " + mode);
		}
		return GameState.fromPreset(id);
	}
}

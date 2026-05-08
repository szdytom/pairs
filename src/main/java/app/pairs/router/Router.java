package app.pairs.router;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import app.pairs.view.Blackboard;
import app.pairs.view.Event;
import app.pairs.view.KeyEvent;
import app.pairs.view.Widget;

import io.github.libsdl4j.api.render.*;

public class Router {
	private static final int MIN_SCALE = 1;
	private static final int MAX_SCALE = 12;
	private static final int SCALE_STEP = 1;
	private static final int DEFAULT_SCALE = 6;

	private static final Router INSTANCE = new Router();

	private Page currentPage;
	private int scale = DEFAULT_SCALE;
	private Page pendingPage;
	private int windowWidth = 1_024;
	private int windowHeight = 768;

	public static Router instance() {
		return INSTANCE;
	}

	public void navigateTo(Page page) {
		if (currentPage == null) {
			currentPage = page;
			currentPage.onEnter();
			relayout();
			return;
		}
		pendingPage = page;
	}

	public void shutdown() {
		if (currentPage != null) {
			currentPage.onExit();
			currentPage.destroy();
			currentPage = null;
		}
	}

	public Page currentPage() {
		return currentPage;
	}

	public void update(long deltaTimeMs) {
		if (pendingPage != null) {
			if (currentPage != null) {
				currentPage.onExit();
				currentPage.destroy();
			}
			currentPage = pendingPage;
			pendingPage = null;
			currentPage.onEnter();
			relayout();
		}
		if (currentPage != null) {
			currentPage.update(deltaTimeMs);
		}
		Blackboard bb = getBlackboard();
		if (bb != null && bb.layoutDirty) {
			relayout();
			bb.layoutDirty = false;
		}
	}

	public void render(SDL_Renderer renderer) {
		SDL_SetRenderDrawColor(
			renderer, (byte)255, (byte)255, (byte)255, (byte)255
		);
		SDL_RenderClear(renderer);
		if (currentPage != null) {
			currentPage.render(renderer, scale);
		}
	}

	public boolean onEvent(Event event) {
		if (event instanceof KeyEvent ke
		    && ke.type() == Event.Type.KEY_PRESSED) {
			switch (ke.keycode()) {
			case SDLK_EQUALS -> {
				zoomIn();
				return true;
			}
			case SDLK_MINUS -> {
				zoomOut();
				return true;
			}
			case SDLK_0 -> {
				zoomReset();
				return true;
			}
			}
		}
		return currentPage != null && currentPage.onEvent(event);
	}

	public Widget getRoot() {
		return currentPage != null ? currentPage.getRoot() : null;
	}

	public Blackboard getBlackboard() {
		return currentPage != null ? currentPage.getBlackboard() : null;
	}

	public int getScale() {
		return scale;
	}

	public void windowResized(int w, int h) {
		windowWidth = w;
		windowHeight = h;
		relayout();
	}

	public void zoomIn() {
		scale = Math.min(MAX_SCALE, scale + SCALE_STEP);
		System.out.println("Scale: " + scale);
		relayout();
	}

	public void zoomOut() {
		scale = Math.max(MIN_SCALE, scale - SCALE_STEP);
		System.out.println("Scale: " + scale);
		relayout();
	}

	public void zoomReset() {
		scale = DEFAULT_SCALE;
		System.out.println("Scale reset to " + scale);
		relayout();
	}

	public void relayout() {
		Widget root = getRoot();
		if (root != null) {
			root.measure();
			root.layout(0, 0, windowWidth / scale, windowHeight / scale);
		}
	}

	private Router() {}
}

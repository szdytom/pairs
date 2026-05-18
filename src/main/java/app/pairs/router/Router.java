package app.pairs.router;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.keycode.SDL_Keymod.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import app.pairs.view.Blackboard;
import app.pairs.view.Event;
import app.pairs.view.KeyEvent;
import app.pairs.view.MouseEvent;
import app.pairs.view.Widget;

import io.github.libsdl4j.api.render.*;

public class Router {
	private static final Router INSTANCE = new Router();

	private final ScaleManager scaleManager = new ScaleManager();
	private Page currentPage;
	private Page pendingPage;
	private int windowWidth = 1_024;
	private int windowHeight = 768;
	private boolean quitRequested;

	private Router() {
		scaleManager.updateAutoScale(windowWidth, windowHeight);
		scaleManager.logScale(windowWidth, windowHeight);
	}

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

	public void quit() {
		quitRequested = true;
	}

	public boolean shouldQuit() {
		return quitRequested;
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
		setRenderDrawColor(renderer, rgb(255, 255, 255));
		SDL_RenderClear(renderer);
		if (currentPage != null) {
			currentPage.render(renderer, scaleManager.getScale());
		}
	}

	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
			Blackboard bb = getBlackboard();
			if (bb != null) {
				if (me.type() == Event.Type.MOUSE_MOVED) {
					bb.mouseX = me.x();
					bb.mouseY = me.y();
				} else if (me.type() == Event.Type.MOUSE_LEAVE) {
					bb.mouseX = bb.mouseY = -1;
				}
			}
		}
		if (event instanceof KeyEvent ke && ke.type() == Event.Type.KEY_PRESSED
		    && (ke.modifiers() & KMOD_CTRL) != 0) {
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
		return scaleManager.getScale();
	}

	public void windowResized(int w, int h) {
		windowWidth = w;
		windowHeight = h;
		scaleManager.updateAutoScale(windowWidth, windowHeight);
		scaleManager.logScale(windowWidth, windowHeight);
		relayout();
	}

	public void zoomIn() {
		scaleManager.zoomIn();
		scaleManager.logScale(windowWidth, windowHeight);
		relayout();
	}

	public void zoomOut() {
		scaleManager.zoomOut();
		scaleManager.logScale(windowWidth, windowHeight);
		relayout();
	}

	public void zoomReset() {
		scaleManager.zoomReset();
		scaleManager.logScale(windowWidth, windowHeight);
		relayout();
	}

	public void relayout() {
		Widget root = getRoot();
		if (root != null) {
			int s = scaleManager.getScale();
			int logicalW = (windowWidth + s - 1) / s;
			int logicalH = (windowHeight + s - 1) / s;
			root.measure();
			root.layout(0, 0, logicalW, logicalH);
		}
	}
}

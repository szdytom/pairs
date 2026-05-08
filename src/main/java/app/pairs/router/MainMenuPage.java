package app.pairs.router;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;

import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class MainMenuPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public MainMenuPage() {
		this.blackboard = new Blackboard();
		var text = new TextComponent(
			"Pairs - Press SPACE to Play", 2, 30, 30, 30
		);
		var layout = new AlignLayout();
		layout.setProp("h-align", AlignLayout.HAlign.CENTER);
		layout.setProp("v-align", AlignLayout.VAlign.CENTER);
		layout.addChild(text);
		layout.setBlackboard(blackboard);
		this.root = layout;
	}

	@Override
	public void update(long deltaTimeMs) {
		root.update(deltaTimeMs);
	}

	@Override
	public void render(SDL_Renderer renderer, int scale) {
		root.render(renderer, 0, 0, scale);
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof KeyEvent ke && ke.type() == Event.Type.KEY_PRESSED
		    && ke.keycode() == SDLK_SPACE) {
			// TODO: navigate to actual LevelPage when game starts
			return true;
		}
		return false;
	}

	@Override
	public void destroy() {
		root.destroy();
	}

	@Override
	public Widget getRoot() {
		return root;
	}

	@Override
	public Blackboard getBlackboard() {
		return blackboard;
	}
}

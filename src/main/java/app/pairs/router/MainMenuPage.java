package app.pairs.router;

import app.pairs.logic.GameState;
import app.pairs.map.TilemapFactory;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class MainMenuPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public MainMenuPage() {
		this.blackboard = new Blackboard();
		this.root = new MainMenuComponent(this::startGame, this::quitGame);
		root.setBlackboard(blackboard);
	}

	private void startGame() {
		var gameState = new GameState(
			TilemapFactory.fromPreset("tilemap/medium")
		);
		Router.instance().navigateTo(new LevelPage(gameState, 180_000L));
	}

	private void quitGame() {
		Router.instance().quit();
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
		return root.dispatchEvent(event, 0, 0);
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

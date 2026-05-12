package app.pairs.router;

import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.view.Blackboard;
import app.pairs.view.Event;
import app.pairs.view.LevelComponent;
import app.pairs.view.Widget;

import io.github.libsdl4j.api.render.*;

public class LevelPage implements Page {
	private final LevelComponent root;
	private final Blackboard blackboard;

	public LevelPage(GameState gameState, long totalCountdownMs) {
		this.blackboard = new Blackboard();
		this.root = new LevelComponent(gameState, totalCountdownMs, blackboard);
	}

	@Override
	public void onEnter() {
		AudioManager.instance().playWithFadeIn(
			"LevelMusic", "minecraft", 3000f
		);
	}

	@Override
	public void onExit() {
		AudioManager.instance().fadeOutMusic(1000f);
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

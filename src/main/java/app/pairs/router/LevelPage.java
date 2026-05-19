package app.pairs.router;

import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.user.User;
import app.pairs.user.UserSession;
import app.pairs.view.Blackboard;
import app.pairs.view.Event;
import app.pairs.view.LevelComponent;
import app.pairs.view.Widget;

import io.github.libsdl4j.api.render.*;

public class LevelPage implements Page {
	public static final long DEFAULT_COUNTDOWN_MS = 300_000L;

	private final LevelComponent root;
	private final Blackboard blackboard;

	public LevelPage(GameState gameState, long totalCountdownMs) {
		this.blackboard = new Blackboard();
		this.root = new LevelComponent(gameState, totalCountdownMs, blackboard);
	}

	/**
	 * Load from save: restart always uses totalCountdownMs, but the level
	 * starts from initialRemainingMs.
	 */
	public LevelPage(
		GameState gameState, long totalCountdownMs, long initialRemainingMs
	) {
		this.blackboard = new Blackboard();
		this.root = new LevelComponent(
			gameState, totalCountdownMs, initialRemainingMs, blackboard
		);
	}

	@Override
	public void onEnter() {
		AudioManager.instance().fadeOutMusic(3000f);
		AudioManager.instance().shufflePlayWithFadeIn("LevelMusic", 3000f);
	}

	@Override
	public void onExit() {
		AudioManager.instance().fadeOutMusic(3000f);
		User user = UserSession.instance().getUser();
		if (!user.isAuthorized()) {
			return;
		}
		GameState gs = blackboard.get(GameState.class);
		Long id = UserSession.instance().getActiveSaveId();
		if (root.isCleared()) {
			if (id != null) {
				user.updateSave(id, gs);
				user.deleteSave(id);
				UserSession.instance().setActiveSaveId(null);
			}
		} else if (!root.isTimedOut()) {
			// mid-game: overwrite existing save or create a new one
			if (id != null) {
				user.updateSave(id, gs);
			} else {
				long newId = user.saveGame(gs);
				UserSession.instance().setActiveSaveId(newId);
			}
		}
		// timed out and not cleared: no save action
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

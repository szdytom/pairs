package app.pairs.router;

import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.map.DifficultyParams;
import app.pairs.map.RogueDifficultyGenerator;
import app.pairs.model.CountdownState;
import app.pairs.model.ItemType;
import app.pairs.model.RogueSession;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class RoguePlayPage implements Page {
	private final RogueSession session;
	private final Blackboard blackboard;
	private final LevelComponent levelComponent;
	private boolean synced;
	private boolean transitioning;
	private long transitionTimer;

	public RoguePlayPage(RogueSession session, DifficultyParams params) {
		this.session = session;
		this.blackboard = new Blackboard();
		this.levelComponent = createLevel(params);
	}

	@Override
	public void onEnter() {
		AudioManager.instance().fadeOutMusic(3000f);
		AudioManager.instance().shufflePlayWithFadeIn("LevelMusic", 3000f);
	}

	@Override
	public void onExit() {
		AudioManager.instance().fadeOutMusic(3000f);
		syncToSession();
	}

	private LevelComponent createLevel(DifficultyParams params) {
		var gs = RogueDifficultyGenerator.generate(params);
		gs.gameStatus.items.set(
			ItemType.AUTO_SOLVER, session.items.get(ItemType.AUTO_SOLVER)
		);
		gs.gameStatus.items.set(ItemType.TNT, session.items.get(ItemType.TNT));
		return new LevelComponent(gs, session, blackboard);
	}

	@Override
	public void update(long deltaTimeMs) {
		if (transitioning) {
			transitionTimer -= deltaTimeMs;
			if (transitionTimer <= 0) {
				Router.instance().navigateTo(new RogueShopPage(session));
			}
			return;
		}

		levelComponent.update(deltaTimeMs);

		if (levelComponent.isCleared()) {
			syncToSession();
			transitioning = true;
			transitionTimer = 1_500;
		} else if (levelComponent.isTimedOut()) {
			syncToSession();
			Router.instance().navigateTo(new RogueResultPage(session));
		}
	}

	private void syncToSession() {
		if (synced) {
			return;
		}
		synced = true;
		CountdownState cs = blackboard.get(CountdownState.class);
		GameState gs = blackboard.get(GameState.class);
		session.remainingMs = cs.remainingMs;
		session.spendableScore += gs.gameStatus.score;
		session.items.set(
			ItemType.AUTO_SOLVER, gs.gameStatus.items.get(ItemType.AUTO_SOLVER)
		);
		session.items.set(ItemType.TNT, gs.gameStatus.items.get(ItemType.TNT));
	}

	@Override
	public void render(SDL_Renderer renderer, int scale) {
		levelComponent.render(renderer, 0, 0, scale);
	}

	@Override
	public boolean onEvent(Event event) {
		return levelComponent.dispatchEvent(event, 0, 0);
	}

	@Override
	public void destroy() {
		levelComponent.destroy();
	}

	@Override
	public Widget getRoot() {
		return levelComponent;
	}

	@Override
	public Blackboard getBlackboard() {
		return blackboard;
	}
}

package app.pairs.router;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameType;
import app.pairs.model.RogueSession;
import app.pairs.save.SaveEntry;
import app.pairs.user.User;
import app.pairs.user.UserSession;
import app.pairs.view.*;

import java.util.Optional;

import io.github.libsdl4j.api.render.*;

public class LoadPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public LoadPage() {
		this.blackboard = new Blackboard();
		this.root = new LoadComponent(this::goBack, this::handleSelect);
		root.setBlackboard(blackboard);
	}

	private void goBack() {
		Router.instance().navigateTo(new MainMenuPage());
	}

	private void handleSelect(SaveEntry entry) {
		User user = UserSession.instance().getUser();
		if (entry.type() == GameType.ROGUE) {
			user.loadRogue().ifPresentOrElse(
				data
				-> {
					if (data.hasMap()) {
						Router.instance().navigateTo(
							new RoguePlayPage(data.session(), data.gameState())
						);
					} else {
						Router.instance().navigateTo(
							new RogueShopPage(data.session())
						);
					}
				},
				()
					-> Router.instance().navigateTo(
						new RogueShopPage(new RogueSession())
					)
			);
		} else {
			user.loadSave(entry.id()).ifPresent(snapshot -> {
				GameState gameState = GameState.fromSnapshot(snapshot);
				UserSession.instance().setActiveSaveId(entry.id());
				long remaining = snapshot.status().remainingMs() > 0
					? snapshot.status().remainingMs()
					: LevelPage.DEFAULT_COUNTDOWN_MS;
				Router.instance().navigateTo(new LevelPage(
					gameState, LevelPage.DEFAULT_COUNTDOWN_MS, remaining
				));
			});
		}
	}

	@Override
	public void onEnter() {
		Router.instance().setTitle("Pairs - Load Game");
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

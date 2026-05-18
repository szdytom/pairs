package app.pairs.router;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameType;
import app.pairs.model.ItemType;
import app.pairs.model.RogueSession;
import app.pairs.model.RogueSnapshot;
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
			Optional<RogueSnapshot> snap = user.loadRogue();
			if (snap.isPresent() && snap.get().relatedMapId() != null) {
				RogueSnapshot s = snap.get();
				RogueSession session = s.toSession();
				long mapId = s.relatedMapId();
				Optional<GameSnapshot> mapSnap = user.loadSave(mapId);
				if (mapSnap.isEmpty()) {
					// linked save missing; fall back to shop
					Router.instance().navigateTo(new RogueShopPage(session));
					return;
				}
				GameState gameState = GameState.fromSnapshot(mapSnap.get());
				for (ItemType type : ItemType.values()) {
					Integer count = s.gameItems().get(type.name());
					if (count != null) {
						gameState.gameStatus.items.set(type, count);
					}
				}
				Router.instance().navigateTo(
					new RoguePlayPage(session, gameState, mapId)
				);
			} else {
				RogueSession session = snap.isPresent()
					? snap.get().toSession()
					: new RogueSession();
				Router.instance().navigateTo(new RogueShopPage(session));
			}
		} else {
			user.loadSave(entry.id()).ifPresent(snapshot -> {
				GameState gameState = GameState.fromSnapshot(snapshot);
				UserSession.instance().setActiveSaveId(entry.id());
				long remaining = snapshot.status().remainingMs() > 0
					? snapshot.status().remainingMs()
					: LevelPage.DEFAULT_COUNTDOWN_MS;
				Router.instance().navigateTo(
					new LevelPage(gameState, remaining)
				);
			});
		}
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

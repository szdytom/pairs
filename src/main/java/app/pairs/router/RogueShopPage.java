package app.pairs.router;

import app.pairs.model.RogueSession;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class RogueShopPage implements Page {
	private final Blackboard blackboard;
	private final ShopComponent root;

	public RogueShopPage(RogueSession session) {
		this.blackboard = new Blackboard();
		this.root = new ShopComponent(session, () -> {
			session.level++;
			Router.instance().navigateTo(new RogueStagePage(session));
		});
		root.setBlackboard(blackboard);
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

package app.pairs.router;

import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class StatisticPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public StatisticPage() {
		this.blackboard = new Blackboard();
		this.root = new StatisticComponent(this::goBack);
		root.setBlackboard(blackboard);
	}

	private void goBack() {
		Router.instance().navigateTo(new UserPage());
	}

	@Override
	public void onEnter() {
		Router.instance().setTitle("Statistics");
	}

	@Override
	public void onExit() {}

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

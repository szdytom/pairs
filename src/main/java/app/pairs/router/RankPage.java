package app.pairs.router;

import app.pairs.audio.AudioManager;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class RankPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public RankPage() {
		this.blackboard = new Blackboard();
		this.root = new RankComponent(this::goBack);
		root.setBlackboard(blackboard);
	}

	private void goBack() {
		Router.instance().navigateTo(new MainMenuPage());
	}

	@Override
	public void onEnter() {
		AudioManager.instance().fadeOutMusic(1_000);
		AudioManager.instance().shufflePlayWithFadeIn("RecordMusic", 3000f);
	}

	@Override
	public void onExit() {
		AudioManager.instance().fadeOutMusic(1_000);
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

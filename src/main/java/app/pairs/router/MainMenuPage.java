package app.pairs.router;

import app.pairs.audio.AudioManager;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class MainMenuPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	@Override
	public void onEnter() {
		Router.instance().setTitle("Pairs - Main Menu");
		AudioManager.instance().fadeOutMusic();
		AudioManager.instance().shufflePlayWithFadeIn("MainMusic");
	}

	@Override
	public void onExit() {}

	public MainMenuPage() {
		this.blackboard = new Blackboard();
		this.root = new MainMenuComponent(
			this::startRogue, this::startGame, this::quitGame, this::goToLogin,
			this::goToUser, this::goToLoad, this::goToLeaderboard
		);
		root.setBlackboard(blackboard);
	}

	private void startGame() {
		Router.instance().navigateTo(new DifficultyPage());
	}

	private void quitGame() {
		Router.instance().quit();
	}

	private void goToLogin() {
		Router.instance().navigateTo(new LoginNavPage());
	}

	private void goToUser() {
		Router.instance().navigateTo(new UserPage());
	}

	private void goToLoad() {
		Router.instance().navigateTo(new LoadPage());
	}

	private void goToLeaderboard() {
		Router.instance().navigateTo(new RankPage());
	}

	private void startRogue() {
		Router.instance().navigateTo(
			new RogueStagePage(new app.pairs.model.RogueSession())
		);
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

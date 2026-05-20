package app.pairs.router;

import app.pairs.audio.AudioManager;
import app.pairs.user.NullUser;
import app.pairs.user.UserSession;
import app.pairs.view.Blackboard;
import app.pairs.view.UserPageComponent;

import io.github.libsdl4j.api.render.SDL_Renderer;

public class UserPage implements Page {
	private final UserPageComponent root;
	private final Blackboard blackboard;

	public UserPage() {
		this.blackboard = new Blackboard();
		this.root = new UserPageComponent(this::logOut, this::goBack);
		root.setBlackboard(blackboard);
	}

	private void logOut() {
		UserSession.instance().setUser(new NullUser());
		Router.instance().navigateTo(new MainMenuPage());
	}

	private void goBack() {
		Router.instance().navigateTo(new MainMenuPage());
	}

	@Override
	public void onEnter() {
		Router.instance().setTitle("Pairs - User");
		AudioManager.instance().fadeOutMusic();
		AudioManager.instance().shufflePlayWithFadeIn("UserMusic");
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
	public boolean onEvent(app.pairs.view.Event event) {
		return root.dispatchEvent(event, 0, 0);
	}

	@Override
	public void destroy() {
		root.destroy();
	}

	@Override
	public app.pairs.view.Widget getRoot() {
		return root;
	}

	@Override
	public Blackboard getBlackboard() {
		return blackboard;
	}
}

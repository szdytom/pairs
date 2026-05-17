package app.pairs.router;

import app.pairs.view.Blackboard;
import app.pairs.view.LoginNavComponent;

import io.github.libsdl4j.api.render.SDL_Renderer;

public class LoginNavPage implements Page {
	private final LoginNavComponent root;
	private final Blackboard blackboard;

	public LoginNavPage() {
		this.blackboard = new Blackboard();
		this.root = new LoginNavComponent(this::goToLogin, this::goToRegister);
		root.setBlackboard(blackboard);
	}

	private void goToLogin() {
		Router.instance().navigateTo(new LoginFormPage());
	}

	private void goToRegister() {
		Router.instance().navigateTo(new RegisterFormPage());
	}

	@Override
	public void onEnter() {}

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

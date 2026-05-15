package app.pairs.router;

import app.pairs.user.UserManager;
import app.pairs.user.UserSession;
import app.pairs.view.Blackboard;
import app.pairs.view.LoginComponent;

public class LoginPage implements Page {
	private final LoginComponent root;
	private final Blackboard blackboard;
	public LoginPage() {
		this.blackboard = new Blackboard();
		this.root = new LoginComponent(this::login);
		root.setBlackboard(blackboard);
	}

	private void login() {
		String username = root.getUsername();
		String password = root.getPassword();
		var result = UserManager.loginOrRegister(username, password);
		if (result.isEmpty()) {
			root.showError("Wrong password");
			return;
		}
		UserSession.instance().setUser(result.get());
		Router.instance().navigateTo(new DifficultyPage());
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
	public void render(
		io.github.libsdl4j.api.render.SDL_Renderer renderer, int scale
	) {
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
	public app.pairs.view.Blackboard getBlackboard() {
		return blackboard;
	}
}

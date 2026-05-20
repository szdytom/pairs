package app.pairs.router;

import app.pairs.user.UserManager;
import app.pairs.user.UserSession;
import app.pairs.view.AuthFormComponent;
import app.pairs.view.Blackboard;

import io.github.libsdl4j.api.render.SDL_Renderer;

public class LoginFormPage implements Page {
	private final AuthFormComponent root;
	private final Blackboard blackboard;

	public LoginFormPage() {
		this.blackboard = new Blackboard();
		this.root = new AuthFormComponent(
			this::submit,
			() -> Router.instance().navigateTo(new LoginNavPage()), "Login"
		);
		root.setBlackboard(blackboard);
	}

	private void submit() {
		var user = UserManager.login(root.getUsername(), root.getPassword());
		if (user.isEmpty()) {
			root.showError("Wrong credentials");
			return;
		}
		UserSession.instance().setUser(user.get());
		Router.instance().navigateTo(new MainMenuPage());
	}

	@Override
	public void onEnter() {
		Router.instance().setTitle("Pairs - Login");
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

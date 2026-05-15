package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.user.UserSession;

public class MainMenuComponent extends AlignLayout {
	public MainMenuComponent(
		Runnable onStart, Runnable onLogin, Runnable onQuit, Runnable onLogout,
		Runnable onRogue
	) {
		var title = new TextComponent("Pairs", 5, rgb(30, 30, 30));
		var titleAlign = new AlignLayout();
		title.setProp("h-align", AlignLayout.HAlign.CENTER);
		titleAlign.addChild(title);

		var startBtn = makeButton("Start", onStart);
		var startGuestBtn = makeButton("Guest", onStart);
		var loginBtn = makeButton("Login", onLogin);
		var rogueBtn = makeButton("Rogue", onRogue);
		var quitBtn = makeButton("Quit", onQuit);
		var logoutBtn = makeButton("Logout", onLogout);

		loginBtn.setVisible(!UserSession.instance().isAuthorized());
		startGuestBtn.setVisible(!UserSession.instance().isAuthorized());
		logoutBtn.setVisible(UserSession.instance().isAuthorized());
		startBtn.setVisible(UserSession.instance().isAuthorized());

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(titleAlign);
		column.addChild(startGuestBtn);
		column.addChild(startBtn);
		column.addChild(loginBtn);
		column.addChild(logoutBtn);
		column.addChild(rogueBtn);
		column.addChild(quitBtn);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(column);
	}

	private static Button makeButton(String label, Runnable onClick) {
		var btn = new Button(
			onClick, rgba(200, 200, 200, 255), rgba(160, 160, 160, 255)
		);
		var align = new AlignLayout();
		var text = new TextComponent(label, 2, rgb(50, 50, 50));
		text.setProp("h-align", AlignLayout.HAlign.CENTER);
		text.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(text);
		btn.addChild(align);
		return btn;
	}
}

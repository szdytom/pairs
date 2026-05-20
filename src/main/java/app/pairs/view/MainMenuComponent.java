package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.user.UserSession;

public class MainMenuComponent extends AlignLayout {
	public MainMenuComponent(
		Runnable onStart, Runnable onPractise, Runnable onQuit,
		Runnable onLogin, Runnable onUser, Runnable onLoad,
		Runnable onLeaderboard
	) {
		var title = new TextComponent("Pairs", 5, rgb(30, 30, 30));
		var titleAlign = new AlignLayout();
		title.setProp("h-align", AlignLayout.HAlign.CENTER);
		titleAlign.addChild(title);

		var loginBtn = makeButton("Login", onLogin);
		loginBtn.setVisible(!UserSession.instance().isAuthorized());
		var userBtn = makeButton("User", onUser);
		userBtn.setVisible(UserSession.instance().isAuthorized());

		var leaderboardIcon = Button.fromIcon("cup", onLeaderboard);
		var quitIcon = Button.fromIcon("quit", onQuit);
		var cornerColumn = new FlexLayout(FlexLayout.Direction.ROW, 4);
		cornerColumn.addChild(leaderboardIcon);
		cornerColumn.addChild(quitIcon);
		cornerColumn.setProp("h-align", AlignLayout.HAlign.RIGHT);
		cornerColumn.setProp("v-align", AlignLayout.VAlign.BOTTOM);
		cornerColumn.setProp("h-padding", 4);
		cornerColumn.setProp("v-padding", 4);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(titleAlign);
		column.addChild(makeButton("Start", onStart));
		column.addChild(makeButton("Practise", onPractise));
		column.addChild(loginBtn);
		column.addChild(userBtn);
		column.addChild(makeButton("Load", onLoad));
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(column);
		addChild(cornerColumn);
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

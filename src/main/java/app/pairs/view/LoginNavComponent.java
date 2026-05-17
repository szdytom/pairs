package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.router.*;

public class LoginNavComponent extends AlignLayout {
	public LoginNavComponent(Runnable onLogin, Runnable onRegister) {
		var homeBtn = Button.fromIcon(
			"home", () -> Router.instance().navigateTo(new MainMenuPage())
		);
		homeBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		homeBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		homeBtn.setProp("h-padding", 4);
		homeBtn.setProp("v-padding", 4);
		addChild(homeBtn);

		var hello = new TextComponent("Hello", 2, rgb(30, 30, 30));
		var msg = new TextComponent(
			"Login to save your progress", 1, rgb(128, 128, 128)
		);
		var glue = new GlueWidget();
		glue.setProp("flex-grow", 1);
		var loginBtn = makeButton("Login", onLogin);
		var registerBtn = makeButton("Register", onRegister);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(hello);
		column.addChild(msg);
		column.addChild(glue);
		column.addChild(loginBtn);
		column.addChild(registerBtn);
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

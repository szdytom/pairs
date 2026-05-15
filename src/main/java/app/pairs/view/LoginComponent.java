package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.router.*;

public class LoginComponent extends AlignLayout {
	private final TextField usernameField;
	private final TextField passwordField;
	private final TextComponent errorLabel;

	public LoginComponent(Runnable onLogin) {
		var title = new TextComponent("Login", 2, rgb(30, 30, 30));
		var titleAlign = new AlignLayout();
		title.setProp("h-align", AlignLayout.HAlign.CENTER);
		title.setProp("v-align", AlignLayout.VAlign.TOP);
		titleAlign.addChild(title);
		var homeBtn = Button.fromIcon(
			"home", () -> Router.instance().navigateTo(new MainMenuPage())
		);
		homeBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		homeBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		titleAlign.addChild(homeBtn);

		var loginBtn = makeButton("Login/Register", onLogin);
		var air = new AlignLayout();
		air.setProp("flex-grow", 1);
		var row = new FlexLayout(FlexLayout.Direction.ROW, 0);
		row.addChild(air);
		row.addChild(loginBtn);

		var username = new TextComponent("Username", 1, rgb(30, 30, 30));

		var password = new TextComponent("Password", 1, rgb(30, 30, 30));

		TextField usernameField = new TextField(
			2, rgb(30, 30, 30), rgba(255, 255, 255, 240), rgb(60, 60, 60),
			rgba(200, 220, 255, 240)
		);
		usernameField.setMaxLength(20);
		this.usernameField = usernameField;

		TextField passwordField = new TextField(
			2, rgb(30, 30, 30), rgba(255, 255, 255, 240), rgb(60, 60, 60),
			rgba(200, 220, 255, 240)
		);
		passwordField.setMaxLength(20);
		this.passwordField = passwordField;

		this.errorLabel = new TextComponent("", 1, rgb(200, 40, 40));

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(titleAlign);
		column.addChild(username);
		column.addChild(usernameField);
		column.addChild(password);
		column.addChild(passwordField);
		column.addChild(errorLabel);
		column.addChild(row);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(column);
	}

	public String getUsername() {
		return usernameField.text();
	}

	public String getPassword() {
		return passwordField.text();
	}

	public void showError(String msg) {
		errorLabel.setText(msg);
		errorLabel.setVisible(true);
	}

	private static Button makeButton(String label, Runnable onClick) {
		var btn = new Button(
			onClick, rgba(200, 200, 200, 255), rgba(160, 160, 160, 255)
		);
		var align = new AlignLayout();
		var text = new TextComponent(label, 1, rgb(50, 50, 50));
		text.setProp("h-align", AlignLayout.HAlign.CENTER);
		text.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(text);
		btn.addChild(align);
		return btn;
	}
}

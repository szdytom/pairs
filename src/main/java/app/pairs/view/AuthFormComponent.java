package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.router.*;

public class AuthFormComponent extends AlignLayout {
	private final TextField usernameField;
	private final TextField passwordField;
	private final TextComponent errorLabel;

	public AuthFormComponent(
		Runnable onSubmit, Runnable onBack, String submitLabel
	) {
		var homeBtn = Button.fromIcon(
			"home", () -> Router.instance().navigateTo(new MainMenuPage())
		);
		homeBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		homeBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		homeBtn.setProp("h-padding", 4);
		homeBtn.setProp("v-padding", 4);
		addChild(homeBtn);

		usernameField = new TextField(
			1, rgb(30, 30, 30), rgba(255, 255, 255, 240), rgb(60, 60, 60),
			rgba(200, 220, 255, 240)
		);
		usernameField.setMaxLength(20);

		passwordField = new TextField(
			1, rgb(30, 30, 30), rgba(255, 255, 255, 240), rgb(60, 60, 60),
			rgba(200, 220, 255, 240)
		);
		passwordField.setMaxLength(20);

		errorLabel = new TextComponent("", 1, rgb(200, 40, 40));

		var submitBtn = makeButton(submitLabel, onSubmit);
		var backBtn = makeButton("Back", onBack);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(new TextComponent("Username", 1, rgb(30, 30, 30)));
		column.addChild(usernameField);
		column.addChild(new TextComponent("Password", 1, rgb(30, 30, 30)));
		column.addChild(passwordField);
		column.addChild(errorLabel);
		column.addChild(submitBtn);
		column.addChild(backBtn);
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

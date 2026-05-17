package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.router.*;

public class AuthFormComponent extends AlignLayout {
	private static final int MAX_LENGTH = 30;

	private final TextField usernameField;
	private final TextField passwordField;
	private final TextField confirmPasswordField;
	private final TextComponent errorLabel;

	public AuthFormComponent(
		Runnable onSubmit, Runnable onBack, String submitLabel
	) {
		this(onSubmit, onBack, submitLabel, false);
	}

	public AuthFormComponent(
		Runnable onSubmit, Runnable onBack, String submitLabel,
		boolean hasConfirm
	) {
		var homeBtn = Button.fromIcon(
			"home", () -> Router.instance().navigateTo(new MainMenuPage())
		);
		homeBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		homeBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		homeBtn.setProp("h-padding", 4);
		homeBtn.setProp("v-padding", 4);
		addChild(homeBtn);

		int grayBg = rgb(230, 230, 230);
		usernameField = new TextField(
			1, rgb(30, 30, 30), grayBg, rgb(60, 60, 60),
			rgba(200, 220, 255, 240)
		);
		usernameField.setMaxLength(MAX_LENGTH);

		passwordField = new TextField(
			1, rgb(30, 30, 30), grayBg, rgb(60, 60, 60),
			rgba(200, 220, 255, 240),
			TextField.InputType.PASSWORD
		);
		passwordField.setMaxLength(MAX_LENGTH);

		confirmPasswordField = new TextField(
			1, rgb(30, 30, 30), grayBg, rgb(60, 60, 60),
			rgba(200, 220, 255, 240),
			TextField.InputType.PASSWORD
		);
		confirmPasswordField.setMaxLength(MAX_LENGTH);

		errorLabel = new TextComponent("", 1, rgb(200, 40, 40));

		var submitBtn = makeButton(submitLabel, onSubmit);
		var backBtn = makeButton("Back", onBack);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(new TextComponent("Username", 1, rgb(30, 30, 30)));
		column.addChild(usernameField);
		column.addChild(new TextComponent("Password", 1, rgb(30, 30, 30)));
		column.addChild(passwordField);
		if (hasConfirm) {
			column.addChild(
				new TextComponent("Confirm Password", 1, rgb(30, 30, 30))
			);
			column.addChild(confirmPasswordField);
		}
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

	public String getConfirmPassword() {
		return confirmPasswordField.text();
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

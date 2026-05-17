package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.render.SdlRender.SDL_DestroyTexture;

import app.pairs.user.UserSession;

import io.github.libsdl4j.api.render.*;

public class UserPageComponent extends AlignLayout {
	private final SDL_Texture avatar;

	public UserPageComponent(Runnable onLogout, Runnable onBack) {
		String username = UserSession.instance().getUser().getUsername();
		avatar = Identicon.create(username);

		var avatarImg = new ImageComponent(
			avatar, Identicon.TEX_SIZE, Identicon.TEX_SIZE
		);
		avatarImg.setProp("v-align", AlignLayout.VAlign.CENTER);

		var nameText = new TextComponent(username, 2, rgb(30, 30, 30));
		var textAlign = new AlignLayout();
		nameText.setProp("v-align", AlignLayout.VAlign.CENTER);
		textAlign.addChild(nameText);

		var infoRow = new FlexLayout(FlexLayout.Direction.ROW, 8);
		infoRow.addChild(avatarImg);
		infoRow.addChild(textAlign);

		var infoWrapper = new AlignLayout();
		infoWrapper.addChild(new GlueWidget(180, 0));
		infoRow.setProp("h-align", AlignLayout.HAlign.CENTER);
		infoWrapper.addChild(infoRow);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(infoWrapper);
		column.addChild(makeButton("Logout", onLogout));
		column.addChild(makeButton("Back", onBack));
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(column);
	}

	@Override
	public void destroy() {
		if (avatar != null) {
			SDL_DestroyTexture(avatar);
		}
		super.destroy();
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

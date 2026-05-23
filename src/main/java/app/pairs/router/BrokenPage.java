package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class BrokenPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public BrokenPage() {
		this.blackboard = new Blackboard();

		var msg = new TextComponent("SAVE DATA CORRUPTED", 3, rgb(160, 60, 60));
		msg.setProp("h-align", AlignLayout.HAlign.CENTER);
		msg.setProp("v-align", AlignLayout.VAlign.TOP);

		var face = new TextComponent(":-(", 4, rgb(160, 60, 60));
		face.setProp("h-align", AlignLayout.HAlign.CENTER);
		face.setProp("v-align", AlignLayout.VAlign.CENTER);

		var backBtn = new Button(
			()
				-> Router.instance().navigateTo(new MainMenuPage()),
			rgba(200, 200, 200, 255), rgba(160, 160, 160, 255)
		);
		var backLabel = new TextComponent("Back", 2, rgb(50, 50, 50));
		backLabel.setProp("h-align", AlignLayout.HAlign.CENTER);
		backLabel.setProp("v-align", AlignLayout.VAlign.CENTER);
		backBtn.addChild(backLabel);
		backBtn.setProp("h-align", AlignLayout.HAlign.CENTER);
		backBtn.setProp("v-align", AlignLayout.VAlign.BOTTOM);
		backBtn.setProp("v-padding", 2);

		var layout = new AlignLayout();
		layout.addChild(msg);
		layout.addChild(face);
		layout.addChild(backBtn);
		root = layout;
		root.setBlackboard(blackboard);
	}

	@Override
	public void onEnter() {
		Router.instance().setTitle("Pairs - Save Data Corrupted");
	}

	@Override
	public void update(long deltaTimeMs) {
		root.update(deltaTimeMs);
	}

	@Override
	public void render(SDL_Renderer renderer, int scale) {
		root.render(renderer, 0, 0, scale);
	}

	@Override
	public boolean onEvent(Event event) {
		return root.dispatchEvent(event, 0, 0);
	}

	@Override
	public void destroy() {
		root.destroy();
	}

	@Override
	public Widget getRoot() {
		return root;
	}

	@Override
	public Blackboard getBlackboard() {
		return blackboard;
	}
}

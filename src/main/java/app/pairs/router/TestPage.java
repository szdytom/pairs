package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class TestPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public TestPage() {
		this.blackboard = new Blackboard();

		var input = new TextField(
			3, rgb(30, 30, 30), rgba(255, 255, 255, 240), rgb(60, 60, 60),
			rgba(200, 220, 255, 240)
		);
		input.setMaxLength(20);
		input.setProp("h-align", AlignLayout.HAlign.CENTER);

		var label = new TextComponent(
			"Component Test — type below:", 2, rgb(80, 80, 80)
		);
		label.setProp("h-align", AlignLayout.HAlign.CENTER);

		var quitBtn = new Button(
			()
				-> Router.instance().quit(),
			rgba(200, 200, 200, 255), rgba(160, 160, 160, 255)
		);
		var btnLabel = new TextComponent("Quit", 2, rgb(50, 50, 50));
		btnLabel.setProp("h-align", AlignLayout.HAlign.CENTER);
		btnLabel.setProp("v-align", AlignLayout.VAlign.CENTER);
		quitBtn.addChild(btnLabel);

		var output = new TextComponent("", 2, rgb(100, 100, 100));
		output.setProp("h-align", AlignLayout.HAlign.CENTER);
		output.setProp("h-padding", 20);

		input.setProp("h-padding", 20);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 12);
		column.addChild(label);
		column.addChild(input);
		column.addChild(quitBtn);
		column.addChild(output);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);

		root = column;
		root.setBlackboard(blackboard);
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

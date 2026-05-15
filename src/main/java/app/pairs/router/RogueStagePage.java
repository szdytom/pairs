package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.model.RogueSession;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class RogueStagePage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public RogueStagePage(RogueSession session) {
		this.blackboard = new Blackboard();

		var title = new TextComponent(
			"Round " + session.level, 3, rgb(30, 30, 30)
		);
		title.setProp("h-align", AlignLayout.HAlign.CENTER);

		var startBtn = makeButton(
			"Start",
			() -> Router.instance().navigateTo(new RoguePlayPage(session))
		);
		var quitBtn = makeButton(
			"Quit", () -> Router.instance().navigateTo(new MainMenuPage())
		);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 12);
		column.addChild(title);
		column.addChild(startBtn);
		column.addChild(quitBtn);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);

		var rootAlign = new AlignLayout();
		rootAlign.addChild(column);
		rootAlign.setBlackboard(blackboard);
		this.root = rootAlign;
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

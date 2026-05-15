package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.model.RogueSession;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class RogueResultPage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public RogueResultPage(RogueSession session) {
		this.blackboard = new Blackboard();

		long timeUsed = RogueSession.TOTAL_TIME_MS - session.remainingMs;
		long mins = timeUsed / 60_000;
		long secs = (timeUsed % 60_000) / 1_000;

		var gameOverText = new TextComponent("Game Over", 4, rgb(40, 40, 40));
		gameOverText.setProp("h-align", AlignLayout.HAlign.CENTER);

		var scoreText = new TextComponent(
			"Score: " + session.getTotalEarned(), 2, rgb(60, 60, 60)
		);
		scoreText.setProp("h-align", AlignLayout.HAlign.CENTER);

		var roundText = new TextComponent(
			"Round: " + (session.level - 1), 2, rgb(60, 60, 60)
		);
		roundText.setProp("h-align", AlignLayout.HAlign.CENTER);

		var timeText = new TextComponent(
			String.format("Time: %02d:%02d", mins, secs), 2, rgb(60, 60, 60)
		);
		timeText.setProp("h-align", AlignLayout.HAlign.CENTER);

		var menuBtn = makeButton(
			"Main Menu", () -> Router.instance().navigateTo(new MainMenuPage())
		);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
		column.addChild(gameOverText);
		column.addChild(scoreText);
		column.addChild(roundText);
		column.addChild(timeText);
		column.addChild(menuBtn);
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

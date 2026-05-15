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

		long totalAvailable = RogueSession.TOTAL_TIME_MS
			+ session.totalTimePurchasedMs;
		long timeUsed = totalAvailable - session.remainingMs;
		long mins = timeUsed / 60_000;
		long secs = (timeUsed % 60_000) / 1_000;

		var gameOverText = new TextComponent("Game Over", 4, rgb(40, 40, 40));
		gameOverText.setProp("h-align", AlignLayout.HAlign.CENTER);

		int labelColor = rgb(120, 120, 120);
		int valueColor = rgb(50, 50, 50);

		var grid = new GridLayout(2, 8, 0, 2);
		addRow(grid, "Round", String.valueOf(session.level - 1), labelColor, valueColor);
		addRow(grid, "Time", String.format("%02d:%02d", mins, secs), labelColor, valueColor);
		addRow(grid, "Score", String.valueOf(session.getTotalEarned()), labelColor, valueColor);

		var menuBtn = makeButton(
			"Main Menu", () -> Router.instance().navigateTo(new MainMenuPage())
		);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 12);
		column.addChild(gameOverText);
		column.addChild(grid);
		column.addChild(menuBtn);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);

		var rootAlign = new AlignLayout();
		rootAlign.addChild(column);
		rootAlign.setBlackboard(blackboard);
		this.root = rootAlign;
	}

	private static void addRow(
		GridLayout grid, String label, String value,
		int labelColor, int valueColor
	) {
		grid.addChild(new TextComponent(label, 2, labelColor));
		grid.addChild(new TextComponent(value, 2, valueColor));
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

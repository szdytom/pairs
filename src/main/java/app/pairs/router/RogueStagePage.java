package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.map.DifficultyParams;
import app.pairs.map.RogueDifficultyGenerator;
import app.pairs.model.RogueSession;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class RogueStagePage implements Page {
	private final Blackboard blackboard;
	private final Widget root;

	public RogueStagePage(RogueSession session) {
		this.blackboard = new Blackboard();
		DifficultyParams params = RogueDifficultyGenerator.preview(
			session.level
		);

		var title = new TextComponent(
			"Round " + session.level, 3, rgb(30, 30, 30)
		);
		title.setProp("h-align", AlignLayout.HAlign.CENTER);

		var grid = new GridLayout(2, 8, 0, 2);
		int labelColor = rgb(120, 120, 120);
		int valueColor = rgb(50, 50, 50);
		addRow(grid, "Grid", params.sizeLabel(), labelColor, valueColor);
		addRow(
			grid, "Types", String.valueOf(params.types()), labelColor,
			valueColor
		);
		addRow(grid, "Spread", params.spreadLabel(), labelColor, valueColor);
		addRow(grid, "Pairing", params.strategyLabel(), labelColor, valueColor);
		addRow(
			grid, "Points",
			params.totalPoints() + " (" + params.tierLabel() + ")", labelColor,
			valueColor
		);

		var quitBtn = makeButton(
			"Quit", () -> Router.instance().navigateTo(new MainMenuPage())
		);
		var startBtn = makeButton(
			"Let's rock\u2192",
			()
				-> Router.instance().navigateTo(
					new RoguePlayPage(session, params)
				)
		);
		startBtn.setProp("flex-grow", 1);
		var buttonRow = new FlexLayout(FlexLayout.Direction.ROW, 8);
		buttonRow.addChild(quitBtn);
		buttonRow.addChild(startBtn);
		buttonRow.setProp("h-align", AlignLayout.HAlign.CENTER);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 10);
		column.addChild(title);
		column.addChild(grid);
		column.addChild(buttonRow);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);

		var rootAlign = new AlignLayout();
		rootAlign.addChild(column);
		rootAlign.setBlackboard(blackboard);
		this.root = rootAlign;
	}

	private static void addRow(
		GridLayout grid, String label, String value, int labelColor,
		int valueColor
	) {
		grid.addChild(new TextComponent(label, 1, labelColor));
		grid.addChild(new TextComponent(value, 1, valueColor));
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

package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.logic.GameState;
import app.pairs.map.RogueDifficultyGenerator;
import app.pairs.map.TileSelectionPolicy;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class CustomDifficultyPage implements Page {
	private static final int MIN_SIZE = 6;
	private static final int MAX_SIZE = 14;
	private static final int MIN_TYPES = 3;
	private static final int MAX_TYPES = 20;
	private static final String[] ON_OFF = {"Off", "On"};
	private static final String[] TRI_TACTIC_LABELS = {
		"Kind", "Neutral", "Mean"
	};
	private static final TileSelectionPolicy.Spread[] SPREADS = {
		TileSelectionPolicy.Spread.NO_DUPLICATES,
		TileSelectionPolicy.Spread.FREE,
		TileSelectionPolicy.Spread.PREFER_DUPLICATES
	};

	private final Blackboard blackboard;
	private final Widget root;
	private final CarouselSelector widthSel;
	private final CarouselSelector heightSel;
	private final CarouselSelector typesSel;
	private final CarouselSelector slabsSel;
	private final CarouselSelector spreadSel;
	private final CarouselSelector pairingSel;

	public CustomDifficultyPage() {
		this.blackboard = new Blackboard();
		int labelColor = rgb(120, 120, 120);

		widthSel = makeNumericSelector(MIN_SIZE, MAX_SIZE);
		heightSel = makeNumericSelector(MIN_SIZE, MAX_SIZE);
		typesSel = makeNumericSelector(MIN_TYPES, MAX_TYPES);
		slabsSel = makeOptionsSelector(ON_OFF);
		spreadSel = makeOptionsSelector(TRI_TACTIC_LABELS);
		pairingSel = makeOptionsSelector(TRI_TACTIC_LABELS);

		var title = new TextComponent("Customize Level", 2, rgb(30, 30, 30));
		title.setProp("h-align", AlignLayout.HAlign.CENTER);

		var grid = new GridLayout(2, 8, 4, 2);
		addRow(grid, "Width", widthSel, labelColor);
		addRow(grid, "Height", heightSel, labelColor);
		addRow(grid, "Types", typesSel, labelColor);
		addRow(grid, "Slabs", slabsSel, labelColor);
		addRow(grid, "Spread", spreadSel, labelColor);
		addRow(grid, "Pairing", pairingSel, labelColor);

		var backBtn = makeButton("Back", this::goBack);
		var startBtn = makeButton("Start\u2192", this::startGame);
		startBtn.setProp("flex-grow", 1);
		var buttonRow = new FlexLayout(FlexLayout.Direction.ROW, 8);
		buttonRow.addChild(backBtn);
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

	private static CarouselSelector makeNumericSelector(int min, int max) {
		int count = max - min + 1;
		int maxW = 0;
		for (int i = 0; i < count; i++) {
			var t = new TextComponent(
				String.valueOf(min + i), 1, rgb(50, 50, 50)
			);
			int[] sz = t.measure();
			maxW = Math.max(maxW, sz[0]);
		}
		return new CarouselSelector(
			count,
			i
			-> {
				var t = new TextComponent(
					String.valueOf(min + i), 1, rgb(50, 50, 50)
				);
				t.setProp("h-align", AlignLayout.HAlign.CENTER);
				t.setProp("v-align", AlignLayout.VAlign.CENTER);
				return t;
			},
			null, maxW + 40, false, CarouselSelector.arrow("-", 1),
			CarouselSelector.arrow("+", 1)
		);
	}

	private static CarouselSelector makeOptionsSelector(String[] labels) {
		int maxW = 0;
		for (String l : labels) {
			var t = new TextComponent(l, 1, rgb(50, 50, 50));
			int[] sz = t.measure();
			maxW = Math.max(maxW, sz[0]);
		}
		return new CarouselSelector(
			labels.length,
			i
			-> {
				var t = new TextComponent(labels[i], 1, rgb(50, 50, 50));
				t.setProp("h-align", AlignLayout.HAlign.CENTER);
				t.setProp("v-align", AlignLayout.VAlign.CENTER);
				return t;
			},
			null, maxW + 40, false, CarouselSelector.arrow("<", 1),
			CarouselSelector.arrow(">", 1)
		);
	}

	private static void addRow(
		GridLayout grid, String label, CarouselSelector sel, int labelColor
	) {
		grid.addChild(new TextComponent(label, 1, labelColor));
		grid.addChild(sel);
	}

	private void startGame() {
		int w = MIN_SIZE + widthSel.getIndex();
		int h = MIN_SIZE + heightSel.getIndex();
		int types = MIN_TYPES + typesSel.getIndex();
		boolean slabs = slabsSel.getIndex() == 1;
		TileSelectionPolicy.Spread spread = SPREADS[spreadSel.getIndex()];
		int strategyIdx = pairingSel.getIndex();
		Router.instance().navigateTo(new LevelPage(
			RogueDifficultyGenerator.generateCustom(
				w, h, types, slabs, spread, strategyIdx
			),
			180_000L
		));
	}

	private void goBack() {
		Router.instance().navigateTo(new DifficultyPage());
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

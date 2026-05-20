package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.map.CustomGameBuilder;
import app.pairs.map.TileSelectionPolicy;
import app.pairs.user.UserSession;
import app.pairs.view.*;

import java.util.function.Consumer;

import io.github.libsdl4j.api.render.*;

public class CustomDifficultyPage implements Page {
	private static final int MIN_SIZE = 4;
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
	private final TextComponent pointsLabel;
	private final TextComponent pointsValue;
	private final TextComponent errorText;

	public CustomDifficultyPage() {
		this.blackboard = new Blackboard();
		int labelColor = rgb(120, 120, 120);

		Consumer<Integer> onUpdate = i -> refreshPoints();

		widthSel = makeNumericSelector(MIN_SIZE, MAX_SIZE, onUpdate);
		heightSel = makeNumericSelector(MIN_SIZE, MAX_SIZE, onUpdate);
		typesSel = makeNumericSelector(MIN_TYPES, MAX_TYPES, onUpdate);
		slabsSel = makeOptionsSelector(ON_OFF, onUpdate);
		spreadSel = makeOptionsSelector(TRI_TACTIC_LABELS, onUpdate);
		pairingSel = makeOptionsSelector(TRI_TACTIC_LABELS, onUpdate);

		var title = new TextComponent("Customize Level", 2, rgb(30, 30, 30));
		title.setProp("h-align", AlignLayout.HAlign.CENTER);

		pointsLabel = new TextComponent("Points", 1, labelColor);
		pointsValue = new TextComponent("", 1, rgb(50, 50, 50));
		errorText = new TextComponent("", 1, rgb(200, 50, 50));
		errorText.setVisible(false);
		errorText.setProp("h-align", AlignLayout.HAlign.CENTER);

		var grid = new GridLayout(2, 8, 4, 2);
		addRow(grid, "Width", widthSel, labelColor);
		addRow(grid, "Height", heightSel, labelColor);
		addRow(grid, "Types", typesSel, labelColor);
		addRow(grid, "Slabs", slabsSel, labelColor);
		addRow(grid, "Spread", spreadSel, labelColor);
		addRow(grid, "Pairing", pairingSel, labelColor);
		grid.addChild(pointsLabel);
		grid.addChild(pointsValue);

		var backBtn = makeButton("Back", this::goBack);
		var startBtn = makeButton("Start\u2192", this::startGame);
		startBtn.setProp("flex-grow", 1);
		var buttonRow = new FlexLayout(FlexLayout.Direction.ROW, 8);
		buttonRow.addChild(backBtn);
		buttonRow.addChild(startBtn);
		buttonRow.setProp("h-align", AlignLayout.HAlign.CENTER);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		column.addChild(title);
		column.addChild(new GlueWidget(0, 10));
		column.addChild(grid);
		column.addChild(errorText);
		column.addChild(buttonRow);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);

		var rootAlign = new AlignLayout();
		rootAlign.addChild(column);
		rootAlign.setBlackboard(blackboard);
		this.root = rootAlign;

		refreshPoints();
	}

	private void refreshPoints() {
		int w = MIN_SIZE + widthSel.getIndex();
		int h = MIN_SIZE + heightSel.getIndex();
		int types = MIN_TYPES + typesSel.getIndex();
		boolean slabs = slabsSel.getIndex() == 1;
		TileSelectionPolicy.Spread spread = SPREADS[spreadSel.getIndex()];
		int strategyIdx = pairingSel.getIndex();

		String err = CustomGameBuilder.validationError(w, h, types);
		if (err != null) {
			pointsLabel.setVisible(false);
			pointsValue.setVisible(false);
			errorText.setVisible(true);
			errorText.setText("Error: " + err);
		} else {
			pointsLabel.setVisible(true);
			pointsValue.setVisible(true);
			errorText.setVisible(false);
			int pts = CustomGameBuilder.computePoints(
				w, h, types, slabs, spread, strategyIdx
			);
			pointsValue.setText(
				pts + " (" + CustomGameBuilder.tierForPoints(pts).name() + ")"
			);
		}
	}

	private static CarouselSelector makeNumericSelector(
		int min, int max, Consumer<Integer> onChange
	) {
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
			onChange, maxW + 40, false, CarouselSelector.arrow("-", 1),
			CarouselSelector.arrow("+", 1)
		);
	}

	private static CarouselSelector makeOptionsSelector(
		String[] labels, Consumer<Integer> onChange
	) {
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
			onChange, maxW + 40, false, CarouselSelector.arrow("<", 1),
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
		if (CustomGameBuilder.validationError(w, h, types) != null)
			return;
		boolean slabs = slabsSel.getIndex() == 1;
		TileSelectionPolicy.Spread spread = SPREADS[spreadSel.getIndex()];
		int strategyIdx = pairingSel.getIndex();
		UserSession.instance().setActiveSaveId(null);
		Router.instance().navigateTo(new LevelPage(
			CustomGameBuilder.build(w, h, types, slabs, spread, strategyIdx),
			LevelPage.DEFAULT_COUNTDOWN_MS
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
	public void onEnter() {
		Router.instance().setTitle("Pairs - Custom Difficulty");
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

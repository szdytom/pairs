package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.logic.GameState.OpKind;
import app.pairs.model.CountdownState;
import app.pairs.model.ItemType;
import app.pairs.model.RogueSession;
import app.pairs.model.Tilemap;
import app.pairs.router.MainMenuPage;
import app.pairs.router.Router;
import app.pairs.solver.Move;
import app.pairs.solver.SolverResult;

import io.github.libsdl4j.api.render.*;

/**
 * Top-level component that owns a {@link GameState} and an
 * {@link IsometricGridView}. Bridges the int-based tile IDs from the model to
 * the view, resolves mouse-over cells, and drives the selection/elimination
 * interaction.
 */
public class LevelComponent extends Container {
	private static final long HINT_COOLDOWN_MS = 5_000;
	private static final long ENTRY_STAGGER_MS = 20;

	private GameState gameState;
	private final IsometricGridView gridView;
	private int gridWidth;
	private int gridHeight;
	private float[][] highlighted;

	private int mouseX = -1;
	private int mouseY = -1;
	private int hoveredRow = -1;
	private int hoveredCol = -1;
	private int prevHoveredRow = -1;
	private int prevHoveredCol = -1;
	private final int[] hitResult = new int[2];

	private int selectedRow = -1;
	private int selectedCol = -1;

	private boolean cleared;
	private boolean timedOut;
	private int totalPairs;
	private boolean tntSelecting;

	private long totalCountdownMs;
	private RogueSession rogueSession;
	private long lastEliminationTimeMs;

	// Layout state
	private final AlignLayout alignLayout;
	private final LevelSidebar sidebar;
	private final int[] measuredSize = new int[2];
	private int gridOriginX;
	private int gridOriginY;

	// Child text components
	private final TextComponent overlayText;
	private final PairCounter pairCounter;
	private final Button hintBtn;
	private final Button undoBtn;
	private final Button retryBtn;

	private final AutoPairingState autoPairingState = new AutoPairingState();
	private ItemListComponent itemList;
	private int autoHLRow1 = -1;
	private int autoHLCol1 = -1;
	private int autoHLRow2 = -1;
	private int autoHLCol2 = -1;
	private boolean gameStarted;

	public LevelComponent(
		GameState gameState, long totalCountdownMs, Blackboard blackboard
	) {
		this(gameState, totalCountdownMs, totalCountdownMs, blackboard);
	}

	public LevelComponent(
		GameState gameState, long totalCountdownMs, long initialRemainingMs,
		Blackboard blackboard
	) {
		this.gameState = gameState;
		this.gridWidth = gameState.getWidth();
		this.gridHeight = gameState.getHeight();
		this.highlighted = new float[gridHeight][gridWidth];

		int tileCount = 0;
		for (int r = 0; r < gridHeight; r++) {
			for (int c = 0; c < gridWidth; c++)
				if (gameState.getTile(r, c) > 0)
					tileCount++;
		}
		int totalPairs = tileCount / 2;

		this.totalPairs = totalPairs;
		this.totalCountdownMs = totalCountdownMs;
		gameState.gameStatus.countdown.remainingMs = initialRemainingMs;
		this.lastEliminationTimeMs = gameState.gameStatus.countdown.now();

		blackboard.put(GameState.class, gameState);
		blackboard.put(CountdownState.class, gameState.gameStatus.countdown);
		setBlackboard(blackboard);

		this.gridView = new IsometricGridView(gridWidth, gridHeight);

		this.hintBtn = Button.fromIcon("hint", () -> {
			if (timedOut || cleared || autoPairingState.isActive()) {
				return;
			}
			SolverResult result = gameState.solve(50);
			if (result.moves().isEmpty()) {
				return;
			}
			Move m = result.moves().get(0);
			startHint(m.r1(), m.c1(), m.r2(), m.c2());
		});
		hintBtn.setVisible(false);

		this.retryBtn = Button.fromIcon("restart", () -> restart());

		var topBar = new FlexLayout(FlexLayout.Direction.ROW, 4);
		topBar.setProp("h-align", AlignLayout.HAlign.RIGHT);
		topBar.setProp("v-align", AlignLayout.VAlign.TOP);
		topBar.setProp("h-padding", 4);
		topBar.setProp("v-padding", 4);
		topBar.addChild(hintBtn);

		var homeBtn = Button.fromIcon(
			"home", () -> Router.instance().navigateTo(new MainMenuPage())
		);
		homeBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		homeBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		homeBtn.setProp("h-padding", 4);
		homeBtn.setProp("v-padding", 4);

		this.pairCounter = new PairCounter(totalPairs);
		pairCounter.setProp("h-align", AlignLayout.HAlign.CENTER);
		pairCounter.setProp("v-align", AlignLayout.VAlign.TOP);
		pairCounter.setProp("v-padding", 0);

		this.alignLayout = new AlignLayout();
		alignLayout.addChild(topBar);
		alignLayout.addChild(homeBtn);
		alignLayout.addChild(pairCounter);
		gridView.setProp("h-align", AlignLayout.HAlign.CENTER);
		gridView.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(gridView);

		this.overlayText = new TextComponent("Cleared!", 2, rgb(40, 40, 40));
		overlayText.setVisible(false);
		overlayText.setProp("h-align", AlignLayout.HAlign.CENTER);
		overlayText.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(overlayText);

		this.itemList = new ItemListComponent(
			gameState.gameStatus.getCount(ItemType.AUTO_SOLVER),
			gameState.gameStatus.getCount(ItemType.TNT), type -> {
				if (timedOut || cleared || autoPairingState.isActive()) {
					return;
				}

				switch (type) {
				case AUTO_SOLVER -> {
					if (!gameState.gameStatus.reduceItem(type)) {
						return;
					}
					SolverResult result = gameState.solve(1_000);
					for (Move m : result.moves()) {
						startHint(m.r1(), m.c1(), m.r2(), m.c2());
					}
				}
				case TNT -> {
					if (!gameState.gameStatus.reduceItem(type)) {
						return;
					}
					selectedRow = -1;
					selectedCol = -1;
					tntSelecting = true;
					itemList.showAbort();
				}
				}
			}
		);
		itemList.setVisible(!gameState.gameStatus.items.isEmpty());
		itemList.setProp("v-align", AlignLayout.VAlign.BOTTOM);
		itemList.setProp("h-align", AlignLayout.HAlign.CENTER);
		itemList.setProp("v-padding", 10);
		alignLayout.addChild(itemList);

		this.sidebar = new LevelSidebar();

		this.undoBtn = Button.fromIcon("revert", () -> {
			if (timedOut || cleared || autoPairingState.isActive()) {
				return;
			}
			gameState.undo();
			gridView.reset();
			sidebar.notifyStateUpdated();
		});
		undoBtn.setVisible(false);
		topBar.addChild(undoBtn);
		topBar.addChild(retryBtn);

		addChild(alignLayout);
		addChild(sidebar);
		startEntryAnimation();
	}

	public LevelComponent(
		GameState gameState, RogueSession session, Blackboard blackboard
	) {
		this(gameState, session, session.remainingMs, blackboard);
	}

	public LevelComponent(
		GameState gameState, RogueSession session, long initialRemainingMs,
		Blackboard blackboard
	) {
		this(gameState, session.remainingMs, initialRemainingMs, blackboard);
		this.rogueSession = session;
	}

	@Override
	public void update(long deltaTimeMs) {
		if (!cleared && !timedOut && gameStarted) {
			gameState.gameStatus.countdown.remainingMs = Math.max(
				0, gameState.gameStatus.countdown.remainingMs - deltaTimeMs
			);
			if (gameState.gameStatus.countdown.remainingMs == 0) {
				timedOut = true;
				autoPairingState.clear();
				gridView.setVisible(false);
			}
		}
		boolean hintReady = !cleared && !timedOut
			&& !autoPairingState.isActive()
			&& gameState.gameStatus.countdown.now() - lastEliminationTimeMs
				>= HINT_COOLDOWN_MS;
		hintBtn.setVisible(hintReady);
		undoBtn.setVisible(!cleared && !timedOut && gameState.canUndo());
		if (!cleared && !timedOut && gameState.isStall()) {
			pairCounter.showStall();
		} else if (
			!cleared && !timedOut
			&& gameState.gameStatus.countdown.now() - lastEliminationTimeMs
				< 1_000
			&& gameState.gameStatus.combo >= 2
		) {
			pairCounter.showCombo(gameState.gameStatus.combo);
		} else {
			pairCounter.setProgress(totalPairs - gameState.remainingPairs());
		}
		autoPairingState.update(deltaTimeMs);
		super.update(deltaTimeMs);
	}

	/** Reset the level with the map generated. */
	public void restart() {
		gameState.gameStatus.countdown.remainingMs = totalCountdownMs;
		gameState.gameStatus.countdown.resetPause();
		gameState.restart();
		lastEliminationTimeMs = gameState.gameStatus.countdown.now();
		tntSelecting = false;
		int newW = gameState.getWidth();
		int newH = gameState.getHeight();
		if (newW != gridWidth || newH != gridHeight) {
			gridWidth = newW;
			gridHeight = newH;
			highlighted = new float[gridHeight][gridWidth];
			gridView.setGridSize(newW, newH);
		} else {
			gridView.reset();
		}
		cleared = false;
		timedOut = false;
		gridView.setVisible(true);
		overlayText.setVisible(false);
		autoPairingState.clear();
		clearAutoHighlights();
		selectedRow = -1;
		selectedCol = -1;
		hoveredRow = -1;
		hoveredCol = -1;
		prevHoveredRow = -1;
		prevHoveredCol = -1;
		mouseX = -1;
		mouseY = -1;
		startEntryAnimation();
		sidebar.notifyStateUpdated();
	}

	private void startEntryAnimation() {
		gameStarted = false;
		gameState.gameStatus.countdown.pause();
		gridView.setEntryPlaying(true);
		int[][] order = gridView.getDepthOrder();
		for (int[] cell : order) {
			int r = cell[0], c = cell[1];
			if (gameState.getTile(r, c) > 0) {
				final int row = r;
				final int col = c;
				autoPairingState.push(
					new ActionStep(() -> gridView.startEntry(row, col))
				);
				autoPairingState.push(
					new AutoPairingState.WaitStep(ENTRY_STAGGER_MS)
				);
			}
		}
		autoPairingState.push(dt -> gridView.isEntryComplete());
		autoPairingState.push(new ActionStep(() -> {
			gridView.setEntryPlaying(false);
			gameState.gameStatus.countdown.resume();
			gameStarted = true;
		}));
	}

	public boolean isCleared() {
		return cleared;
	}

	public boolean isTimedOut() {
		return timedOut;
	}

	/** Handle a mouse click at the current cursor position. */
	public void handleClick() {
		if (timedOut || cleared || !gameStarted) {
			return;
		}

		if (tntSelecting) {
			handleTntClick();
			return;
		}

		updateHoveredCell();

		if (hoveredRow < 0 || hoveredCol < 0) {
			return;
		}
		if (gameState.getTile(hoveredRow, hoveredCol) <= 0) {
			return;
		}

		if (selectedRow < 0) {
			selectedRow = hoveredRow;
			selectedCol = hoveredCol;
		} else if (selectedRow == hoveredRow && selectedCol == hoveredCol) {
			selectedRow = -1;
			selectedCol = -1;
		} else {
			if (gameState.canEliminate(
					selectedRow, selectedCol, hoveredRow, hoveredCol
				)) {
				int r1 = selectedRow, c1 = selectedCol;
				int r2 = hoveredRow, c2 = hoveredCol;
				selectedRow = -1;
				selectedCol = -1;
				autoHLRow1 = r1;
				autoHLCol1 = c1;
				autoHLRow2 = r2;
				autoHLCol2 = c2;
				autoPairingState.push(
					dt -> gridView.isAnimationReady(r1, c1)
						&& gridView.isAnimationReady(r2, c2)
				);
				autoPairingState.push(new ActionStep(() -> {
					eliminatePair(r1, c1, r2, c2);
					clearAutoHighlights();
				}));
			} else {
				selectedRow = hoveredRow;
				selectedCol = hoveredCol;
			}
		}
	}

	@Override
	public int[] measure() {
		int[] gridSize = gridView.measure();
		measuredSize[0] = gridSize[0] + sidebar.measure()[0];
		measuredSize[1] = gridSize[1] + 40;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		int sidebarW = sidebar.measure()[0];
		// alignLayout fills the left area up to the sidebar.
		alignLayout.layout(0, 0, w - sidebarW, h);
		// sidebar is pinned to the right edge at its natural width.
		sidebar.layout(w - sidebarW, 0, sidebarW, h);
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int myGlobalX = parentX + layoutX;
		int myGlobalY = parentY + layoutY;

		gridOriginX = myGlobalX + gridView.layoutX;
		gridOriginY = myGlobalY + gridView.layoutY;

		if (!autoPairingState.isActive()) {
			updateHoveredCell();
		}
		updateHighlighted();

		if (timedOut) {
			overlayText.setText("Time Out!");
		} else if (cleared) {
			overlayText.setText("Cleared!");
		}
		overlayText.setVisible(timedOut || cleared);
		super.render(renderer, parentX, parentY, scale);
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
			if (autoPairingState.isActive()) {
				return false;
			}
			switch (me.type()) {
			case MOUSE_MOVED:
			case MOUSE_LEAVE:
				mouseX = me.x();
				mouseY = me.y();
				return true;
			case MOUSE_PRESSED:
				handleClick();
				return true;
			default:
				break;
			}
		}
		return false;
	}

	// ---- auto-pairing
	// --------------------------------------------------------

	private static class ActionStep implements AutoPairingStep {
		private final Runnable action;

		ActionStep(Runnable action) {
			this.action = action;
		}

		@Override
		public boolean update(long deltaTimeMs) {
			action.run();
			return true;
		}
	}

	public void pushAutoPairingStep(AutoPairingStep step) {
		autoPairingState.push(step);
	}

	private void startHint(int r1, int c1, int r2, int c2) {
		autoPairingState.push(new ActionStep(this::clearAutoHighlights));
		pushHighlightAndWait(r1, c1);
		pushHighlightAndWait(r2, c2);
		autoPairingState.push(new ActionStep(() -> {
			performAutoElimination(r1, c1, r2, c2);
			clearAutoHighlights();
		}));
	}

	private void pushHighlightAndWait(int row, int col) {
		autoPairingState.push(new ActionStep(() -> addAutoHighlight(row, col)));
		autoPairingState.push(dt -> gridView.isAnimationReady(row, col));
		autoPairingState.push(new AutoPairingState.WaitStep(200));
	}

	private void clearAutoHighlights() {
		autoHLRow1 = autoHLCol1 = autoHLRow2 = autoHLCol2 = -1;
	}

	private void addAutoHighlight(int row, int col) {
		if (autoHLRow1 < 0) {
			autoHLRow1 = row;
			autoHLCol1 = col;
		} else {
			autoHLRow2 = row;
			autoHLCol2 = col;
		}
	}

	private void eliminatePair(int r1, int c1, int r2, int c2) {
		if (timedOut || cleared)
			return;
		long now = gameState.gameStatus.countdown.now();
		int elapsed = (int)(now - lastEliminationTimeMs);
		AudioManager.instance().play(
			"eliminate", gameState.getTileString(r1, c1)
		);
		gameState.eliminate(r1, c1, r2, c2, elapsed, OpKind.MANUAL);
		lastEliminationTimeMs = now;

		gridView.reset();
		if (gameState.isCleared()) {
			cleared = true;
			overlayText.setVisible(true);
		}
		sidebar.notifyStateUpdated();
	}

	private void performAutoElimination(int r1, int c1, int r2, int c2) {
		if (timedOut || cleared)
			return;
		long now = gameState.gameStatus.countdown.now();
		int elapsed = (int)(now - lastEliminationTimeMs);
		AudioManager.instance().play(
			"eliminate", gameState.getTileString(r1, c1)
		);
		gameState.eliminate(r1, c1, r2, c2, elapsed, OpKind.AUTO);
		lastEliminationTimeMs = now;
		gridView.reset();
		pairCounter.setProgress(totalPairs - gameState.remainingPairs());
		if (gameState.isCleared()) {
			cleared = true;
			overlayText.setVisible(true);
		}
		sidebar.notifyStateUpdated();
	}

	// ---- TNT ---------------------------------------------------------------

	private void handleTntClick() {
		updateHoveredCell();
		if (hoveredRow < 0 || hoveredCol < 0) {
			abortTnt();
			return;
		}
		int tileId = gameState.getTile(hoveredRow, hoveredCol);
		if (tileId <= 0) {
			abortTnt();
			return;
		}
		var positions = gameState.eliminateType(tileId);
		tntSelecting = false;
		itemList.hideAbort();
		int score = gameState.tntScorePerTile();
		AudioManager.instance().playRandom("explode");
		for (Tilemap.TilePos pos : positions) {
			gameState.clearTile(pos.row(), pos.col());
			gameState.gameStatus.changeScore(score);
		}
		gridView.reset();
		if (gameState.isCleared()) {
			cleared = true;
			overlayText.setVisible(true);
		}
		sidebar.notifyStateUpdated();
	}

	private void abortTnt() {
		tntSelecting = false;
		gameState.gameStatus.items.add(ItemType.TNT, 1);
		itemList.hideAbort();
	}

	// ---- private helpers ---------------------------------------------------

	private void updateHoveredCell() {
		prevHoveredRow = hoveredRow;
		prevHoveredCol = hoveredCol;
		gridView.cellAt(
			gridOriginX, gridOriginY, mouseX, mouseY, prevHoveredRow,
			prevHoveredCol, hitResult
		);
		hoveredRow = hitResult[0];
		hoveredCol = hitResult[1];
	}

	private void updateHighlighted() {
		for (int r = 0; r < gridHeight; r++) {
			for (int c = 0; c < gridWidth; c++) {
				highlighted[r][c] = 0f;
			}
		}

		if (autoPairingState.isActive()) {
			highlightIf(autoHLRow1, autoHLCol1, 1f);
			highlightIf(autoHLRow2, autoHLCol2, 1f);
		} else if (tntSelecting && hoveredRow >= 0 && hoveredCol >= 0) {
			int tileId = gameState.getTile(hoveredRow, hoveredCol);
			if (tileId > 0) {
				for (int r = 0; r < gridHeight; r++) {
					for (int c = 0; c < gridWidth; c++) {
						if (gameState.getTile(r, c) == tileId) {
							highlighted[r][c] = 1f;
						}
					}
				}
			}
		} else {
			highlightIf(selectedRow, selectedCol, 1f);
			highlightIf(hoveredRow, hoveredCol, 0.5f);
		}

		gridView.setHighlighted(highlighted);
	}

	private void highlightIf(int row, int col, float value) {
		if (row >= 0) {
			highlighted[row][col] = Math.max(highlighted[row][col], value);
		}
	}
}

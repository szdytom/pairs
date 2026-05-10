package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;

import app.pairs.asset.IconManager;
import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.model.CountdownState;
import app.pairs.model.ItemType;
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
	private static final int SIDEBAR_WIDTH = 100;
	private static final long HINT_COOLDOWN_MS = 5_000;
	private static final long ENTRY_STAGGER_MS = 20;

	private GameState gameState;
	private final IsometricGridView gridView;
	private int gridWidth;
	private int gridHeight;
	private boolean[][] highlighted;

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

	private long totalCountdownMs;
	private final CountdownState countdownState;
	private long lastEliminationTimeMs;

	// Layout state
	private final AlignLayout alignLayout;
	private final LevelSidebar sidebar;
	private final int[] measuredSize = new int[2];
	private int gridOriginX;
	private int gridOriginY;

	// Child text components
	private final TextComponent overlayText;
	private final Button hintBtn;

	private final AutoPairingState autoPairingState = new AutoPairingState();
	private int autoHLRow1 = -1;
	private int autoHLCol1 = -1;
	private int autoHLRow2 = -1;
	private int autoHLCol2 = -1;
	private boolean gameStarted;

	public LevelComponent(
		GameState gameState, long totalCountdownMs, Blackboard blackboard
	) {
		this.gameState = gameState;
		this.gridWidth = gameState.getWidth();
		this.gridHeight = gameState.getHeight();
		this.highlighted = new boolean[gridHeight][gridWidth];

		this.totalCountdownMs = totalCountdownMs;
		this.countdownState = new CountdownState();
		countdownState.remainingMs = totalCountdownMs;
		this.lastEliminationTimeMs = countdownState.now();

		blackboard.put(GameState.class, gameState);
		blackboard.put(CountdownState.class, countdownState);
		setBlackboard(blackboard);

		this.gridView = new IsometricGridView(gridWidth, gridHeight);

		var hintIcon = IconManager.instance().getTexture("hint");
		var hintImage = new ImageComponent(hintIcon, 16, 16);
		this.hintBtn = new Button(() -> {
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
		hintBtn.setProp("h-align", AlignLayout.HAlign.RIGHT);
		hintBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		hintBtn.setProp("h-padding", 4);
		hintBtn.setProp("v-padding", 4);
		hintBtn.setVisible(false);
		hintBtn.addChild(hintImage);

		this.alignLayout = new AlignLayout();
		alignLayout.addChild(hintBtn);
		gridView.setProp("h-align", AlignLayout.HAlign.CENTER);
		gridView.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(gridView);

		this.overlayText = new TextComponent("Cleared!", 2, rgb(40, 40, 40));
		overlayText.setVisible(false);
		overlayText.setProp("h-align", AlignLayout.HAlign.CENTER);
		overlayText.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(overlayText);

		var itemList = new ItemListComponent(
			gameState.gameStatus.getCount(ItemType.AUTO_SOLVER), type -> {
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
				}
			}
		);
		itemList.setProp("v-align", AlignLayout.VAlign.BOTTOM);
		itemList.setProp("h-align", AlignLayout.HAlign.CENTER);
		itemList.setProp("v-padding", 20);
		alignLayout.addChild(itemList);

		this.sidebar = new LevelSidebar();

		addChild(alignLayout);
		addChild(sidebar);
		startEntryAnimation();
	}

	@Override
	public void update(long deltaTimeMs) {
		if (!cleared && !timedOut && gameStarted) {
			countdownState.remainingMs = Math.max(
				0, countdownState.remainingMs - deltaTimeMs
			);
			if (countdownState.remainingMs == 0) {
				timedOut = true;
				autoPairingState.clear();
				gridView.setVisible(false);
			}
		}
		boolean hintReady = !cleared && !timedOut
			&& !autoPairingState.isActive()
			&& countdownState.now() - lastEliminationTimeMs >= HINT_COOLDOWN_MS;
		hintBtn.setVisible(hintReady);
		autoPairingState.update(deltaTimeMs);
		super.update(deltaTimeMs);
	}

	/** Reset the level with the map generated. */
	public void restart() {
		countdownState.remainingMs = totalCountdownMs;
		countdownState.resetPause();
		lastEliminationTimeMs = countdownState.now();
		gameState.restart();
		int newW = gameState.getWidth();
		int newH = gameState.getHeight();
		if (newW != gridWidth || newH != gridHeight) {
			gridWidth = newW;
			gridHeight = newH;
			highlighted = new boolean[gridHeight][gridWidth];
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
		countdownState.pause();
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
			countdownState.resume();
			gameStarted = true;
		}));
	}

	/** Handle a mouse click at the current cursor position. */
	public void handleClick() {
		if (timedOut || cleared || !gameStarted) {
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
				eliminatePair(selectedRow, selectedCol, hoveredRow, hoveredCol);
				selectedRow = -1;
				selectedCol = -1;
			} else {
				selectedRow = hoveredRow;
				selectedCol = hoveredCol;
			}
		}
	}

	@Override
	public int[] measure() {
		int[] gridSize = gridView.measure();
		measuredSize[0] = gridSize[0] + SIDEBAR_WIDTH;
		measuredSize[1] = gridSize[1] + 40;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		// alignLayout fills the left area up to the sidebar.
		alignLayout.layout(0, 0, w - SIDEBAR_WIDTH, h);
		// sidebar is pinned to the right edge at its natural height.
		sidebar.layout(w - SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH, h);
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
		} else if (event instanceof KeyEvent ke) {
			if (ke.keycode() == SDLK_SPACE) {
				restart();
				return true;
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
		long now = countdownState.now();
		int elapsed = (int)(now - lastEliminationTimeMs);
		AudioManager.instance().play(
			"eliminate", gameState.getTileString(r1, c1)
		);
		gameState.eliminate(r1, c1, r2, c2, elapsed);
		lastEliminationTimeMs = now;

		gridView.reset();
		if (gameState.isCleared()) {
			cleared = true;
			overlayText.setVisible(true);
		}
		sidebar.notifyStateUpdated();
	}

	private void performAutoElimination(int r1, int c1, int r2, int c2) {
		eliminatePair(r1, c1, r2, c2);
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
				highlighted[r][c] = false;
			}
		}

		if (autoPairingState.isActive()) {
			highlightIf(autoHLRow1, autoHLCol1);
			highlightIf(autoHLRow2, autoHLCol2);
		} else {
			highlightIf(selectedRow, selectedCol);
			highlightIf(hoveredRow, hoveredCol);
		}

		gridView.setHighlighted(highlighted);
	}

	private void highlightIf(int row, int col) {
		if (row >= 0) {
			highlighted[row][col] = true;
		}
	}
}

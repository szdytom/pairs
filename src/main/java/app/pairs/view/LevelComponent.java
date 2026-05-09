package app.pairs.view;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;

import app.pairs.logic.GameState;
import app.pairs.model.CountdownState;

import io.github.libsdl4j.api.render.*;

/**
 * Top-level component that owns a {@link GameState} and an
 * {@link IsometricGridView}. Bridges the int-based tile IDs from the model to
 * the view, resolves mouse-over cells, and drives the selection/elimination
 * interaction.
 */
public class LevelComponent extends Container {
	private static final int SIDEBAR_WIDTH = 100;

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
		this.lastEliminationTimeMs = System.currentTimeMillis();

		blackboard.put(GameState.class, gameState);
		blackboard.put(CountdownState.class, countdownState);
		setBlackboard(blackboard);

		this.gridView = new IsometricGridView(gridWidth, gridHeight);

		this.alignLayout = new AlignLayout();
		gridView.setProp("h-align", AlignLayout.HAlign.CENTER);
		gridView.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(gridView);

		this.overlayText = new TextComponent("CLEARED!", 2, 40, 40, 40);
		overlayText.setVisible(false);
		overlayText.setProp("h-align", AlignLayout.HAlign.CENTER);
		overlayText.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(overlayText);

		this.sidebar = new LevelSidebar();

		addChild(alignLayout);
		addChild(sidebar);
	}

	@Override
	public void update(long deltaTimeMs) {
		if (!cleared && !timedOut) {
			countdownState.remainingMs = Math.max(
				0, countdownState.remainingMs - deltaTimeMs
			);
			if (countdownState.remainingMs == 0) {
				timedOut = true;
				gridView.setVisible(false);
			}
		}
		super.update(deltaTimeMs);
	}

	public void setMousePosition(int x, int y) {
		this.mouseX = x;
		this.mouseY = y;
	}

	/** Reset the level with the map generated. */
	public void restart() {
		countdownState.remainingMs = totalCountdownMs;
		lastEliminationTimeMs = System.currentTimeMillis();
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
		selectedRow = -1;
		selectedCol = -1;
		hoveredRow = -1;
		hoveredCol = -1;
		prevHoveredRow = -1;
		prevHoveredCol = -1;
		mouseX = -1;
		mouseY = -1;
		sidebar.notifyStateUpdated();
	}

	/** Handle a mouse click at the current cursor position. */
	public void handleClick() {
		if (timedOut || cleared) {
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
				long now = System.currentTimeMillis();
				int elapsed = (int)(now - lastEliminationTimeMs);
				gameState.operate(
					selectedRow, selectedCol, hoveredRow, hoveredCol, elapsed
				);
				lastEliminationTimeMs = now;
				gridView.reset();
				if (gameState.isCleared()) {
					cleared = true;
					overlayText.setVisible(true);
				}
				selectedRow = -1;
				selectedCol = -1;
				sidebar.notifyStateUpdated();
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

		updateHoveredCell();
		updateHighlighted();

		if (timedOut) {
			overlayText.setText("Time Out!");
		} else if (cleared) {
			overlayText.setText("CLEARED!");
		}
		overlayText.setVisible(timedOut || cleared);
		super.render(renderer, parentX, parentY, scale);
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
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

		if (selectedRow >= 0) {
			highlighted[selectedRow][selectedCol] = true;
		}
		if (hoveredRow >= 0) {
			highlighted[hoveredRow][hoveredCol] = true;
		}

		gridView.setHighlighted(highlighted);
	}
}

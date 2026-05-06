package app.pairs.view;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;

import app.pairs.logic.GameState;

import io.github.libsdl4j.api.render.*;

/**
 * Top-level component that owns a {@link GameState} and an
 * {@link IsometricGridView}. Bridges the int-based tile IDs from the model to
 * the view, resolves mouse-over cells, and drives the selection/elimination
 * interaction.
 */
public class LevelComponent extends Container {
	private static final int TILE_CONTENT_WIDTH = 16;
	private static final int TILE_CONTENT_HEIGHT = 16;

	private GameState gameState;
	private final IsometricGridView gridView;
	private final IsometricMapper mapper;
	private int gridWidth;
	private int gridHeight;
	private int[][] depthOrder;
	private boolean[][] highlighted;

	private int mouseX = -1;
	private int mouseY = -1;
	private int hoveredRow = -1;
	private int hoveredCol = -1;
	private int prevHoveredRow = -1;
	private int prevHoveredCol = -1;

	private int selectedRow = -1;
	private int selectedCol = -1;

	private boolean cleared;

	// Layout state
	private final AlignLayout alignLayout;
	private final int[] measuredSize = new int[2];
	/**
	 * Grid origin in global logical space, cached in render() for hit-testing.
	 */
	private int gridGlobalX;
	private int gridGlobalY;

	// Child text components
	private final TextComponent titleText;
	private final TextComponent scaleText;
	private final TextComponent clearedText;

	public LevelComponent(GameState gameState, IsometricMapper mapper) {
		this.gameState = gameState;
		this.mapper = mapper;
		this.gridWidth = gameState.getWidth();
		this.gridHeight = gameState.getHeight();
		this.depthOrder = mapper.getDepthSortedOrder(gridHeight, gridWidth);
		this.highlighted = new boolean[gridHeight][gridWidth];

		Blackboard bb = new Blackboard();
		bb.put(GameState.class, gameState);
		setBlackboard(bb);

		this.gridView = new IsometricGridView(mapper, gridWidth, gridHeight);

		this.alignLayout = new AlignLayout();
		alignLayout.setProp("h-align", AlignLayout.HAlign.CENTER);
		alignLayout.setProp("v-align", AlignLayout.VAlign.CENTER);
		alignLayout.addChild(gridView);

		this.titleText = new TextComponent("Pairs", 1, 200, 200, 255);
		this.scaleText = new TextComponent("Scale: 6x", 1, 180, 180, 180);
		this.clearedText = new TextComponent("CLEARED!", 2, 255, 255, 100);
		clearedText.setVisible(false);

		addChild(alignLayout);
		addChild(titleText);
		addChild(scaleText);
		addChild(clearedText);
	}

	public void setMousePosition(int x, int y) {
		this.mouseX = x;
		this.mouseY = y;
	}

	public void setScaleText(int scale) {
		scaleText.setText("Scale: " + scale + "x");
	}

	/** Reset the level with the map generated. */
	public void restart() {
		gameState.restart();
		int newW = gameState.getWidth();
		int newH = gameState.getHeight();
		if (newW != gridWidth || newH != gridHeight) {
			gridWidth = newW;
			gridHeight = newH;
			depthOrder = mapper.getDepthSortedOrder(gridHeight, gridWidth);
			highlighted = new boolean[gridHeight][gridWidth];
		}
		cleared = false;
		clearedText.setVisible(false);
		selectedRow = -1;
		selectedCol = -1;
		hoveredRow = -1;
		hoveredCol = -1;
		prevHoveredRow = -1;
		prevHoveredCol = -1;
		mouseX = -1;
		mouseY = -1;
		gridView.reset();
	}
	/** Handle a mouse click at the current cursor position. */
	public void handleClick() {
		resolveHoveredCell();

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
				gameState.operate(
					selectedRow, selectedCol, hoveredRow, hoveredCol, 0
				);
				gridView.reset();
				if (gameState.isCleared()) {
					cleared = true;
					clearedText.setVisible(true);
				}
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
		measuredSize[0] = gridSize[0];
		measuredSize[1] = gridSize[1] + 40;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		alignLayout.layout(0, 0, w, h);

		int[] titleSize = titleText.measure();
		titleText.layout(1, 1, titleSize[0], titleSize[1]);

		int[] scaleSize = scaleText.measure();
		scaleText.layout(1, 14, scaleSize[0], scaleSize[1]);

		int[] clearedSize = clearedText.measure();
		clearedText.layout(4, 12, clearedSize[0], clearedSize[1]);
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int myGlobalX = parentX + layoutX;
		int myGlobalY = parentY + layoutY;

		gridGlobalX = myGlobalX + gridView.layoutX + gridView.originOffsetX;
		gridGlobalY = myGlobalY + gridView.layoutY + gridView.originOffsetY
			+ TILE_CONTENT_HEIGHT / 3;

		resolveHoveredCell();
		updateHighlighted();

		clearedText.setVisible(cleared);
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

	private void resolveHoveredCell() {
		prevHoveredRow = hoveredRow;
		prevHoveredCol = hoveredCol;

		if (mouseX < 0 || mouseY < 0) {
			hoveredRow = -1;
			hoveredCol = -1;
			return;
		}

		int halfW = TILE_CONTENT_WIDTH / 2;
		int halfH = TILE_CONTENT_HEIGHT / 2;
		int hoverOffset = TILE_CONTENT_HEIGHT / 3;

		for (int i = depthOrder.length - 1; i >= 0; i--) {
			int[] pos = depthOrder[i];
			int r = pos[0];
			int c = pos[1];

			if (gameState.getTile(r, c) <= 0)
				continue;

			int cx = gridGlobalX + (c - r) * TILE_CONTENT_WIDTH / 2;
			int cy = gridGlobalY + (c + r) * TILE_CONTENT_WIDTH / 4;
			int left = cx - halfW;
			int right = cx + halfW;
			int top = cy - halfH;
			int bottom = cy + halfH;

			if (mouseX >= left && mouseX < right && mouseY >= top
			    && mouseY < bottom) {
				hoveredRow = r;
				hoveredCol = c;
				return;
			}

			if (r == prevHoveredRow && c == prevHoveredCol) {
				int raisedTop = top - hoverOffset;
				int raisedBottom = bottom - hoverOffset;
				if (mouseX >= left && mouseX < right && mouseY >= raisedTop
				    && mouseY < raisedBottom) {
					hoveredRow = r;
					hoveredCol = c;
					return;
				}
			}
		}

		hoveredRow = -1;
		hoveredCol = -1;
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

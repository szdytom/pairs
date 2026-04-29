package app.pairs.view;

import app.pairs.asset.TileRegistry;
import app.pairs.logic.GameState;
import app.pairs.map.TilemapFactory;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Top-level component that owns a {@link GameState} and an
 * {@link IsometricGridView}. Bridges the int-based tile IDs from the model to
 * the string-based IDs expected by the view, resolves mouse-over cells, and
 * drives the selection/elimination interaction.
 */
public class LevelComponent implements ViewComponent {
	private static final int TILE_CONTENT_WIDTH = 16;
	private static final int TILE_CONTENT_HEIGHT = 16;

	/** Remap generated type IDs (1..12) to visually interesting textures. */
	private static final String[] TYPE_TEXTURE_MAP = {
		/*  1 */ "80", /*  2 */ "81", /*  3 */ "82", /*  4 */ "83",
		/*  5 */ "84", /*  6 */ "90", /*  7 */ "91", /*  8 */ "92",
		/*  9 */ "93", /* 10 */ "94", /* 11 */ "95", /* 12 */ "96",
	};

	private final TilemapFactory factory;
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
	private int levelX;
	private int levelY;
	private final int[] measuredSize = new int[2];
	/** Grid origin offset relative to this component in logical pixels. */
	private int gridOriginX;
	private int gridOriginY;
	/**
	 * Grid origin in global logical space, cached in render() for hit-testing.
	 */
	private int gridGlobalX;
	private int gridGlobalY;

	// Child text components
	private final TextComponent titleText;
	private final TextComponent scaleText;
	private final TextComponent clearedText;

	public LevelComponent(
		TilemapFactory factory, TileRegistry tileRegistry,
		TileRegistry hlTileRegistry, IsometricMapper mapper
	) {
		this.factory = factory;
		this.gameState = GameState.fromFactory(factory);
		this.mapper = mapper;
		this.gridWidth = gameState.getWidth();
		this.gridHeight = gameState.getHeight();
		this.depthOrder = mapper.getDepthSortedOrder(gridHeight, gridWidth);
		this.highlighted = new boolean[gridHeight][gridWidth];

		this.gridView = new IsometricGridView(
			buildGrid(), tileRegistry, hlTileRegistry, mapper
		);

		this.titleText = new TextComponent("Pairs", 1, 200, 200, 255);
		this.scaleText = new TextComponent("Scale: 6x", 1, 180, 180, 180);
		this.clearedText = new TextComponent("CLEARED!", 2, 255, 255, 100);
	}

	public void setMousePosition(int x, int y) {
		this.mouseX = x;
		this.mouseY = y;
	}

	public void setScaleText(int scale) {
		scaleText.setText("Scale: " + scale + "x");
	}

	/** Reset the level with a newly generated map. */
	public void restart() {
		gameState = GameState.fromFactory(factory);
		int newW = gameState.getWidth();
		int newH = gameState.getHeight();
		if (newW != gridWidth || newH != gridHeight) {
			gridWidth = newW;
			gridHeight = newH;
			depthOrder = mapper.getDepthSortedOrder(gridHeight, gridWidth);
			highlighted = new boolean[gridHeight][gridWidth];
		}
		cleared = false;
		selectedRow = -1;
		selectedCol = -1;
		hoveredRow = -1;
		hoveredCol = -1;
		prevHoveredRow = -1;
		prevHoveredCol = -1;
		mouseX = -1;
		mouseY = -1;
		gridView.setGrid(buildGrid());
	}

	/** Handle a mouse click at the current cursor position. */
	public void handleClick() {
		// Re-resolve hovered cell so the click sees current mouseX/mouseY even
		// when no MOUSEMOTION was processed this frame before the click.
		resolveHoveredCell();

		if (hoveredRow < 0 || hoveredCol < 0) {
			return;
		}
		if (gameState.getTile(hoveredRow, hoveredCol) <= 0) {
			return;
		}

		if (selectedRow < 0) {
			// First selection.
			selectedRow = hoveredRow;
			selectedCol = hoveredCol;
		} else if (selectedRow == hoveredRow && selectedCol == hoveredCol) {
			// Click the same tile again — deselect.
			selectedRow = -1;
			selectedCol = -1;
		} else {
			// Second selection — attempt elimination.
			if (gameState.canEliminate(
					selectedRow, selectedCol, hoveredRow, hoveredCol
				)) {
				gameState.operate(
					selectedRow, selectedCol, hoveredRow, hoveredCol
				);
				gridView.setGrid(buildGrid());
				if (gameState.isCleared()) {
					cleared = true;
				}
				selectedRow = -1;
				selectedCol = -1;
			} else {
				// Invalid — move selection to the new tile.
				selectedRow = hoveredRow;
				selectedCol = hoveredCol;
			}
		}
	}

	@Override
	public void update(long deltaTimeMs) {
		// Propagate update to child components
		gridView.update(deltaTimeMs);
		titleText.update(deltaTimeMs);
		scaleText.update(deltaTimeMs);
		clearedText.update(deltaTimeMs);
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
		this.levelX = x;
		this.levelY = y;
		// Distance from grid origin to the leftmost pixel of the bounding box.
		int stepX = mapper.getTileWidth() / 2;
		int leftHalf = (gridHeight - 1) * stepX + TILE_CONTENT_WIDTH / 2;
		int margin = 5;
		gridOriginX = leftHalf + margin;
		gridOriginY = margin + TILE_CONTENT_HEIGHT / 2;
		gridView.layout(gridOriginX, gridOriginY, 0, 0);

		titleText.layout(1, 1, 0, 0);
		scaleText.layout(1, 14, 0, 0);
		clearedText.layout(4, 12, 0, 0);
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int myGlobalX = parentX + levelX;
		int myGlobalY = parentY + levelY;

		// Cache grid origin in global logical space for hit-testing.
		gridGlobalX = myGlobalX + gridOriginX;
		gridGlobalY = myGlobalY + gridOriginY;

		resolveHoveredCell();
		updateHighlighted();
		gridView.render(renderer, myGlobalX, myGlobalY, scale);
		titleText.render(renderer, myGlobalX, myGlobalY, scale);
		scaleText.render(renderer, myGlobalX, myGlobalY, scale);

		if (cleared) {
			clearedText.render(renderer, myGlobalX, myGlobalY, scale);
		}
	}

	@Override
	public void destroy() {
		gridView.destroy();
	}

	// ---- private helpers ---------------------------------------------------

	private String[][] buildGrid() {
		String[][] grid = new String[gridHeight][gridWidth];
		for (int r = 0; r < gridHeight; r++) {
			for (int c = 0; c < gridWidth; c++) {
				int tile = gameState.getTile(r, c);
				grid[r][c] = tile > 0 ? textureId(tile) : null;
			}
		}
		return grid;
	}

	/** Map a GameState tile type ID (1-based) to the sprite-sheet string ID. */
	private static String textureId(int typeId) {
		if (typeId >= 1 && typeId <= TYPE_TEXTURE_MAP.length) {
			return TYPE_TEXTURE_MAP[typeId - 1];
		}
		return Integer.toString(typeId);
	}

	/**
	 * Hit-test tiles front-to-back in logical-pixel space.  The grid origin
	 * {@link #gridGlobalX}/{@link #gridGlobalY} was cached in
	 * {@link #render(SDL_Renderer, int, int, int)}.
	 */
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

		// Front-to-back (reverse depth order)
		for (int i = depthOrder.length - 1; i >= 0; i--) {
			int[] pos = depthOrder[i];
			int r = pos[0];
			int c = pos[1];

			if (gameState.getTile(r, c) <= 0)
				continue;

			// Tile diamond centre in global logical-pixel space.
			int cx = gridGlobalX + (c - r) * TILE_CONTENT_WIDTH / 2;
			int cy = gridGlobalY + (c + r) * TILE_CONTENT_WIDTH / 4;
			int left = cx - halfW;
			int right = cx + halfW;
			int top = cy - halfH;
			int bottom = cy + halfH;

			// Normal position
			if (mouseX >= left && mouseX < right && mouseY >= top
			    && mouseY < bottom) {
				hoveredRow = r;
				hoveredCol = c;
				return;
			}

			// Previously hovered tile: also check the raised position so the
			// hover doesn't glitch when the tile lifts.
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

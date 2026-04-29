package app.pairs.view;

/**
 * Converts 2D grid coordinates to isometric logical-pixel coordinates.
 *
 * Isometric projection with 2:1 width-to-height ratio:
 *   localX = (col - row) * tileWidth / 2
 *   localY = (col + row) * tileWidth / 4
 *
 * The vertical step is half the horizontal step because the diamond
 * footprint height is half its width (2:1 ratio).
 *
 * <p>All coordinates returned are <em>local logical pixels</em> — offsets
 * from the grid's own origin.  The renderer adds a parent-supplied global
 * position and multiplies by scale to get screen coordinates.
 */
public class IsometricMapper {
	private final int tileWidth;
	private final int tileHeight;

	public IsometricMapper(int tileWidth, int tileHeight) {
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	/**
	 * Converts grid {@code (row, col)} to local logical-pixel coordinates
	 * (offset from the grid origin).
	 */
	public IsometricCoordinate gridToLogical(int row, int col) {
		int stepX = tileWidth / 2;
		int stepY = tileWidth / 4;
		int x = (col - row) * stepX;
		int y = (col + row) * stepY;
		return new IsometricCoordinate(x, y);
	}

	/**
	 * Converts local logical-pixel coordinates back to grid {@code (row, col)}.
	 */
	public int[] logicalToGrid(int localX, int localY) {
		int halfStepX = tileWidth / 2;
		int halfStepY = tileWidth / 4;

		double colMinusRow = (double)localX / halfStepX;
		double colPlusRow = (double)localY / halfStepY;

		double col = (colMinusRow + colPlusRow) / 2.0;
		double row = colPlusRow - col;

		int r = (int)Math.round(row);
		int c = (int)Math.round(col);

		return new int[] {r, c};
	}

	/**
	 * Returns {@code true} if the local-logical-pixel point is inside the
	 * visual diamond of the tile at {@code (row, col)}.
	 */
	public boolean contains(int localX, int localY, int row, int col) {
		IsometricCoordinate center = gridToLogical(row, col);
		int halfW = tileWidth / 2;
		int halfH = tileHeight / 2;
		int dx = Math.abs(localX - center.x);
		int dy = Math.abs(localY - center.y);
		return dx * halfH + dy * halfW <= halfW * halfH;
	}

	/**
	 * Gets the draw order for proper depth sorting.
	 * Returns tiles sorted back-to-front (increasing row+col sum).
	 */
	public int[][] getDepthSortedOrder(int gridRows, int gridCols) {
		int count = gridRows * gridCols;
		int[][] order = new int[count][];

		int idx = 0;
		for (int sum = 0; sum < gridRows + gridCols - 1; sum++) {
			for (int row = 0; row < gridRows; row++) {
				int col = sum - row;
				if (col >= 0 && col < gridCols) {
					order[idx++] = new int[] {row, col};
				}
			}
		}
		return order;
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public static class IsometricCoordinate {
		public final int x;
		public final int y;

		public IsometricCoordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
}

package app.pairs.view;

/**
 * Converts 2D grid coordinates to isometric screen coordinates.
 *
 * Isometric projection with 2:1 width-to-height ratio:
 *   screenX = originX + (col - row) * tileWidth * scale / 2
 *   screenY = originY + (col + row) * tileWidth * scale / 4
 *
 * The vertical step is half the horizontal step because the diamond
 * footprint height is half its width (2:1 ratio).
 */
public class IsometricMapper {
	private final int tileWidth;
	private final int tileHeight;
	private final int originX;
	private final int originY;

	public IsometricMapper(
		int tileWidth, int tileHeight, int originX, int originY
	) {
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.originX = originX;
		this.originY = originY;
	}

	/**
	 * Converts grid (row, col) to screen (x, y) at the given scale.
	 * The returned point represents the center of the tile's diamond footprint.
	 */
	public IsometricCoordinate gridToScreen(int row, int col, int scale) {
		int stepX = tileWidth * scale / 2;
		int stepY = tileWidth * scale / 4;
		int x = originX + (col - row) * stepX;
		int y = originY + (col + row) * stepY;
		return new IsometricCoordinate(x, y);
	}

	/**
	 * Converts screen (x, y) to grid (row, col) at the given scale.
	 */
	public int[] screenToGrid(int screenX, int screenY, int scale) {
		int halfStepX = tileWidth * scale / 2;
		int halfStepY = tileWidth * scale / 4;

		int relX = screenX - originX;
		int relY = screenY - originY;

		double colMinusRow = (double)relX / halfStepX;
		double colPlusRow = (double)relY / halfStepY;

		double col = (colMinusRow + colPlusRow) / 2.0;
		double row = colPlusRow - col;

		int r = (int)Math.round(row);
		int c = (int)Math.round(col);

		return new int[] {r, c};
	}

	/**
	 * Returns true if the screen point is inside the visual diamond of the
	 * tile at (row, col). The diamond matches the tile sprite extents
	 * (half-width = tileWidth * scale / 2, half-height = tileHeight * scale /
	 * 2). Uses integer-only arithmetic.
	 */
	public boolean contains(
		int screenX, int screenY, int row, int col, int scale
	) {
		IsometricCoordinate center = gridToScreen(row, col, scale);
		int halfW = tileWidth * scale / 2;
		int halfH = tileHeight * scale / 2;
		int dx = Math.abs(screenX - center.x);
		int dy = Math.abs(screenY - center.y);
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

	public int getOriginX() {
		return originX;
	}

	public int getOriginY() {
		return originY;
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

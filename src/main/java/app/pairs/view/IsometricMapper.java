package app.pairs.view;

/**
 * Converts 2D grid coordinates to isometric logical-pixel coordinates.
 *
 * Isometric projection with 2:1 width-to-height ratio:
 *   logicalX = originX + (col - row) * tileWidth / 2
 *   logicalY = originY + (col + row) * tileWidth / 4
 *
 * The vertical step is half the horizontal step because the diamond
 * footprint height is half its width (2:1 ratio).
 *
 * <p>This mapper only bridges grid ({@code row, col}) ⟷ {@linkplain
 * #gridToLogical(int, int) logical pixels}. Converting to screen pixels
 * (multiplying by {@code scale}) is handled by the renderer.
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
	 * Converts grid {@code (row, col)} to logical-pixel coordinates
	 * (positions at scale 1).
	 */
	public IsometricCoordinate gridToLogical(int row, int col) {
		int stepX = tileWidth / 2;
		int stepY = tileWidth / 4;
		int x = originX + (col - row) * stepX;
		int y = originY + (col + row) * stepY;
		return new IsometricCoordinate(x, y);
	}

	/**
	 * Converts logical-pixel coordinates back to grid {@code (row, col)}.
	 */
	public int[] logicalToGrid(int logicalX, int logicalY) {
		int halfStepX = tileWidth / 2;
		int halfStepY = tileWidth / 4;

		int relX = logicalX - originX;
		int relY = logicalY - originY;

		double colMinusRow = (double)relX / halfStepX;
		double colPlusRow = (double)relY / halfStepY;

		double col = (colMinusRow + colPlusRow) / 2.0;
		double row = colPlusRow - col;

		int r = (int)Math.round(row);
		int c = (int)Math.round(col);

		return new int[] {r, c};
	}

	/**
	 * Returns {@code true} if the logical-pixel point is inside the visual
	 * diamond of the tile at {@code (row, col)}.
	 */
	public boolean contains(int logicalX, int logicalY, int row, int col) {
		IsometricCoordinate center = gridToLogical(row, col);
		int halfW = tileWidth / 2;
		int halfH = tileHeight / 2;
		int dx = Math.abs(logicalX - center.x);
		int dy = Math.abs(logicalY - center.y);
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

package app.pairs.view;

/**
 * Simplified CSS-grid container that places children in a fixed-column grid
 * with uniform horizontal and vertical gaps.
 */
public class GridLayout extends Container {
	private final int fixedColumns;
	private final int gapX;
	private final int gapY;
	private final int[] measuredSize = new int[2];
	private final int[] cellSize = new int[2];

	public GridLayout(int fixedColumns, int gapX, int gapY) {
		if (fixedColumns < 1) {
			throw new IllegalArgumentException("fixedColumns must be >= 1");
		}
		if (gapX < 0 || gapY < 0) {
			throw new IllegalArgumentException("gapX and gapY must be >= 0");
		}
		this.fixedColumns = fixedColumns;
		this.gapX = gapX;
		this.gapY = gapY;
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			measuredSize[0] = 0;
			measuredSize[1] = 0;
			return measuredSize;
		}
		int cellW = 0;
		int cellH = 0;
		for (Widget child : children) {
			int[] size = child.measure();
			if (size[0] > cellW) {
				cellW = size[0];
			}
			if (size[1] > cellH) {
				cellH = size[1];
			}
		}
		cellSize[0] = cellW;
		cellSize[1] = cellH;
		int rows = (children.size() + fixedColumns - 1) / fixedColumns;
		measuredSize[0] = fixedColumns * cellW + (fixedColumns - 1) * gapX;
		measuredSize[1] = rows * cellH + (rows - 1) * gapY;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		int cellW, cellH;
		if (w > 0 && h > 0) {
			cellW = (w - (fixedColumns - 1) * gapX) / fixedColumns;
			cellH = cellW;
		} else {
			cellW = cellSize[0];
			cellH = cellSize[1];
		}
		int index = 0;
		for (Widget child : children) {
			int col = index % fixedColumns;
			int row = index / fixedColumns;
			child.layout(
				col * (cellW + gapX), row * (cellH + gapY), cellW, cellH
			);
			index++;
		}
	}
}

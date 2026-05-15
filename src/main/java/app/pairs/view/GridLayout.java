package app.pairs.view;

/**
 * Simplified CSS-grid container that places children in a fixed-column grid
 * with uniform padding and horizontal/vertical gaps.
 */
public class GridLayout extends Container {
	private final int fixedColumns;
	private final int gapX;
	private final int gapY;
	private final int padding;
	private final int[] measuredSize = new int[2];
	private final int[] cellSize = new int[2];

	public GridLayout(int fixedColumns, int gapX, int gapY) {
		this(fixedColumns, gapX, gapY, 0);
	}

	public GridLayout(int fixedColumns, int gapX, int gapY, int padding) {
		if (fixedColumns < 1) {
			throw new IllegalArgumentException("fixedColumns must be >= 1");
		}
		if (gapX < 0 || gapY < 0) {
			throw new IllegalArgumentException("gapX and gapY must be >= 0");
		}
		this.fixedColumns = fixedColumns;
		this.gapX = gapX;
		this.gapY = gapY;
		this.padding = padding;
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
		int visibleCount = 0;
		for (Widget child : children) {
			if (!child.isVisible()) {
				continue;
			}
			visibleCount++;
			int[] size = child.measure();
			if (size[0] > cellW) {
				cellW = size[0];
			}
			if (size[1] > cellH) {
				cellH = size[1];
			}
		}
		if (visibleCount == 0) {
			measuredSize[0] = 0;
			measuredSize[1] = 0;
			return measuredSize;
		}
		cellSize[0] = cellW;
		cellSize[1] = cellH;
		int rows = (visibleCount + fixedColumns - 1) / fixedColumns;
		int p2 = padding * 2;
		measuredSize[0] = fixedColumns * cellW + (fixedColumns - 1) * gapX + p2;
		measuredSize[1] = rows * cellH + (rows - 1) * gapY + p2;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		if (children.isEmpty()) {
			return;
		}
		int innerW = w - padding * 2;
		int innerH = h - padding * 2;
		int cellW, cellH;
		if (innerW > 0 && innerH > 0) {
			cellW = (innerW - (fixedColumns - 1) * gapX) / fixedColumns;
			cellH = (innerH - (rows() - 1) * gapY) / rows();
		} else {
			cellW = cellSize[0];
			cellH = cellSize[1];
		}
		int index = 0;
		for (Widget child : children) {
			if (!child.isVisible()) {
				continue;
			}
			int col = index % fixedColumns;
			int row = index / fixedColumns;
			child.layout(
				padding + col * (cellW + gapX), padding + row * (cellH + gapY),
				cellW, cellH
			);
			index++;
		}
	}

	private int rows() {
		int count = 0;
		for (Widget child : children) {
			if (child.isVisible()) {
				count++;
			}
		}
		return (count + fixedColumns - 1) / fixedColumns;
	}
}

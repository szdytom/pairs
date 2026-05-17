package app.pairs.view;

import java.util.Arrays;

/**
 * Simplified CSS-grid container that places children in a fixed-column grid
 * with uniform padding and horizontal/vertical gaps.
 *
 * <p>By default all cells share the same size (uniform).  Pass {@code
 * autoColumnWidths = true} to give each column its own width — the widest
 * child in that column.
 */
public class GridLayout extends Container {
	private final int fixedColumns;
	private final int gapX;
	private final int gapY;
	private final int padding;
	private final boolean autoColumnWidths;
	private final int[] measuredSize = new int[2];
	private final int[] cellSize = new int[2];
	private int[] colWidths;

	public GridLayout(int fixedColumns, int gapX, int gapY) {
		this(fixedColumns, gapX, gapY, 0, false);
	}

	public GridLayout(int fixedColumns, int gapX, int gapY, int padding) {
		this(fixedColumns, gapX, gapY, padding, false);
	}

	public GridLayout(
		int fixedColumns, int gapX, int gapY, int padding,
		boolean autoColumnWidths
	) {
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
		this.autoColumnWidths = autoColumnWidths;
		this.colWidths = new int[fixedColumns];
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			measuredSize[0] = 0;
			measuredSize[1] = 0;
			return measuredSize;
		}
		if (autoColumnWidths)
			return measureAutoColumns();

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

	private int[] measureAutoColumns() {
		Arrays.fill(colWidths, 0);
		int cellH = 0;
		int visibleCount = 0;
		int index = 0;
		for (Widget child : children) {
			if (!child.isVisible()) {
				continue;
			}
			visibleCount++;
			int[] size = child.measure();
			int col = index % fixedColumns;
			if (size[0] > colWidths[col])
				colWidths[col] = size[0];
			if (size[1] > cellH)
				cellH = size[1];
			index++;
		}
		if (visibleCount == 0) {
			measuredSize[0] = 0;
			measuredSize[1] = 0;
			return measuredSize;
		}
		cellSize[0] = 0;
		cellSize[1] = cellH;
		int rows = (visibleCount + fixedColumns - 1) / fixedColumns;
		int p2 = padding * 2;
		int totalW = p2;
		for (int i = 0; i < fixedColumns; i++) {
			totalW += colWidths[i];
			if (i > 0)
				totalW += gapX;
		}
		measuredSize[0] = totalW;
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

		if (autoColumnWidths) {
			int cellH = innerH > 0 ? (innerH - (rows() - 1) * gapY) / rows()
								   : cellSize[1];
			if (cellH < 0)
				cellH = 0;
			int index = 0;
			for (Widget child : children) {
				if (!child.isVisible()) {
					continue;
				}
				int col = index % fixedColumns;
				int row = index / fixedColumns;
				int cx = padding;
				for (int i = 0; i < col; i++)
					cx += colWidths[i] + gapX;
				child.layout(
					cx, padding + row * (cellH + gapY), colWidths[col], cellH
				);
				index++;
			}
			return;
		}

		int cellW, cellH;
		if (innerW > 0 && innerH > 0) {
			cellW = (innerW - (fixedColumns - 1) * gapX) / fixedColumns;
			cellH = (innerH - (rows() - 1) * gapY) / rows();
		} else {
			cellW = cellSize[0];
			cellH = cellSize[1];
		}
		if (cellW < 0)
			cellW = 0;
		if (cellH < 0)
			cellH = 0;
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

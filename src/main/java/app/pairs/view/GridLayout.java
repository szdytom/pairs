package app.pairs.view;

import java.util.ArrayList;
import java.util.List;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Simplified CSS-grid container that places children in a fixed-column grid
 * with uniform horizontal and vertical gaps.
 */
public class GridLayout implements ViewComponent {
	private final int fixedColumns;
	private final int gapX;
	private final int gapY;
	private final List<ViewComponent> children = new ArrayList<>();
	private int layoutX;
	private int layoutY;
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

	public void addChild(ViewComponent child) {
		children.add(child);
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			return measuredSize;
		}
		int cellW = 0;
		int cellH = 0;
		for (ViewComponent child : children) {
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
		this.layoutX = x;
		this.layoutY = y;
		int cellW, cellH;
		if (w > 0 && h > 0) {
			cellW = (w - (fixedColumns - 1) * gapX) / fixedColumns;
			cellH = cellW;
		} else {
			cellW = cellSize[0];
			cellH = cellSize[1];
		}
		int index = 0;
		for (ViewComponent child : children) {
			int col = index % fixedColumns;
			int row = index / fixedColumns;
			child.layout(
				col * (cellW + gapX), row * (cellH + gapY), cellW, cellH
			);
			index++;
		}
	}

	@Override
	public void update(long deltaTimeMs) {
		for (ViewComponent child : children) {
			child.update(deltaTimeMs);
		}
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int myGlobalX = parentX + layoutX;
		int myGlobalY = parentY + layoutY;
		for (ViewComponent child : children) {
			child.render(renderer, myGlobalX, myGlobalY, scale);
		}
	}

	@Override
	public void destroy() {
		for (ViewComponent child : children) {
			child.destroy();
		}
	}
}

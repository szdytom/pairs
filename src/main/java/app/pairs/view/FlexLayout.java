package app.pairs.view;

import java.util.ArrayList;
import java.util.List;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Simplified flex-box container that lays out children sequentially in a row
 * or column with uniform gap.
 */
public class FlexLayout implements ViewComponent {
	public enum Direction { ROW, COLUMN }

	private final Direction direction;
	private final int gap;
	private final List<ViewComponent> children = new ArrayList<>();
	private int layoutX;
	private int layoutY;
	private final int[] measuredSize = new int[2];
	private final List<int[]> childSizes = new ArrayList<>();

	public FlexLayout(Direction direction, int gap) {
		this.direction = direction;
		this.gap = gap;
	}

	public void addChild(ViewComponent child) {
		children.add(child);
	}

	@Override
	public int[] measure() {
		childSizes.clear();
		int totalW = 0;
		int totalH = 0;
		int maxW = 0;
		int maxH = 0;
		for (ViewComponent child : children) {
			int[] size = child.measure();
			childSizes.add(size);
			int cw = size[0];
			int ch = size[1];
			if (direction == Direction.ROW) {
				totalW += cw;
				if (ch > maxH) {
					maxH = ch;
				}
			} else {
				totalH += ch;
				if (cw > maxW) {
					maxW = cw;
				}
			}
		}
		int gaps = gap * Math.max(0, children.size() - 1);
		if (direction == Direction.ROW) {
			measuredSize[0] = totalW + gaps;
			measuredSize[1] = maxH;
		} else {
			measuredSize[0] = maxW;
			measuredSize[1] = totalH + gaps;
		}
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		this.layoutX = x;
		this.layoutY = y;
		int cursor = 0;
		for (int i = 0; i < children.size(); i++) {
			int[] size = childSizes.get(i);
			ViewComponent child = children.get(i);
			if (direction == Direction.ROW) {
				child.layout(cursor, 0, size[0], h);
				cursor += size[0] + gap;
			} else {
				child.layout(0, cursor, w, size[1]);
				cursor += size[1] + gap;
			}
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

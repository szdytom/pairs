package app.pairs.view;

/**
 * Simplified flex-box container that lays out children sequentially in a row
 * or column with uniform gap and padding.
 */
public class FlexLayout extends Container {
	public enum Direction { ROW, COLUMN }

	private final Direction direction;
	private final int gap;
	private final int padding;
	private final int[] measuredSize = new int[2];

	public FlexLayout(Direction direction, int gap) {
		this(direction, gap, 0);
	}

	public FlexLayout(Direction direction, int gap, int padding) {
		this.direction = direction;
		this.gap = gap;
		this.padding = padding;
	}

	@Override
	public int[] measure() {
		int totalW = 0;
		int totalH = 0;
		int maxW = 0;
		int maxH = 0;
		for (Widget child : children) {
			int[] size = child.measure();
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
		int p2 = padding * 2;
		if (direction == Direction.ROW) {
			measuredSize[0] = totalW + gaps + p2;
			measuredSize[1] = maxH + p2;
		} else {
			measuredSize[0] = maxW + p2;
			measuredSize[1] = totalH + gaps + p2;
		}
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		int cursor = padding;
		int innerW = w - padding * 2;
		int innerH = h - padding * 2;
		for (Widget child : children) {
			int[] size = child.measure();
			if (direction == Direction.ROW) {
				child.layout(cursor, padding, size[0], innerH);
				cursor += size[0] + gap;
			} else {
				child.layout(padding, cursor, innerW, size[1]);
				cursor += size[1] + gap;
			}
		}
	}
}

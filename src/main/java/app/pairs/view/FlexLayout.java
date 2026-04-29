package app.pairs.view;

/**
 * Simplified flex-box container that lays out children sequentially in a row
 * or column with uniform gap.
 */
public class FlexLayout extends Container {
	public enum Direction { ROW, COLUMN }

	private final Direction direction;
	private final int gap;
	private final int[] measuredSize = new int[2];

	public FlexLayout(Direction direction, int gap) {
		this.direction = direction;
		this.gap = gap;
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
		super.layout(x, y, w, h);
		int cursor = 0;
		for (int i = 0; i < children.size(); i++) {
			Widget child = children.get(i);
			int[] size = child.measure(); // re-measure since childSizes was
			                              // removed
			if (direction == Direction.ROW) {
				child.layout(cursor, 0, size[0], h);
				cursor += size[0] + gap;
			} else {
				child.layout(0, cursor, w, size[1]);
				cursor += size[1] + gap;
			}
		}
	}
}

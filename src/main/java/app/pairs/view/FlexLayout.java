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
		int innerW = w - padding * 2;
		int innerH = h - padding * 2;

		int totalNatural = 0;
		int totalGrow = 0;
		for (Widget child : children) {
			int[] size = child.measure();
			Integer grow = child.getProp("flex-grow");
			if (grow != null && grow > 0) {
				totalGrow += grow;
			}
			if (direction == Direction.ROW) {
				totalNatural += size[0];
			} else {
				totalNatural += size[1];
			}
		}
		int gaps = gap * Math.max(0, children.size() - 1);
		int avail = direction == Direction.ROW ? innerW : innerH;
		int remaining = Math.max(0, avail - totalNatural - gaps);

		int cursor = padding;
		for (Widget child : children) {
			int[] size = child.measure();
			Integer grow = child.getProp("flex-grow");
			if (direction == Direction.ROW) {
				int cw = size[0];
				if (grow != null && grow > 0 && totalGrow > 0) {
					cw += remaining * grow / totalGrow;
				}
				child.layout(cursor, padding, cw, innerH);
				cursor += cw + gap;
			} else {
				int ch = size[1];
				if (grow != null && grow > 0 && totalGrow > 0) {
					ch += remaining * grow / totalGrow;
				}
				child.layout(padding, cursor, innerW, ch);
				cursor += ch + gap;
			}
		}
	}
}

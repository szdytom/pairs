package app.pairs.view;

/**
 * Container that aligns each child independently within the available space.
 * Children may overlap freely. Later children are rendered later (higher
 * z-index).
 *
 * <p>Each child's alignment is controlled via its own {@code "h-align"}
 * and {@code "v-align"} properties, which accept {@link HAlign} and
 * {@link VAlign} enum values respectively:
 * <pre>{@code
 * child.setProp("h-align", AlignLayout.HAlign.CENTER);
 * child.setProp("v-align", AlignLayout.VAlign.BOTTOM);
 * }</pre>
 * Defaults are {@link HAlign#LEFT} and {@link VAlign#TOP}.
 *
 * <p>Each child keeps its measured natural size and is positioned according
 * to the alignment rules within the allocated rectangle.  The child's
 * {@code layoutX/Y} is always the top-left corner of its measured bounding
 * box (standard convention).
 */
public class AlignLayout extends Container {
	public enum HAlign { LEFT, CENTER, RIGHT }
	public enum VAlign { TOP, CENTER, BOTTOM }

	private final int[] measuredSize = new int[2];

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			return Widget.ZERO_SIZE;
		}
		int maxW = 0;
		int maxH = 0;
		for (Widget child : children) {
			int[] s = child.measure();
			if (s[0] > maxW)
				maxW = s[0];
			if (s[1] > maxH)
				maxH = s[1];
		}
		measuredSize[0] = maxW;
		measuredSize[1] = maxH;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);

		for (Widget child : children) {
			HAlign ha = child.getProp("h-align");
			if (ha == null)
				ha = HAlign.LEFT;
			VAlign va = child.getProp("v-align");
			if (va == null)
				va = VAlign.TOP;

			int[] childSize = child.measure();
			int cw = childSize[0];
			int ch = childSize[1];

			int cx = switch (ha) {
				case LEFT -> 0;
				case CENTER -> (w - cw) / 2;
				case RIGHT -> w - cw;
			};
			int cy = switch (va) {
				case TOP -> 0;
				case CENTER -> (h - ch) / 2;
				case BOTTOM -> h - ch;
			};

			child.layout(cx, cy, cw, ch);
		}
	}
}

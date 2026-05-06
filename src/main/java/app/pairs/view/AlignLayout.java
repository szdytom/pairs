package app.pairs.view;

/**
 * Container that aligns its single child within the available space.
 *
 * <p>Alignment is controlled via the {@code "h-align"} and
 * {@code "v-align"} properties, which accept {@link HAlign} and
 * {@link VAlign} enum values respectively:
 * <pre>{@code
 * layout.setProp("h-align", AlignLayout.HAlign.CENTER);
 * layout.setProp("v-align", AlignLayout.VAlign.BOTTOM);
 * }</pre>
 * Defaults are {@link HAlign#LEFT} and {@link VAlign#TOP}.
 *
 * <p>The child is placed but NOT resized — the child keeps its measured
 * natural size and is positioned according to the alignment rules within
 * the allocated rectangle.  The child's {@code layoutX/Y} is always the
 * top-left corner of its measured bounding box (standard convention).
 */
public class AlignLayout extends Container {
	public enum HAlign { LEFT, CENTER, RIGHT }
	public enum VAlign { TOP, CENTER, BOTTOM }

	@Override
	public void addChild(Widget child) {
		if (!children.isEmpty()) {
			throw new IllegalStateException(
				"AlignLayout supports only one child"
			);
		}
		super.addChild(child);
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			return Widget.ZERO_SIZE;
		}
		return children.getFirst().measure();
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		if (children.isEmpty()) {
			return;
		}

		Widget child = children.getFirst();
		int[] childSize = child.measure();
		int cw = childSize[0];
		int ch = childSize[1];

		HAlign ha = getProp("h-align");
		if (ha == null)
			ha = HAlign.LEFT;
		VAlign va = getProp("v-align");
		if (va == null)
			va = VAlign.TOP;

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

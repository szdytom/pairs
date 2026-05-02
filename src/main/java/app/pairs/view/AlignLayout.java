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
 * <p>By default the child's {@code layoutX/Y} is the top-left corner of its
 * bounding box ({@link Origin#TOP_LEFT}), which is the standard convention
 * for rectangular widgets.  For widgets whose layout origin is at the visual
 * center of their measured bounding box (e.g. isometric grids), set
 * {@code "h-origin"} and/or {@code "v-origin"} to {@link Origin#CENTER}:
 * <pre>{@code
 * layout.setProp("h-origin", AlignLayout.Origin.CENTER);
 * layout.setProp("v-origin", AlignLayout.Origin.TOP_LEFT);
 * }</pre>
 * Setting {@code "origin"} sets both axes at once.
 *
 * <p>The child is placed but NOT resized — the child keeps its measured
 * natural size and is positioned according to the alignment rules within
 * the allocated rectangle.
 */
public class AlignLayout extends Container {
	public enum HAlign { LEFT, CENTER, RIGHT }
	public enum VAlign { TOP, CENTER, BOTTOM }
	public enum Origin { TOP_LEFT, CENTER }

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

		Origin sharedOrigin = getProp("origin");
		Origin hOrigin = getProp("h-origin");
		if (hOrigin == null)
			hOrigin = sharedOrigin;
		if (hOrigin == null)
			hOrigin = Origin.TOP_LEFT;
		Origin vOrigin = getProp("v-origin");
		if (vOrigin == null)
			vOrigin = sharedOrigin;
		if (vOrigin == null)
			vOrigin = Origin.TOP_LEFT;

		int cx = switch (hOrigin) {
			case TOP_LEFT ->
				switch (ha) {
				case LEFT -> 0;
				case CENTER -> (w - cw) / 2;
				case RIGHT -> w - cw;
				};
			case CENTER ->
				switch (ha) {
				case LEFT -> cw / 2;
				case CENTER -> w / 2;
				case RIGHT -> w - cw / 2;
				};
		};
		int cy = switch (vOrigin) {
			case TOP_LEFT ->
				switch (va) {
				case TOP -> 0;
				case CENTER -> (h - ch) / 2;
				case BOTTOM -> h - ch;
				};
			case CENTER ->
				switch (va) {
				case TOP -> ch / 2;
				case CENTER -> h / 2;
				case BOTTOM -> h - ch / 2;
				};
		};

		child.layout(cx, cy, cw, ch);
	}
}

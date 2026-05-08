package app.pairs.view;

import io.github.libsdl4j.api.render.*;

public class ScrollListLayout extends Container {
	private final int gap;
	private final int padding;
	private int scrollIndex;
	private final int[] measuredSize = new int[2];

	public ScrollListLayout(int gap, int padding) {
		this.gap = gap;
		this.padding = padding;
	}

	public void scrollBy(int delta) {
		if (children.isEmpty())
			return;
		scrollBy(delta > 0 ? 1 : -1, false);
	}

	public void scrollBy(int delta, boolean page) {
		if (children.isEmpty())
			return;
		int step = page ? Math.max(1, visibleChildCount()) : 1;
		scrollIndex = Math.max(
			0, Math.min(scrollIndex + delta * step, maxScrollIndex())
		);
		blackboard().layoutDirty = true;
	}

	public void scrollToBottom() {
		scrollIndex = Integer.MAX_VALUE;
		blackboard().layoutDirty = true;
	}

	public boolean isAtBottom() {
		return scrollIndex >= maxScrollIndex();
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			measuredSize[0] = 0;
			measuredSize[1] = 0;
			return measuredSize;
		}
		int maxW = 0;
		int totalH = 0;
		for (Widget child : children) {
			int[] size = child.measure();
			if (size[0] > maxW)
				maxW = size[0];
			totalH += size[1];
		}
		int gaps = gap * Math.max(0, children.size() - 1);
		int p2 = padding * 2;
		measuredSize[0] = maxW + p2;
		measuredSize[1] = totalH + gaps + p2;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		if (children.isEmpty())
			return;

		int n = children.size();
		int innerW = w - 2 * padding;
		int viewportContentH = h - 2 * padding;

		int[] absY = new int[n];
		int[] childH = new int[n];
		int cy = 0;
		for (int i = 0; i < n; i++) {
			absY[i] = cy;
			childH[i] = children.get(i).measure()[1];
			cy += childH[i] + gap;
		}

		int maxScroll = 0;
		if (cy > viewportContentH) {
			int used = 0;
			for (int i = n - 1; i >= 0; i--) {
				used += childH[i];
				if (used > viewportContentH) {
					maxScroll = Math.min(i + 1, n - 1);
					break;
				}
				used += gap;
				maxScroll = i;
			}
		}
		scrollIndex = Math.min(scrollIndex, maxScroll);

		int base = absY[scrollIndex];
		for (int i = 0; i < n; i++) {
			Widget child = children.get(i);
			child.layout(padding, padding + absY[i] - base, innerW, childH[i]);
		}
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int myGlobalX = parentX + layoutX;
		int myGlobalY = parentY + layoutY;

		for (int i = scrollIndex; i < children.size(); i++) {
			Widget child = children.get(i);
			if (!child.isVisible())
				continue;
			if (child.layoutY + child.layoutH > layoutH)
				break;
			child.render(renderer, myGlobalX, myGlobalY, scale);
		}
	}

	@Override
	public boolean dispatchEvent(
		Event event, int parentGlobalX, int parentGlobalY
	) {
		int myGlobalX = parentGlobalX + layoutX;
		int myGlobalY = parentGlobalY + layoutY;

		if (event instanceof MouseEvent me) {
			if (me.x() < myGlobalX || me.x() >= myGlobalX + layoutW
			    || me.y() < myGlobalY || me.y() >= myGlobalY + layoutH) {
				return false;
			}
			for (int i = scrollIndex; i < children.size(); i++) {
				Widget child = children.get(i);
				if (!child.isVisible())
					continue;
				if (child.layoutY + child.layoutH > layoutH)
					break;
				int cx = myGlobalX + child.layoutX;
				int cy = myGlobalY + child.layoutY;
				if (me.x() >= cx && me.x() < cx + child.layoutW && me.y() >= cy
				    && me.y() < cy + child.layoutH) {
					if (child.dispatchEvent(event, myGlobalX, myGlobalY)
					    || event.isConsumed()) {
						return true;
					}
					return onEvent(event) || event.isConsumed();
				}
			}
		} else if (event instanceof ScrollEvent se) {
			if (se.x() >= myGlobalX && se.x() < myGlobalX + layoutW
			    && se.y() >= myGlobalY && se.y() < myGlobalY + layoutH) {
				int dir = se.delta() > 0 ? -1 : 1;
				scrollBy(dir);
				return true;
			}
			return false;
		}
		return onEvent(event) || event.isConsumed();
	}

	private int visibleChildCount() {
		if (children.isEmpty() || layoutH <= 0)
			return 0;
		int count = 0;
		int y = padding;
		for (int i = scrollIndex; i < children.size(); i++) {
			int ch = children.get(i).layoutH;
			if (y + ch > layoutH)
				break;
			y += ch + gap;
			count++;
		}
		return count;
	}

	private int maxScrollIndex() {
		int n = children.size();
		if (n == 0)
			return 0;
		int vh = layoutH - 2 * padding;
		int used = 0;
		for (int i = n - 1; i >= 0; i--) {
			used += children.get(i).layoutH;
			if (used > vh)
				return Math.min(i + 1, n - 1);
			used += gap;
		}
		return 0;
	}
}

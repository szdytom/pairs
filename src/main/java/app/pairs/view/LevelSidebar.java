package app.pairs.view;

import static io.github.libsdl4j.api.render.SdlRender.*;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * A fixed-width panel that wraps an {@link OpLogList} and draws a left border.
 * Renders the border as a 1 logical-pixel vertical line at its left edge.
 */
public class LevelSidebar extends Container {
	private static final int BORDER_WIDTH = 1;

	private final OpLogList opLogList;
	private final int[] measuredSize = new int[2];
	private final SDL_Rect borderRect = new SDL_Rect();

	public LevelSidebar() {
		this.opLogList = new OpLogList();
		addChild(opLogList);
	}

	public void notifyStateUpdated() {
		opLogList.dispatchEvent(new Event(Event.Type.GAME_STATE_UPDATED), 0, 0);
	}

	@Override
	public int[] measure() {
		int[] childSize = opLogList.measure();
		measuredSize[0] = childSize[0] + BORDER_WIDTH;
		measuredSize[1] = childSize[1];
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		opLogList.layout(BORDER_WIDTH, 0, w - BORDER_WIDTH, h);
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int gx = (parentX + layoutX) * scale;
		int gy = (parentY + layoutY) * scale;
		int gh = layoutH * scale;

		// Left border: 1 logical pixel at the widget's left edge.
		borderRect.x = gx;
		borderRect.y = gy;
		borderRect.w = BORDER_WIDTH * scale;
		borderRect.h = gh;
		SDL_SetRenderDrawColor(
			renderer, (byte)80, (byte)80, (byte)80, (byte)255
		);
		SDL_RenderFillRect(renderer, borderRect);

		super.render(renderer, parentX, parentY, scale);
	}

	/** Exposed for event forwarding from the parent. */
	OpLogList opLogList() {
		return opLogList;
	}
}

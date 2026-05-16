package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.render.SdlRender.*;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * A fixed-width panel that wraps an {@link OpLogList} and draws a left border.
 * Renders the border as a 1 logical-pixel vertical line at its left edge.
 */
public class LevelSidebar extends Container {
	private static final int BORDER_WIDTH = 1;

	private final FlexLayout column;
	private final OpLogList opLogList;
	private final int[] measuredSize = new int[2];
	private final SDL_Rect borderRect = new SDL_Rect();

	public LevelSidebar() {
		LevelInfo levelInfo = new LevelInfo();
		var levelInfoWrap = new AlignLayout();
		levelInfo.setProp("h-align", AlignLayout.HAlign.CENTER);
		levelInfoWrap.addChild(levelInfo);

		this.column = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		column.addChild(levelInfoWrap);

		this.opLogList = new OpLogList();
		opLogList.setProp("flex-grow", 1);
		column.addChild(opLogList);

		addChild(column);
	}

	public void notifyStateUpdated() {
		opLogList.dispatchEvent(new Event(Event.Type.GAME_STATE_UPDATED), 0, 0);
	}

	@Override
	public int[] measure() {
		int[] s = column.measure();
		measuredSize[0] = s[0] + BORDER_WIDTH;
		measuredSize[1] = s[1];
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		column.layout(BORDER_WIDTH, 0, w - BORDER_WIDTH, h);
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
		setRenderDrawColor(renderer, rgb(80, 80, 80));
		SDL_RenderFillRect(renderer, borderRect);

		super.render(renderer, parentX, parentY, scale);
	}

	/** Exposed for event forwarding from the parent. */
	OpLogList opLogList() {
		return opLogList;
	}
}

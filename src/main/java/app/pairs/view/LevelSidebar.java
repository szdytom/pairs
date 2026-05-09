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

	private final AlignLayout levelInfoWrap;
	private final OpLogList opLogList;
	private final int[] measuredSize = new int[2];
	private final SDL_Rect borderRect = new SDL_Rect();

	public LevelSidebar() {
		LevelInfo levelInfo = new LevelInfo();
		this.levelInfoWrap = new AlignLayout();
		levelInfo.setProp("h-align", AlignLayout.HAlign.CENTER);
		levelInfoWrap.addChild(levelInfo);
		this.opLogList = new OpLogList();
		addChild(levelInfoWrap);
		addChild(opLogList);
	}

	public void notifyStateUpdated() {
		opLogList.dispatchEvent(new Event(Event.Type.GAME_STATE_UPDATED), 0, 0);
	}

	@Override
	public int[] measure() {
		int[] infoSize = levelInfoWrap.measure();
		int[] listSize = opLogList.measure();
		measuredSize[0] = Math.max(infoSize[0], listSize[0]) + BORDER_WIDTH;
		measuredSize[1] = infoSize[1] + listSize[1];
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		int innerW = w - BORDER_WIDTH;
		int infoH = levelInfoWrap.measure()[1];
		levelInfoWrap.layout(BORDER_WIDTH, 0, innerW, infoH);
		opLogList.layout(BORDER_WIDTH, infoH, innerW, h - infoH);
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

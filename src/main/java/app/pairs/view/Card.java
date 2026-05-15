package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.blendmode.SDL_BlendMode.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.*;

public class Card extends Container {
	private final int fixedWidth;
	private final int color;
	private final int hoverColor;
	private final int hPadding;
	private final int vPadding;
	private final SDL_Rect bgRect = new SDL_Rect();
	private final int[] measuredSize = new int[2];

	public Card(
		int fixedWidth, int color, int hoverColor, int hPadding, int vPadding
	) {
		this.fixedWidth = fixedWidth;
		this.color = color;
		this.hoverColor = hoverColor;
		this.hPadding = hPadding;
		this.vPadding = vPadding;
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			return Widget.ZERO_SIZE;
		}
		int[] childSize = children.get(0).measure();
		measuredSize[0] = fixedWidth;
		measuredSize[1] = childSize[1] + vPadding * 2;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		for (Widget child : children) {
			child.layout(
				hPadding, vPadding, layoutW - hPadding * 2,
				layoutH - vPadding * 2
			);
		}
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		bgRect.x = (parentX + layoutX) * scale;
		bgRect.y = (parentY + layoutY) * scale;
		bgRect.w = layoutW * scale;
		bgRect.h = layoutH * scale;

		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND);
		setRenderDrawColor(renderer, hovered ? hoverColor : color);
		SDL_RenderFillRect(renderer, bgRect);
		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_NONE);

		super.render(renderer, parentX, parentY, scale);
	}
}

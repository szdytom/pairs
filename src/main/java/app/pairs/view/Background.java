package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.blendmode.SDL_BlendMode.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.*;

public class Background extends Container {
	private int color;
	private int hoverColor;
	private final SDL_Rect bgRect = new SDL_Rect();
	private final int[] measuredSize = new int[2];

	public Background(int color) {
		this(color, color);
	}

	public Background(int color, int hoverColor) {
		this.color = color;
		this.hoverColor = hoverColor;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void setHoverColor(int hoverColor) {
		this.hoverColor = hoverColor;
	}

	@Override
	public int[] measure() {
		if (children.isEmpty()) {
			return Widget.ZERO_SIZE;
		}
		int maxW = 0;
		int maxH = 0;
		for (Widget child : children) {
			int[] s = child.measure();
			if (s[0] > maxW) {
				maxW = s[0];
			}
			if (s[1] > maxH) {
				maxH = s[1];
			}
		}
		measuredSize[0] = maxW;
		measuredSize[1] = maxH;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		for (Widget child : children) {
			child.layout(0, 0, w, h);
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

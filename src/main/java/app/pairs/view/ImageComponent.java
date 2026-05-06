package app.pairs.view;

import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderCopy;

import io.github.libsdl4j.api.rect.*;
import io.github.libsdl4j.api.render.*;

/**
 * Renders an {@link SDL_Texture} cropped to the specified content area
 * (1 px left inset to skip the shadow border).
 */
public class ImageComponent extends Widget {
	private final SDL_Texture texture;
	private final SDL_Rect srcRect = new SDL_Rect();
	private final int contentW;
	private final int contentH;
	private final int[] measuredSize = new int[2];
	private final SDL_Rect dstRect = new SDL_Rect();

	public ImageComponent(SDL_Texture texture, int contentW, int contentH) {
		this.texture = texture;
		this.contentW = contentW;
		this.contentH = contentH;
		srcRect.x = 1;
		srcRect.y = 0;
		srcRect.w = contentW;
		srcRect.h = contentH;
		measuredSize[0] = contentW;
		measuredSize[1] = contentH;
	}

	@Override
	public int[] measure() {
		return measuredSize;
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		if (texture == null) {
			return;
		}
		dstRect.x = (parentX + layoutX) * scale;
		dstRect.y = (parentY + layoutY) * scale;
		dstRect.w = contentW * scale;
		dstRect.h = contentH * scale;
		SDL_RenderCopy(renderer, texture, srcRect, dstRect);
	}
}

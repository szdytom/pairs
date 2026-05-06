package app.pairs.view;

import static io.github.libsdl4j.api.render.SdlRender.SDL_RenderCopy;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;

/**
 * Renders a fixed-size {@link SDL_Texture} cropped to the tile content area
 * (1 px inset to skip the shadow border).
 */
public class ImageComponent extends Widget {
	private static final int SIZE = 16;

	private final SDL_Texture texture;
	private final SDL_Rect srcRect = new SDL_Rect();
	private final int[] measuredSize = new int[] {SIZE, SIZE};

	public ImageComponent(SDL_Texture texture) {
		this.texture = texture;
		srcRect.x = 1;
		srcRect.y = 1;
		srcRect.w = SIZE;
		srcRect.h = SIZE;
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
		SDL_Rect dst = new SDL_Rect();
		dst.x = (parentX + layoutX) * scale;
		dst.y = (parentY + layoutY) * scale;
		dst.w = SIZE * scale;
		dst.h = SIZE * scale;
		SDL_RenderCopy(renderer, texture, srcRect, dst);
	}
}

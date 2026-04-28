package app.pairs.view;

import app.pairs.asset.BitmapFont;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.render.SdlRender;

/**
 * Utility for rendering text with a BitmapFont.
 *
 * Extrinsic state (position, color, scale, text) is passed as parameters;
 * intrinsic glyph data and textures live in the shared BitmapFont (flyweight).
 */
public final class BitmapFontRenderer {
	private BitmapFontRenderer() {}

	/**
	 * Renders a line of text. Newlines ('\n') move to the next line;
	 * characters not in the font are silently skipped.
	 *
	 * @param x     logical x-coordinate (screen position = {@code x * scale})
	 * @param y     logical y-coordinate (screen position = {@code y * scale})
	 * @param size  logical font size (effective pixel scale = {@code size *
	 *     scale})
	 * @param scale global pixel scale factor
	 */
	public static void renderText(
		SDL_Renderer renderer, BitmapFont font, String text, int x, int y,
		int size, int scale, int r, int g, int b
	) {
		int cursorX = x * scale;
		int cursorY = y * scale;
		int effective = size * scale;
		int lineHeight = BitmapFont.GLYPH_HEIGHT * effective;
		SDL_Rect dstRect = new SDL_Rect();
		int i = 0;
		while (i < text.length()) {
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);

			if (cp == '\n') {
				cursorX = x * scale;
				cursorY += lineHeight;
				continue;
			}

			int advance = font.getGlyphWidth(cp);
			SDL_Texture tex = font.getTexture(cp);
			if (tex != null) {
				int w = advance * effective;
				dstRect.x = cursorX;
				dstRect.y = cursorY;
				dstRect.w = w;
				dstRect.h = lineHeight;

				SdlRender.SDL_SetTextureColorMod(
					tex, (byte)r, (byte)g, (byte)b
				);
				SdlRender.SDL_RenderCopy(
					renderer, tex, (io.github.libsdl4j.api.rect.SDL_Rect)null,
					dstRect
				);
			}

			cursorX += advance * effective;
		}
	}

	/**
	 * Returns the logical width of {@code text} (in logical pixels, before
	 * multiplying by {@code scale}). Returns -1 if the text contains a newline.
	 */
	public static int measureText(BitmapFont font, String text, int size) {
		int width = 0;
		for (int i = 0; i < text.length();) {
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);
			if (cp == '\n') {
				return -1;
			}
			width += font.getGlyphWidth(cp);
		}
		return width * size;
	}
}

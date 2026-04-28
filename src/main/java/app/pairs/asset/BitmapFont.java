package app.pairs.asset;

import static io.github.libsdl4j.api.blendmode.SDL_BlendMode.SDL_BLENDMODE_BLEND;
import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_ABGR8888;

import java.util.HashMap;
import java.util.Map;

import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.render.SdlRender;
import io.github.libsdl4j.api.surface.SDL_Surface;
import io.github.libsdl4j.api.surface.SdlSurface;

/**
 * Flyweight: glyph bitmap data and pre-created textures are intrinsic (shared)
 * state. Position, color, and text string are extrinsic and passed at render
 * time.
 */
public class BitmapFont {
	public static final int GLYPH_HEIGHT = 12;
	public static final int DEFAULT_ADVANCE = 6;

	private final Map<Integer, int[]> glyphData;
	private final Map<Integer, SDL_Texture> glyphTextures;
	private final Map<Integer, Integer> glyphWidths;

	BitmapFont(Map<Integer, int[]> data) {
		this.glyphData = data;
		this.glyphTextures = new HashMap<>();
		this.glyphWidths = new HashMap<>();
		computeGlyphWidths();
	}

	private void computeGlyphWidths() {
		for (var entry : glyphData.entrySet()) {
			int cp = entry.getKey();
			int[] rows = entry.getValue();
			int maxBits = 0;
			for (int v : rows) {
				if (v == 0) {
					continue;
				}
				int bits = 32 - Integer.numberOfLeadingZeros(v);
				if (bits > maxBits) {
					maxBits = bits;
				}
			}
			if (maxBits == 0) {
				glyphWidths.put(cp, DEFAULT_ADVANCE);
			} else {
				glyphWidths.put(cp, maxBits + 1);
			}
		}
	}

	/**
	 * Pre-creates an SDL_Texture for every non-empty glyph.
	 * Call once after construction, before any render calls.
	 */
	public void prebuildTextures(SDL_Renderer renderer) {
		for (var entry : glyphData.entrySet()) {
			int cp = entry.getKey();
			int width = glyphWidths.get(cp);
			int[] rows = entry.getValue();

			if (!hasContent(rows)) {
				continue;
			}

			int activeBits = width - 1;
			SDL_Surface surface = SdlSurface.SDL_CreateRGBSurfaceWithFormat(
				0, width, GLYPH_HEIGHT, 32, SDL_PIXELFORMAT_ABGR8888
			);

			byte[] pixels = new byte[width * GLYPH_HEIGHT * 4];
			for (int row = 0; row < GLYPH_HEIGHT; row++) {
				int val = rows[row];
				for (int col = 0; col < activeBits; col++) {
					boolean on = ((val >> col) & 1) == 1;
					if (on) {
						int idx = (row * width + col) * 4;
						pixels[idx + 0] = (byte)0xFF;
						pixels[idx + 1] = (byte)0xFF;
						pixels[idx + 2] = (byte)0xFF;
						pixels[idx + 3] = (byte)0xFF;
					}
				}
			}
			surface.getPixels().write(0, pixels, 0, pixels.length);

			SDL_Texture texture = SdlRender.SDL_CreateTextureFromSurface(
				renderer, surface
			);
			SdlRender.SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_BLEND);
			SdlSurface.SDL_FreeSurface(surface);
			glyphTextures.put(cp, texture);
		}
	}

	private static boolean hasContent(int[] rows) {
		for (int v : rows) {
			if (v != 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasGlyph(int codePoint) {
		return glyphData.containsKey(codePoint);
	}

	public SDL_Texture getTexture(int codePoint) {
		return glyphTextures.get(codePoint);
	}

	/**
	 * Returns the advance width for a glyph (active bits + 1 spacing column),
	 * or DEFAULT_ADVANCE for characters not in the font.
	 */
	public int getGlyphWidth(int codePoint) {
		return glyphWidths.getOrDefault(codePoint, DEFAULT_ADVANCE);
	}

	public void dispose() {
		for (SDL_Texture tex : glyphTextures.values()) {
			SdlRender.SDL_DestroyTexture(tex);
		}
		glyphTextures.clear();
	}
}

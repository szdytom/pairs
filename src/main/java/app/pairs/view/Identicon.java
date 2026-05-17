package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_ABGR8888;
import static io.github.libsdl4j.api.render.SdlRender.SDL_CreateTextureFromSurface;
import static io.github.libsdl4j.api.surface.SdlSurface.*;

import app.pairs.asset.AssetManager;

import io.github.libsdl4j.api.render.*;
import io.github.libsdl4j.api.surface.*;

public class Identicon {
	private static final int GRID = 5;
	private static final int SCALE = 6;
	private static final int BORDER = 1;
	public static final int TEX_SIZE = GRID * SCALE + 2 * BORDER;

	private static final int EMPTY_COLOR = rgba(0, 0, 0, 0);
	private static final int BORDER_COLOR = rgb(0, 0, 0);

	public static SDL_Texture create(String username) {
		long hash = fnv1a(username);
		int color = pickColor(hash);

		SDL_Surface surface = SDL_CreateRGBSurfaceWithFormat(
			0, TEX_SIZE, TEX_SIZE, 32, SDL_PIXELFORMAT_ABGR8888
		);
		byte[] data = new byte[TEX_SIZE * TEX_SIZE * 4];

		for (int gy = 0; gy < GRID; gy++) {
			for (int gx = 0; gx < GRID; gx++) {
				int sx = gx > GRID / 2 ? GRID - 1 - gx : gx;
				int bit = gy * (GRID / 2 + 1) + sx;
				boolean fill = ((hash >>> bit) & 1) == 1;
				int cellColor = fill ? color : EMPTY_COLOR;
				for (int dy = 0; dy < SCALE; dy++) {
					for (int dx = 0; dx < SCALE; dx++) {
						int px = BORDER + gx * SCALE + dx;
						int py = BORDER + gy * SCALE + dy;
						setPixel(data, px, py, cellColor);
					}
				}
			}
		}

		for (int i = 0; i < TEX_SIZE; i++) {
			setPixel(data, i, 0, BORDER_COLOR);
			setPixel(data, i, TEX_SIZE - 1, BORDER_COLOR);
			setPixel(data, 0, i, BORDER_COLOR);
			setPixel(data, TEX_SIZE - 1, i, BORDER_COLOR);
		}

		surface.getPixels().write(0, data, 0, data.length);
		SDL_Renderer renderer = AssetManager.instance().renderer();
		SDL_Texture texture = SDL_CreateTextureFromSurface(renderer, surface);
		SDL_FreeSurface(surface);
		return texture;
	}

	private static long fnv1a(String s) {
		long hash = 0xcbf29ce484222325L;
		for (int i = 0; i < s.length(); i++) {
			hash ^= s.charAt(i);
			hash *= 0x100000001b3L;
		}
		return hash;
	}

	private static int pickColor(long hash) {
		float hue = ((hash >>> 48) & 0xFF) * 360f / 256;
		float sat = 0.5f + ((hash >>> 40) & 0x3F) / 128f;
		float light = 0.4f + ((hash >>> 32) & 0x3F) / 128f;
		return hsl(hue, Math.min(sat, 1f), Math.min(light, 1f));
	}

	private static void setPixel(byte[] data, int x, int y, int color) {
		int idx = (y * TEX_SIZE + x) * 4;
		if (color == 0) {
			data[idx] = 0;
			data[idx + 1] = 0;
			data[idx + 2] = 0;
			data[idx + 3] = 0;
		} else {
			data[idx] = (byte)b(color);
			data[idx + 1] = (byte)g(color);
			data[idx + 2] = (byte)r(color);
			data[idx + 3] = (byte)255;
		}
	}
}

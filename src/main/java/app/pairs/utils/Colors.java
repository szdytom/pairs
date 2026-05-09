package app.pairs.utils;

import static io.github.libsdl4j.api.render.SdlRender.SDL_SetRenderDrawColor;

import io.github.libsdl4j.api.render.SDL_Renderer;

public final class Colors {
	private Colors() {}

	private static final int ALPHA_SHIFT = 24;
	private static final int RED_SHIFT = 16;
	private static final int GREEN_SHIFT = 8;
	private static final int BLUE_SHIFT = 0;

	/** Pack 0-255 r,g,b into 0xAARRGGBB with alpha=255. */
	public static int rgb(int r, int g, int b) {
		return 0xFF << ALPHA_SHIFT | (r & 0xFF) << RED_SHIFT
			| (g & 0xFF) << GREEN_SHIFT | (b & 0xFF) << BLUE_SHIFT;
	}

	/** Pack 0-255 r,g,b,a into 0xAARRGGBB. */
	public static int rgba(int r, int g, int b, int a) {
		return (a & 0xFF) << ALPHA_SHIFT | (r & 0xFF) << RED_SHIFT
			| (g & 0xFF) << GREEN_SHIFT | (b & 0xFF) << BLUE_SHIFT;
	}

	public static int r(int color) {
		return (color >> RED_SHIFT) & 0xFF;
	}
	public static int g(int color) {
		return (color >> GREEN_SHIFT) & 0xFF;
	}
	public static int b(int color) {
		return (color >> BLUE_SHIFT) & 0xFF;
	}
	public static int a(int color) {
		return (color >> ALPHA_SHIFT) & 0xFF;
	}

	public static void setRenderDrawColor(SDL_Renderer renderer, int color) {
		SDL_SetRenderDrawColor(
			renderer, (byte)r(color), (byte)g(color), (byte)b(color),
			(byte)a(color)
		);
	}
}

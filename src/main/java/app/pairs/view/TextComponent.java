package app.pairs.view;

import app.pairs.asset.AssetManager;
import app.pairs.asset.BitmapFont;

import io.github.libsdl4j.api.render.*;

/**
 * A leaf {@link ViewComponent} that renders a line of text using a
 * {@link BitmapFont}.
 */
public class TextComponent extends Widget {
	private final BitmapFont font;
	private String text;
	private int size;
	private int color;
	private final int[] measuredSize = new int[2];

	public TextComponent(BitmapFont font, String text, int size, int color) {
		this.font = font;
		this.text = text;
		this.size = size;
		this.color = color;
	}

	/**
	 * Convenience constructor that retrieves the project's default font
	 * ({@code "monogram/font"}) from the {@link AssetManager} singleton.
	 */
	public TextComponent(String text, int size, int color) {
		this(AssetManager.instance().get("monogram/font"), text, size, color);
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public String text() {
		return text;
	}

	@Override
	public int[] measure() {
		int w = BitmapFontRenderer.measureText(font, text, size);
		measuredSize[1] = BitmapFont.GLYPH_HEIGHT * size;
		if (w < 0) {
			w = 80 * size;
		}
		measuredSize[0] = Math.max(0, w);
		return measuredSize;
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int globalX = parentX + layoutX;
		int globalY = parentY + layoutY;
		BitmapFontRenderer.renderText(
			renderer, font, text, globalX, globalY, size, scale, color
		);
	}
}

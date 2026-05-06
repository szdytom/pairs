package app.pairs.view;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;

import io.github.libsdl4j.api.render.SDL_Texture;

/**
 * A single entry in the operation history log.
 * Composed of a tile {@link ImageComponent} followed by a coordinate
 * {@link TextComponent}, arranged inside a {@link FlexLayout} with 1 px
 * padding around the whole entry.
 */
public class OpLogEntry extends Container {
	private static final int PADDING = 1;
	private static final int GAP = 2;
	private static final int FONT_SIZE = 1;
	private static final int R = 180;
	private static final int G = 180;
	private static final int B = 180;

	private final TextComponent text;
	private final FlexLayout inner;
	private final int[] measuredSize = new int[2];

	public OpLogEntry(int tileId, int r1, int c1, int r2, int c2) {
		String label = "(" + r1 + " " + c1 + ")→(" + r2 + " " + c2 + ")";

		TileRegistry reg = AssetManager.instance().get("tiles/typed");
		SDL_Texture tex = reg.getTexture(tileId);

		inner = new FlexLayout(FlexLayout.Direction.ROW, GAP);
		inner.addChild(new ImageComponent(tex));
		this.text = new TextComponent(label, FONT_SIZE, R, G, B);
		inner.addChild(text);
		addChild(inner);
	}

	@Override
	public int[] measure() {
		int[] innerSize = inner.measure();
		measuredSize[0] = innerSize[0] + PADDING * 2;
		measuredSize[1] = innerSize[1] + PADDING * 2;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		inner.layout(PADDING, PADDING, w - PADDING * 2, h - PADDING * 2);
		// Shift the text baseline down so it sits better against the 16 px
		// icon.
		text.layoutY += 2;
	}
}

package app.pairs.view;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;

import io.github.libsdl4j.api.render.SDL_Texture;

/**
 * A single entry in the operation history log.
 * Composed of a tile {@link ImageComponent} followed by a coordinate
 * {@link TextComponent}, arranged in a {@link FlexLayout} row with the
 * text centered vertically against the tile icon.
 */
public class OpLogEntry extends FlexLayout {
	private static final int GAP = 2;
	private static final int PADDING = 1;
	private static final int FONT_SIZE = 1;
	private static final int R = 180;
	private static final int G = 180;
	private static final int B = 180;

	public OpLogEntry(int tileId, int r1, int c1, int r2, int c2) {
		super(Direction.ROW, GAP, PADDING);

		TileRegistry reg = AssetManager.instance().get("tiles/typed");
		SDL_Texture tex = reg.getTexture(tileId);

		addChild(
			new ImageComponent(tex, reg.getTileWidth() - 2, reg.getTileHeight())
		);

		AlignLayout align = new AlignLayout();
		align.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(new TextComponent(
			"(" + r1 + " " + c1 + ")→(" + r2 + " " + c2 + ")", FONT_SIZE, R, G,
			B
		));
		addChild(align);
	}
}

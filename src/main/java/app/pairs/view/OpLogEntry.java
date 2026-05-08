package app.pairs.view;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.*;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.surface.SdlSurface.*;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;

import java.util.List;

import io.github.libsdl4j.api.render.*;
import io.github.libsdl4j.api.surface.*;

/**
 * A single entry in the operation history log.
 * Composed of a tile {@link ImageComponent} followed by a path thumbnail
 * {@link ImageComponent}, arranged in a {@link FlexLayout} row.
 */
public class OpLogEntry extends FlexLayout {
	private static final int GAP = 2;
	private static final int PADDING = 1;

	private static final byte PATH_R = (byte)60;
	private static final byte PATH_G = (byte)140;
	private static final byte PATH_B = (byte)240;

	private static final byte ENDPOINT_R = (byte)240;
	private static final byte ENDPOINT_G = (byte)200;
	private static final byte ENDPOINT_B = (byte)40;

	private SDL_Texture thumbnail;

	public OpLogEntry(
		int index, int tileId, List<Integer> path, int mapRows, int mapCols,
		int timeMs
	) {
		super(Direction.ROW, GAP, PADDING);

		AlignLayout numWrap = new AlignLayout();
		numWrap.setProp("v-align", AlignLayout.VAlign.CENTER);
		numWrap.addChild(
			new TextComponent(String.format("%3d", index), 1, 160, 160, 160)
		);
		addChild(numWrap);

		TileRegistry reg = AssetManager.instance().get("tiles/typed");
		SDL_Texture tex = reg.getTexture(tileId);
		addChild(
			new ImageComponent(tex, reg.getTileWidth() - 2, reg.getTileHeight())
		);

		thumbnail = createThumbnail(path, mapRows, mapCols);
		int maxD = Math.max(mapRows, mapCols);
		int s = (maxD + 15) / 16;

		AlignLayout align = new AlignLayout();
		align.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(new ImageComponent(
			thumbnail, (mapCols + s - 1) / s + 2, (mapRows + s - 1) / s + 2
		));
		addChild(align);

		AlignLayout timeWrap = new AlignLayout();
		timeWrap.setProp("v-align", AlignLayout.VAlign.CENTER);
		timeWrap.setProp("h-align", AlignLayout.HAlign.RIGHT);
		timeWrap.addChild(
			new TextComponent(formatTime(timeMs), 1, 60, 60, 255)
		);
		addChild(timeWrap);
	}

	private static String formatTime(int timeMs) {
		int sec = timeMs / 1_000;
		int cs = (timeMs % 1_000) / 10;
		return String.format("+%02d.%02ds", sec, cs);
	}

	private SDL_Texture createThumbnail(
		List<Integer> path, int mapRows, int mapCols
	) {
		int maxDim = Math.max(mapRows, mapCols);
		int scale = (maxDim + 15) / 16;
		int w = (mapCols + scale - 1) / scale;
		int h = (mapRows + scale - 1) / scale;
		// +1 for ImageComponent srcRect.x offset, +2 for 1px outside-board
		// border
		int texW = w + 3;
		int texH = h + 2;

		SDL_Surface surface = SDL_CreateRGBSurfaceWithFormat(
			0, texW, texH, 32, SDL_PIXELFORMAT_ABGR8888
		);
		byte[] data = new byte[texW * texH * 4];

		for (int py = 1; py <= h; py++) {
			int rowOff = py * texW * 4;
			for (int px = 2; px <= w + 1; px++) {
				data[rowOff + px * 4 + 3] = 20;
			}
		}

		for (int i = 0; i < path.size() - 2; i += 2) {
			int rA = path.get(i), cA = path.get(i + 1);
			int rB = path.get(i + 2), cB = path.get(i + 3);
			if (rA == rB) {
				int l = Math.min(cA, cB), r = Math.max(cA, cB);
				for (int c = l; c <= r; c++) {
					drawCell(
						data, texW, texH, scale, rA, c, PATH_R, PATH_G, PATH_B,
						mapRows, mapCols
					);
				}
			} else {
				int t = Math.min(rA, rB), b = Math.max(rA, rB);
				for (int r = t; r <= b; r++) {
					drawCell(
						data, texW, texH, scale, r, cA, PATH_R, PATH_G, PATH_B,
						mapRows, mapCols
					);
				}
			}
		}

		int r1 = path.get(0), c1 = path.get(1);
		int r2 = path.get(path.size() - 2), c2 = path.get(path.size() - 1);
		drawCell(
			data, texW, texH, scale, r1, c1, ENDPOINT_R, ENDPOINT_G, ENDPOINT_B,
			mapRows, mapCols
		);
		drawCell(
			data, texW, texH, scale, r2, c2, ENDPOINT_R, ENDPOINT_G, ENDPOINT_B,
			mapRows, mapCols
		);

		surface.getPixels().write(0, data, 0, data.length);

		SDL_Renderer renderer = AssetManager.instance().renderer();
		SDL_Texture texture = SDL_CreateTextureFromSurface(renderer, surface);
		SDL_FreeSurface(surface);
		return texture;
	}

	private static void drawCell(
		byte[] data, int stride, int texH, int scale, int r, int c, byte rCol,
		byte gCol, byte bCol, int mapRows, int mapCols
	) {
		if (r < -1 || r > mapRows || c < -1 || c > mapCols) {
			return;
		}
		int px = Math.floorDiv(c, scale) + 2;
		int py = Math.floorDiv(r, scale) + 1;
		if (px < 1 || px >= stride || py < 0 || py >= texH) {
			return;
		}
		int idx = (py * stride + px) * 4;
		data[idx] = bCol;
		data[idx + 1] = gCol;
		data[idx + 2] = rCol;
		data[idx + 3] = (byte)255;
	}

	@Override
	public void destroy() {
		if (thumbnail != null) {
			SDL_DestroyTexture(thumbnail);
			thumbnail = null;
		}
		super.destroy();
	}
}

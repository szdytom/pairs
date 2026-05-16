package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.*;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.surface.SdlSurface.*;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;

import java.util.List;

import io.github.libsdl4j.api.rect.*;
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

	private static final int PATH_COLOR = rgb(60, 140, 240);
	private static final int ENDPOINT_COLOR = rgb(240, 200, 40);

	private SDL_Texture thumbnail;

	public OpLogEntry(
		int index, int tileId, List<Integer> path, int mapRows, int mapCols,
		int timeMs
	) {
		super(Direction.ROW, GAP, PADDING);

		TextComponent numText = new TextComponent(
			String.format("%2d", index), 1, rgb(160, 160, 160)
		);
		numText.setProp("v-align", AlignLayout.VAlign.CENTER);
		AlignLayout numWrap = new AlignLayout();
		numWrap.addChild(numText);
		addChild(numWrap);

		TileRegistry reg = AssetManager.instance().get("tiles/typed");
		SDL_Texture tex = reg.getTexture(tileId);
		SDL_Rect iconSrc = new SDL_Rect();
		iconSrc.x = 1;
		iconSrc.y = 0;
		iconSrc.w = 16;
		iconSrc.h = 18;
		addChild(new ImageComponent(tex, iconSrc));

		thumbnail = createThumbnail(path, mapRows, mapCols);
		int maxD = Math.max(mapRows, mapCols);
		int s = (maxD + 15) / 16;

		ImageComponent thumbImg = new ImageComponent(
			thumbnail, (mapCols + s - 1) / s + 2, (mapRows + s - 1) / s + 2
		);
		thumbImg.setProp("v-align", AlignLayout.VAlign.CENTER);
		AlignLayout align = new AlignLayout();
		align.addChild(thumbImg);
		addChild(align);

		TextComponent timeText = new TextComponent(
			formatTime(timeMs), 1, rgb(20, 120, 150)
		);
		timeText.setProp("v-align", AlignLayout.VAlign.CENTER);
		AlignLayout timeWrap = new AlignLayout();
		timeWrap.addChild(timeText);
		addChild(timeWrap);
	}

	private static String formatTime(int timeMs) {
		int sec = timeMs / 1_000;
		int cs = (timeMs % 1_000) / 10;
		return String.format("%02d.%02ds", sec, cs);
	}

	private SDL_Texture createThumbnail(
		List<Integer> path, int mapRows, int mapCols
	) {
		int maxDim = Math.max(mapRows, mapCols);
		int scale = (maxDim + 15) / 16;
		int w = (mapCols + scale - 1) / scale;
		int h = (mapRows + scale - 1) / scale;
		// +2 for 1px outside-board border on each side
		int texW = w + 2;
		int texH = h + 2;

		SDL_Surface surface = SDL_CreateRGBSurfaceWithFormat(
			0, texW, texH, 32, SDL_PIXELFORMAT_ABGR8888
		);
		byte[] data = new byte[texW * texH * 4];

		for (int py = 1; py <= h; py++) {
			int rowOff = py * texW * 4;
			for (int px = 1; px <= w; px++) {
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
						data, texW, texH, scale, rA, c, PATH_COLOR, mapRows,
						mapCols
					);
				}
			} else {
				int t = Math.min(rA, rB), b = Math.max(rA, rB);
				for (int r = t; r <= b; r++) {
					drawCell(
						data, texW, texH, scale, r, cA, PATH_COLOR, mapRows,
						mapCols
					);
				}
			}
		}

		int r1 = path.get(0), c1 = path.get(1);
		int r2 = path.get(path.size() - 2), c2 = path.get(path.size() - 1);
		drawCell(
			data, texW, texH, scale, r1, c1, ENDPOINT_COLOR, mapRows, mapCols
		);
		drawCell(
			data, texW, texH, scale, r2, c2, ENDPOINT_COLOR, mapRows, mapCols
		);

		surface.getPixels().write(0, data, 0, data.length);

		SDL_Renderer renderer = AssetManager.instance().renderer();
		SDL_Texture texture = SDL_CreateTextureFromSurface(renderer, surface);
		SDL_FreeSurface(surface);
		return texture;
	}

	private static void drawCell(
		byte[] data, int stride, int texH, int scale, int r, int c, int color,
		int mapRows, int mapCols
	) {
		if (r < -1 || r > mapRows || c < -1 || c > mapCols) {
			return;
		}
		int px = Math.floorDiv(c, scale) + 1;
		int py = Math.floorDiv(r, scale) + 1;
		if (px < 0 || px >= stride || py < 0 || py >= texH) {
			return;
		}
		int idx = (py * stride + px) * 4;
		data[idx] = (byte)b(color);
		data[idx + 1] = (byte)g(color);
		data[idx + 2] = (byte)r(color);
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

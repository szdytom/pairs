package app.pairs.view;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;

import io.github.libsdl4j.api.rect.*;
import io.github.libsdl4j.api.render.*;

/**
 * Renders a 2D grid of tile types using isometric projection.
 * The grid uses int tile IDs; values {@code <= 0} represent an empty cell.
 */
public class IsometricGridView extends Widget {
	private static final int TILE_CONTENT_WIDTH = 16;
	private static final int TILE_CONTENT_HEIGHT = 16;
	private static final int SHADOW_SIZE = 18;
	private static final int SHADOW_Y_OFFSET = TILE_CONTENT_HEIGHT;
	private static final int HOVER_LIFT = TILE_CONTENT_HEIGHT / 3;
	private static final int HOVER_LIFT_MS = 50;

	private final SDL_Rect srcRect = new SDL_Rect();
	private final SDL_Rect dstRect = new SDL_Rect();
	private final SDL_Rect shadowDstRect = new SDL_Rect();
	private final SDL_Texture shadowTex;
	private final SDL_Rect shadowSrcRect = new SDL_Rect();

	private int[][] grid;
	private final TileRegistry tileRegistry;
	private final TileRegistry hlTileRegistry;
	private final IsometricMapper mapper;
	private boolean[][] highlighted;
	private float[][] liftProgress;
	private int[][] depthOrder;
	private final int[] measuredSize = new int[2];

	public IsometricGridView(int[][] grid, IsometricMapper mapper) {
		this.grid = grid;
		this.mapper = mapper;
		this.tileRegistry = AssetManager.instance().get("tiles/typed");
		this.hlTileRegistry = AssetManager.instance().get("hl-tiles/typed");
		this.shadowTex = AssetManager.instance().get("shadow/texture");
		this.depthOrder = mapper.getDepthSortedOrder(
			grid.length, grid[0].length
		);
		this.liftProgress = new float[grid.length][grid[0].length];
		srcRect.x = 1;
		srcRect.y = 1;
		srcRect.w = TILE_CONTENT_WIDTH;
		srcRect.h = TILE_CONTENT_HEIGHT;
		shadowSrcRect.x = 0;
		shadowSrcRect.y = 0;
		shadowSrcRect.w = SHADOW_SIZE;
		shadowSrcRect.h = SHADOW_SIZE;
	}

	public void setGrid(int[][] grid) {
		this.grid = grid;
		this.depthOrder = mapper.getDepthSortedOrder(
			grid.length, grid[0].length
		);
		this.liftProgress = new float[grid.length][grid[0].length];
	}

	public void setHighlighted(boolean[][] highlighted) {
		this.highlighted = highlighted;
	}

	@Override
	public void update(long deltaTimeMs) {
		float step = deltaTimeMs / (float)HOVER_LIFT_MS;
		for (int r = 0; r < liftProgress.length; r++) {
			for (int c = 0; c < liftProgress[r].length; c++) {
				boolean target = highlighted != null && highlighted[r][c];
				if (target) {
					liftProgress[r][c] = Math.min(
						1f, liftProgress[r][c] + step
					);
				} else {
					liftProgress[r][c] = Math.max(
						0f, liftProgress[r][c] - step
					);
				}
			}
		}
	}

	@Override
	public int[] measure() {
		int rows = grid.length;
		int cols = grid[0].length;
		int stepX = mapper.getTileWidth() / 2;
		int stepY = mapper.getTileWidth() / 4;
		measuredSize[0] = (cols - 1 + rows - 1) * stepX + TILE_CONTENT_WIDTH;
		measuredSize[1] = (rows - 1 + cols - 1) * stepY
			+ TILE_CONTENT_HEIGHT / 2 + HOVER_LIFT + SHADOW_SIZE / 2
			+ SHADOW_Y_OFFSET;
		return measuredSize;
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		tileRegistry.createTextures(renderer);
		hlTileRegistry.createTextures(renderer);
		int dstW = TILE_CONTENT_WIDTH * scale;
		int dstH = TILE_CONTENT_HEIGHT * scale;

		int gridGlobalX = parentX + layoutX;
		int gridGlobalY = parentY + layoutY + HOVER_LIFT;

		for (int[] pos : depthOrder) {
			int row = pos[0];
			int col = pos[1];
			int typeId = grid[row][col];

			if (typeId <= 0)
				continue;

			// Global logical → screen: screen = globalLogical * scale
			IsometricMapper.IsometricCoordinate logical = mapper.gridToLogical(
				row, col
			);
			int screenCenterX = (gridGlobalX + logical.x) * scale;
			int screenCenterY = (gridGlobalY + logical.y) * scale;

			// Render shadow below the tile
			boolean isHighlighted = highlighted != null
				&& highlighted[row][col];
			int shadowDstW = SHADOW_SIZE * scale;
			int shadowDstH = SHADOW_SIZE * scale;
			int shadowX = screenCenterX - shadowDstW / 2;
			int shadowY = screenCenterY - shadowDstH / 2
				+ SHADOW_Y_OFFSET * scale;
			shadowDstRect.x = shadowX;
			shadowDstRect.y = shadowY;
			shadowDstRect.w = shadowDstW;
			shadowDstRect.h = shadowDstH;
			SdlRender.SDL_RenderCopy(
				renderer, shadowTex, shadowSrcRect, shadowDstRect
			);

			SDL_Texture tex = isHighlighted
				? hlTileRegistry.getTexture(typeId)
				: tileRegistry.getTexture(typeId);
			if (tex != null) {
				dstRect.x = screenCenterX - dstW / 2;
				int liftPixels = Math.round(
					HOVER_LIFT * liftProgress[row][col]
				);
				dstRect.y = screenCenterY - dstH / 2 - liftPixels * scale;
				dstRect.w = dstW;
				dstRect.h = dstH;

				SdlRender.SDL_RenderCopy(renderer, tex, srcRect, dstRect);
			}
		}
	}
}

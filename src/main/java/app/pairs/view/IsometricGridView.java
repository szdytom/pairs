package app.pairs.view;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.logic.GameState;

import io.github.libsdl4j.api.rect.*;
import io.github.libsdl4j.api.render.*;

public class IsometricGridView extends Widget {
	private static final int TILE_WIDTH = 16;
	private static final int TILE_HEIGHT = 16;
	private static final int TILE_SPRITE_HEIGHT = 18;
	private static final int SHADOW_SIZE = 18;
	private static final int SHADOW_Y_OFFSET = TILE_HEIGHT;
	private static final int HOVER_LIFT = TILE_HEIGHT / 3;
	private static final int HOVER_LIFT_MS = 50;

	private final SDL_Rect srcRect = new SDL_Rect();
	private final SDL_Rect dstRect = new SDL_Rect();
	private final SDL_Rect shadowDstRect = new SDL_Rect();
	private final SDL_Texture shadowTex;
	private final SDL_Rect shadowSrcRect = new SDL_Rect();

	private final IsometricMapper mapper = new IsometricMapper(
		TILE_WIDTH, TILE_HEIGHT
	);
	private int gridWidth;
	private int gridHeight;
	private final TileRegistry tileRegistry;
	private final TileRegistry hlTileRegistry;
	private boolean[][] highlighted;
	private float[][] liftProgress;
	private int[][] depthOrder;
	private final int[] measuredSize = new int[2];
	private int originOffsetX;
	private int originOffsetY;

	public IsometricGridView(int gridWidth, int gridHeight) {
		this.gridWidth = gridWidth;
		this.gridHeight = gridHeight;
		this.tileRegistry = AssetManager.instance().get("tiles/typed");
		this.hlTileRegistry = AssetManager.instance().get("hl-tiles/typed");
		this.shadowTex = AssetManager.instance().get("shadow/texture");
		this.depthOrder = mapper.getDepthSortedOrder(gridHeight, gridWidth);
		this.liftProgress = new float[gridHeight][gridWidth];
		srcRect.x = 1;
		srcRect.y = 0;
		srcRect.w = TILE_WIDTH;
		srcRect.h = TILE_SPRITE_HEIGHT;
		shadowSrcRect.x = 0;
		shadowSrcRect.y = 0;
		shadowSrcRect.w = SHADOW_SIZE;
		shadowSrcRect.h = SHADOW_SIZE;
	}

	public void setGridSize(int width, int height) {
		this.gridWidth = width;
		this.gridHeight = height;
		this.depthOrder = mapper.getDepthSortedOrder(height, width);
		this.highlighted = new boolean[height][width];
		reset();
	}

	public void reset() {
		this.liftProgress = new float[gridHeight][gridWidth];
	}

	public void setHighlighted(boolean[][] highlighted) {
		this.highlighted = highlighted;
	}

	public void cellAt(
		int originX, int originY, int mouseX, int mouseY, int prevRow,
		int prevCol, int[] result
	) {
		result[0] = -1;
		result[1] = -1;

		if (mouseX < 0 || mouseY < 0) {
			return;
		}

		int halfW = TILE_WIDTH / 2;
		int halfH = TILE_HEIGHT / 2;
		int hoverOffset = TILE_HEIGHT / 3;
		int gx = originX + originOffsetX;
		int gy = originY + originOffsetY + HOVER_LIFT;

		GameState gameState = blackboard().get(GameState.class);

		for (int i = depthOrder.length - 1; i >= 0; i--) {
			int[] pos = depthOrder[i];
			int r = pos[0];
			int c = pos[1];

			if (gameState.getTile(r, c) <= 0)
				continue;

			int cx = gx + (c - r) * TILE_WIDTH / 2;
			int cy = gy + (c + r) * TILE_WIDTH / 4;

			int left = cx - halfW;
			int right = cx + halfW;
			int top = cy - halfH;
			int bottom = cy + halfH;

			if (mouseX >= left && mouseX < right && mouseY >= top
			    && mouseY < bottom) {
				result[0] = r;
				result[1] = c;
				return;
			}

			if (r == prevRow && c == prevCol) {
				int raisedTop = top - hoverOffset;
				int raisedBottom = bottom - hoverOffset;
				if (mouseX >= left && mouseX < right && mouseY >= raisedTop
				    && mouseY < raisedBottom) {
					result[0] = r;
					result[1] = c;
					return;
				}
			}
		}
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
		int stepX = mapper.getTileWidth() / 2;
		int stepY = mapper.getTileWidth() / 4;
		measuredSize[0] = (gridWidth - 1 + gridHeight - 1) * stepX + TILE_WIDTH;
		measuredSize[1] = (gridHeight - 1 + gridWidth - 1) * stepY
			+ TILE_HEIGHT / 2 + HOVER_LIFT + SHADOW_SIZE / 2 + SHADOW_Y_OFFSET;
		originOffsetX = (gridHeight - 1) * stepX + TILE_WIDTH / 2;
		originOffsetY = TILE_HEIGHT / 2;
		return measuredSize;
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		GameState gameState = blackboard().get(GameState.class);
		tileRegistry.createTextures(renderer);
		hlTileRegistry.createTextures(renderer);
		int dstW = TILE_WIDTH * scale;
		int dstH = TILE_SPRITE_HEIGHT * scale;

		int gridGlobalX = parentX + layoutX + originOffsetX;
		int gridGlobalY = parentY + layoutY + originOffsetY + HOVER_LIFT;

		for (int[] pos : depthOrder) {
			int row = pos[0];
			int col = pos[1];
			int typeId = gameState.getTile(row, col);

			if (typeId <= 0)
				continue;

			IsometricMapper.IsometricCoordinate logical = mapper.gridToLogical(
				row, col
			);
			int screenCenterX = (gridGlobalX + logical.x) * scale;
			int screenCenterY = (gridGlobalY + logical.y) * scale;

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

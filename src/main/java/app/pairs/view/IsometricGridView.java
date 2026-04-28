package app.pairs.view;

import app.pairs.asset.TileRegistry;

import java.util.HashMap;
import java.util.Map;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.render.SdlRender;
import io.github.libsdl4j.api.surface.SDL_Surface;

/**
 * Renders a 2D grid of tile types using isometric projection.
 * The grid uses string tile IDs; null represents an empty cell.
 */
public class IsometricGridView implements ViewComponent {
	private static final int TILE_CONTENT_WIDTH = 16;
	private static final int TILE_CONTENT_HEIGHT = 16;
	private static final SDL_Rect SRC_RECT = new SDL_Rect();
	private final SDL_Rect dstRect = new SDL_Rect();

	static {
		SRC_RECT.x = 1;
		SRC_RECT.y = 1;
		SRC_RECT.w = TILE_CONTENT_WIDTH;
		SRC_RECT.h = TILE_CONTENT_HEIGHT;
	}

	private String[][] grid;
	private final TileRegistry tileRegistry;
	private final IsometricMapper mapper;
	private Map<String, SDL_Texture> typeTextures;
	private Map<String, SDL_Texture> hlTypeTextures;
	private final TileRegistry hlTileRegistry;
	private boolean texturesInitialized;
	private boolean[][] highlighted;

	public IsometricGridView(
		String[][] grid, TileRegistry tileRegistry, TileRegistry hlTileRegistry,
		IsometricMapper mapper
	) {
		this.grid = grid;
		this.tileRegistry = tileRegistry;
		this.hlTileRegistry = hlTileRegistry;
		this.mapper = mapper;
		this.texturesInitialized = false;
	}

	public void setGrid(String[][] grid) {
		this.grid = grid;
	}

	public void setHighlighted(boolean[][] highlighted) {
		this.highlighted = highlighted;
	}

	@Override
	public void update(long deltaTimeMs) {
		// No dynamic state to update
	}

	@Override
	public void render(SDL_Renderer renderer, int scale) {
		if (!texturesInitialized) {
			initializeTextures(renderer);
		}

		int rows = grid.length;
		int cols = grid[0].length;

		int[][] depthOrder = mapper.getDepthSortedOrder(rows, cols);
		int dstW = TILE_CONTENT_WIDTH * scale;
		int dstH = TILE_CONTENT_HEIGHT * scale;

		int hoverOffset = TILE_CONTENT_HEIGHT * scale / 3;
		int origX = mapper.getOriginX();
		int origY = mapper.getOriginY();

		for (int[] pos : depthOrder) {
			int row = pos[0];
			int col = pos[1];
			String typeId = grid[row][col];

			if (typeId == null || typeId.isEmpty())
				continue;

			// Logical → screen:  screen = origin + (logical - origin) * scale
			IsometricMapper.IsometricCoordinate logical = mapper.gridToLogical(
				row, col
			);
			int screenCenterX = origX + (logical.x - origX) * scale;
			int screenCenterY = origY + (logical.y - origY) * scale;

			boolean isHighlighted = highlighted != null
				&& highlighted[row][col];
			SDL_Texture tex = isHighlighted
					&& hlTypeTextures.containsKey(typeId)
				? hlTypeTextures.get(typeId)
				: typeTextures.get(typeId);
			if (tex != null) {
				dstRect.x = screenCenterX - dstW / 2;
				dstRect.y = screenCenterY - dstH / 2
					- (isHighlighted ? hoverOffset : 0);
				dstRect.w = dstW;
				dstRect.h = dstH;

				SdlRender.SDL_RenderCopy(renderer, tex, SRC_RECT, dstRect);
			}
		}
	}

	private void initializeTextures(SDL_Renderer renderer) {
		typeTextures = new HashMap<>();
		hlTypeTextures = new HashMap<>();

		for (int row = 0; row < tileRegistry.getRows(); row++) {
			for (int col = 0; col < tileRegistry.getColumns(); col++) {
				String typeId = tileRegistry.getTypeId(row, col);
				if (typeId == null)
					continue;
				if (!typeTextures.containsKey(typeId)) {
					SDL_Surface surface = tileRegistry.getTile(row, col);
					if (surface != null) {
						typeTextures.put(
							typeId,
							SdlRender.SDL_CreateTextureFromSurface(
								renderer, surface
							)
						);
					}
				}
				if (hlTileRegistry != null
				    && !hlTypeTextures.containsKey(typeId)) {
					SDL_Surface hlSurface = hlTileRegistry.getTile(row, col);
					if (hlSurface != null) {
						hlTypeTextures.put(
							typeId,
							SdlRender.SDL_CreateTextureFromSurface(
								renderer, hlSurface
							)
						);
					}
				}
			}
		}
		texturesInitialized = true;
	}

	@Override
	public void destroy() {
		if (typeTextures != null) {
			for (SDL_Texture tex : typeTextures.values()) {
				SdlRender.SDL_DestroyTexture(tex);
			}
			typeTextures.clear();
		}
		if (hlTypeTextures != null) {
			for (SDL_Texture tex : hlTypeTextures.values()) {
				SdlRender.SDL_DestroyTexture(tex);
			}
			hlTypeTextures.clear();
		}
	}
}

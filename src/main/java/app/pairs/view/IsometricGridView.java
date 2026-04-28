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
	private boolean texturesInitialized;
	private int mouseX = -1;
	private int mouseY = -1;
	private int hoveredRow = -1;
	private int hoveredCol = -1;
	private int prevHoveredRow = -1;
	private int prevHoveredCol = -1;

	public IsometricGridView(
		String[][] grid, TileRegistry tileRegistry, IsometricMapper mapper
	) {
		this.grid = grid;
		this.tileRegistry = tileRegistry;
		this.mapper = mapper;
		this.texturesInitialized = false;
	}

	public void setGrid(String[][] grid) {
		this.grid = grid;
	}

	public void setMousePosition(int x, int y) {
		this.mouseX = x;
		this.mouseY = y;
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

		resolveHoveredCell(mouseX, mouseY, depthOrder, scale);
		prevHoveredRow = hoveredRow;
		prevHoveredCol = hoveredCol;

		int hoverOffset = TILE_CONTENT_HEIGHT * scale / 3;

		for (int[] pos : depthOrder) {
			int row = pos[0];
			int col = pos[1];
			String typeId = grid[row][col];

			if (typeId == null || typeId.isEmpty())
				continue;

			IsometricMapper.IsometricCoordinate screenPos = mapper.gridToScreen(
				row, col, scale
			);

			SDL_Texture tex = typeTextures.get(typeId);
			if (tex != null) {
				dstRect.x = screenPos.x - dstW / 2;
				dstRect.y = screenPos.y - dstH / 2
					- ((row == hoveredRow && col == hoveredCol) ? hoverOffset
				                                                : 0);
				dstRect.w = dstW;
				dstRect.h = dstH;

				SdlRender.SDL_RenderCopy(renderer, tex, SRC_RECT, dstRect);
			}
		}
	}

	/**
	 * Resolves which grid cell is under the mouse cursor by testing tiles
	 * front-to-back (reverse depth order) against their sprite rectangles.
	 *
	 * <p>For the previously hovered tile, also tests the raised position so
	 * the hover doesn't glitch when the tile lifts.</p>
	 */
	private void resolveHoveredCell(
		int mouseX, int mouseY, int[][] depthOrder, int scale
	) {
		if (mouseX < 0 || mouseY < 0) {
			hoveredRow = -1;
			hoveredCol = -1;
			return;
		}

		int dstW = TILE_CONTENT_WIDTH * scale;
		int dstH = TILE_CONTENT_HEIGHT * scale;
		int hoverOffset = TILE_CONTENT_HEIGHT * scale / 3;

		for (int i = depthOrder.length - 1; i >= 0; i--) {
			int[] pos = depthOrder[i];
			int r = pos[0];
			int c = pos[1];
			String typeId = grid[r][c];
			if (typeId == null || typeId.isEmpty())
				continue;

			IsometricMapper.IsometricCoordinate center = mapper.gridToScreen(
				r, c, scale
			);
			int left = center.x - dstW / 2;
			int right = center.x + dstW / 2;
			int top = center.y - dstH / 2;
			int bottom = center.y + dstH / 2;

			// Normal position
			if (mouseX >= left && mouseX < right && mouseY >= top
			    && mouseY < bottom) {
				hoveredRow = r;
				hoveredCol = c;
				return;
			}

			// Previously hovered tile: raised position with diamond check.
			// Previously hovered tile: also check raised position
			if (r == prevHoveredRow && c == prevHoveredCol) {
				int raisedTop = top - hoverOffset;
				int raisedBottom = bottom - hoverOffset;
				if (mouseX >= left && mouseX < right && mouseY >= raisedTop
				    && mouseY < raisedBottom) {
					hoveredRow = r;
					hoveredCol = c;
					return;
				}
			}
		}
		hoveredRow = -1;
		hoveredCol = -1;
	}

	private void initializeTextures(SDL_Renderer renderer) {
		typeTextures = new HashMap<>();

		for (int row = 0; row < tileRegistry.getRows(); row++) {
			for (int col = 0; col < tileRegistry.getColumns(); col++) {
				String typeId = tileRegistry.getTypeId(row, col);
				if (typeId == null)
					continue;
				if (typeTextures.containsKey(typeId))
					continue;

				SDL_Surface surface = tileRegistry.getTile(row, col);
				if (surface != null) {
					SDL_Texture tex = SdlRender.SDL_CreateTextureFromSurface(
						renderer, surface
					);
					typeTextures.put(typeId, tex);
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
	}
}

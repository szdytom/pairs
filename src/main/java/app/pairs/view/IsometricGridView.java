package app.pairs.view;

import app.pairs.asset.TileRegistry;

import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.render.SdlRender;
import io.github.libsdl4j.api.surface.SDL_Surface;
import io.github.libsdl4j.api.rect.SDL_Rect;

/**
 * Renders a 2D grid of tile types using isometric projection.
 */
public class IsometricGridView implements ViewComponent {
    private static final int TILE_CONTENT_WIDTH = 16;
    private static final int TILE_CONTENT_HEIGHT = 16;

    private int[][] grid;
    private final TileRegistry tileRegistry;
    private final IsometricMapper mapper;
    private final SDL_Renderer renderer;
    private SDL_Texture[] typeTextures;
    private boolean texturesInitialized;

    public IsometricGridView(int[][] grid, TileRegistry tileRegistry,
                             IsometricMapper mapper, SDL_Renderer renderer) {
        this.grid = grid;
        this.tileRegistry = tileRegistry;
        this.mapper = mapper;
        this.renderer = renderer;
        this.texturesInitialized = false;
    }

    public void setGrid(int[][] grid) {
        this.grid = grid;
    }

    @Override
    public void update(long deltaTimeMs) {
        // No dynamic state to update
    }

    @Override
    public void render(SDL_Renderer renderer, int scale) {
        if (!texturesInitialized) {
            initializeTextures();
        }

        int rows = grid.length;
        int cols = grid[0].length;

        int[][] renderOrder = mapper.getDepthSortedOrder(rows, cols);

        int dstW = TILE_CONTENT_WIDTH * scale;
        int dstH = TILE_CONTENT_HEIGHT * scale;

        for (int[] pos : renderOrder) {
            int row = pos[0];
            int col = pos[1];
            int typeId = grid[row][col];

            if (typeId == 0)
                continue;

            IsometricMapper.IsometricCoordinate screenPos = mapper.gridToScreen(row, col, scale);

            SDL_Texture tex = getTextureForType(typeId);
            if (tex != null) {
                SDL_Rect dst = new SDL_Rect();
                dst.x = screenPos.x - dstW / 2;
                dst.y = screenPos.y - dstH / 2;
                dst.w = dstW;
                dst.h = dstH;

                SDL_Rect src = new SDL_Rect();
                src.x = 1;
                src.y = 1;
                src.w = TILE_CONTENT_WIDTH;
                src.h = TILE_CONTENT_HEIGHT;

                SdlRender.SDL_RenderCopy(renderer, tex, src, dst);
            }
        }
    }

    private void initializeTextures() {
        int totalTypes = tileRegistry.getRows() * tileRegistry.getColumns();
        typeTextures = new SDL_Texture[totalTypes];

        for (int row = 0; row < tileRegistry.getRows(); row++) {
            for (int col = 0; col < tileRegistry.getColumns(); col++) {
                SDL_Surface surface = tileRegistry.getTile(row, col);
                if (surface != null) {
                    SDL_Texture tex = SdlRender.SDL_CreateTextureFromSurface(
                        renderer, surface);
                    int typeId = tileRegistry.getTypeForTile(row, col);
                    if (typeId >= 0 && typeId < typeTextures.length) {
                        if (typeTextures[typeId] == null) {
                            typeTextures[typeId] = tex;
                        }
                    }
                }
            }
        }
        texturesInitialized = true;
    }

    private SDL_Texture getTextureForType(int typeId) {
        if (typeId >= 0 && typeId < typeTextures.length) {
            return typeTextures[typeId];
        }
        return null;
    }
}

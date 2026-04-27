package app.pairs.asset;

import io.github.libsdl4j.api.surface.SDL_Surface;
import io.github.libsdl4j.api.surface.SdlSurface;
import io.github.libsdl4j.api.rect.SDL_Rect;

/**
 * Crops a sprite sheet into individual tiles.
 * Expects a grid layout: tiles are extracted row-major from top-left.
 */
public class CropTilesOperation implements AssetOperation {
    private String id;
    private String input;
    private int tileWidth;
    private int tileHeight;
    private int columns;
    private int rows;

    @Override
    public String type() {
        return "crop-tiles";
    }

    @Override
    public void process(Context ctx) throws Exception {
        System.out.println("  Cropping tiles: " + id);

        SDL_Surface source = ctx.getInput(input);

        TileRegistry registry = new TileRegistry(columns, rows, tileWidth, tileHeight);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int srcX = col * tileWidth;
                int srcY = row * tileHeight;

                SDL_Surface tileSurface = SdlSurface.SDL_CreateRGBSurface(
                    0, tileWidth, tileHeight, 32,
                    0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000
                );

                SDL_Rect srcRect = new SDL_Rect();
                srcRect.x = srcX;
                srcRect.y = srcY;
                srcRect.w = tileWidth;
                srcRect.h = tileHeight;

                SDL_Rect dstRect = new SDL_Rect();
                dstRect.x = 0;
                dstRect.y = 0;
                dstRect.w = tileWidth;
                dstRect.h = tileHeight;

                SdlSurface.SDL_BlitSurface(source, srcRect, tileSurface, dstRect);

                registry.setTile(row, col, tileSurface);
            }
        }

        System.out.println("  Cropped " + (columns * rows) + " tiles");
        ctx.put(id, registry);
    }

    public CropTilesOperation id(String id) {
        this.id = id;
        return this;
    }

    public CropTilesOperation input(String input) {
        this.input = input;
        return this;
    }

    public CropTilesOperation tileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
        return this;
    }

    public CropTilesOperation tileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
        return this;
    }

    public CropTilesOperation columns(int columns) {
        this.columns = columns;
        return this;
    }

    public CropTilesOperation rows(int rows) {
        this.rows = rows;
        return this;
    }
}
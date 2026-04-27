package app.pairs.asset;

import io.github.libsdl4j.api.surface.SDL_Surface;

/**
 * Stores cropped tiles in a 1D array with auto-assigned numeric IDs.
 * Also maintains a 2D tile-to-type mapping using string IDs (nullable).
 */
public class TileRegistry {
    private final int rows;
    private final int columns;
    private final int tileWidth;
    private final int tileHeight;
    private final SDL_Surface[] tiles;
    private String[][] typeMapping;

    public TileRegistry(int columns, int rows, int tileWidth, int tileHeight) {
        this.columns = columns;
        this.rows = rows;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tiles = new SDL_Surface[rows * columns];
    }

    public void setTile(int row, int col, SDL_Surface surface) {
        tiles[row * columns + col] = surface;
    }

    public SDL_Surface getTile(int row, int col) {
        return tiles[row * columns + col];
    }

    /**
     * Gets a tile by its auto-assigned numeric ID (index in the 1D array).
     */
    public SDL_Surface getTile(int id) {
        if (id >= 0 && id < tiles.length) {
            return tiles[id];
        }
        return null;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getTileCount() {
        return tiles.length;
    }

    /**
     * Sets a 2D type mapping. Each entry is a string tile ID or null (empty).
     * The dimensions should correspond to the original crop grid.
     */
    public void setTypeMapping(String[][] mapping) {
        this.typeMapping = mapping;
    }

    /**
     * Gets the string type ID for a tile at (row, col).
     * Returns null if no mapping exists or the position is empty.
     */
    public String getTypeId(int row, int col) {
        if (typeMapping == null)
            return null;
        if (row >= 0 && row < typeMapping.length &&
            col >= 0 && col < typeMapping[row].length) {
            return typeMapping[row][col];
        }
        return null;
    }
}

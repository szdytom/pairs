package app.pairs.asset;

import io.github.libsdl4j.api.surface.SDL_Surface;

/**
 * Stores cropped tiles indexed by (row, col) coordinates.
 * Also maintains tile-to-type mapping.
 */
public class TileRegistry {
    private final int rows;
    private final int columns;
    private final int tileWidth;
    private final int tileHeight;
    private final SDL_Surface[][] tiles;
    private int[] tileTypeMap;

    public TileRegistry(int columns, int rows, int tileWidth, int tileHeight) {
        this.columns = columns;
        this.rows = rows;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tiles = new SDL_Surface[rows][columns];
    }

    public void setTile(int row, int col, SDL_Surface surface) {
        tiles[row][col] = surface;
    }

    public SDL_Surface getTile(int row, int col) {
        return tiles[row][col];
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

    public void setTypeMapping(int[] mapping) {
        this.tileTypeMap = mapping;
    }

    /**
     * Gets the game type ID for a tile at (row, col).
     * Returns -1 if no mapping exists.
     */
    public int getTypeForTile(int row, int col) {
        if (tileTypeMap == null)
            return -1;
        int index = row * columns + col;
        if (index >= 0 && index < tileTypeMap.length) {
            return tileTypeMap[index];
        }
        return -1;
    }
}
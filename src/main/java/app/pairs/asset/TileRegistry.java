package app.pairs.asset;

import java.util.HashMap;
import java.util.Map;

import io.github.libsdl4j.api.render.*;
import io.github.libsdl4j.api.surface.*;

/**
 * Stores cropped tiles in a 1D array with auto-assigned numeric IDs and
 * bidirectional int↔String type-ID mapping.
 * <p>
 * Each tile position in the crop grid has a string type ID (set via
 * {@link #setTypeMapping}). Unique non-null string IDs are auto-assigned a
 * sequential numeric ID (starting at 1) that serves as the game's tile type
 * identifier. SDL_Textures can be created and retrieved by numeric ID.
 */
public class TileRegistry implements AutoCloseable {
	private final int rows;
	private final int columns;
	private final int tileWidth;
	private final int tileHeight;
	private final SDL_Surface[] tiles;
	private String[][] typeMapping;

	/** String tile ID → auto-assigned numeric ID. */
	private final Map<String, Integer> strToInt = new HashMap<>();
	/** Numeric ID → String tile ID. */
	private final Map<Integer, String> intToStr = new HashMap<>();
	/** String tile ID → cell position {row, col} in the crop grid. */
	private final Map<String, int[]> strToPos = new HashMap<>();
	/**
	 * Numeric ID → SDL_Texture (created lazily via {@link #createTextures}).
	 */
	private Map<Integer, SDL_Texture> textures;

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
	 * <p>
	 * Also builds the bidirectional int↔String ID mapping: each unique
	 * non-null string ID gets a sequential numeric ID starting at 1.
	 */
	public void setTypeMapping(String[][] mapping) {
		this.typeMapping = mapping;
		buildMappings();
	}

	/**
	 * Gets the string type ID for a tile at (row, col).
	 * Returns null if no mapping exists or the position is empty.
	 */
	public String getTypeId(int row, int col) {
		if (typeMapping == null)
			return null;
		if (row >= 0 && row < typeMapping.length && col >= 0
		    && col < typeMapping[row].length) {
			return typeMapping[row][col];
		}
		return null;
	}

	// ---- bidirectional int↔String mapping --------------------------------

	private void buildMappings() {
		strToInt.clear();
		intToStr.clear();
		strToPos.clear();
		int nextId = 1;
		for (int r = 0; r < typeMapping.length; r++) {
			for (int c = 0; c < typeMapping[r].length; c++) {
				String sid = typeMapping[r][c];
				if (sid != null && !strToInt.containsKey(sid)) {
					strToInt.put(sid, nextId);
					intToStr.put(nextId, sid);
					strToPos.put(sid, new int[] {r, c});
					nextId++;
				}
			}
		}
	}

	/** Returns the numeric ID for a given string tile ID, or -1 if unknown. */
	public int getNumericId(String stringId) {
		return strToInt.getOrDefault(stringId, -1);
	}

	/**
	 * Returns the string tile ID for a given numeric ID, or null if unknown.
	 */
	public String getStringId(int numericId) {
		return intToStr.get(numericId);
	}

	/** Returns the number of registered tile types. */
	public int getTypeCount() {
		return intToStr.size();
	}

	// ---- texture management ----------------------------------------------

	/**
	 * Creates SDL_Textures for every registered tile type from this registry's
	 * surfaces. Must be called with a valid renderer before {@link #getTexture}
	 * is used.
	 */
	public void createTextures(SDL_Renderer renderer) {
		disposeTextures();
		textures = new HashMap<>();
		for (var entry : strToPos.entrySet()) {
			String sid = entry.getKey();
			int[] pos = entry.getValue();
			SDL_Surface surface = tiles[pos[0] * columns + pos[1]];
			if (surface != null) {
				int numId = strToInt.get(sid);
				textures.put(
					numId,
					SdlRender.SDL_CreateTextureFromSurface(renderer, surface)
				);
			}
		}
	}

	/**
	 * Returns the SDL_Texture for the given numeric tile type ID, or null if
	 * {@link #createTextures} has not been called or the ID is unknown.
	 */
	public SDL_Texture getTexture(int typeId) {
		return textures != null ? textures.get(typeId) : null;
	}

	// ---- cleanup ---------------------------------------------------------

	/** Frees both textures and surfaces. */
	public void dispose() {
		disposeTextures();
		for (int i = 0; i < tiles.length; i++) {
			if (tiles[i] != null) {
				SdlSurface.SDL_FreeSurface(tiles[i]);
				tiles[i] = null;
			}
		}
	}

	@Override
	public void close() {
		dispose();
	}

	private void disposeTextures() {
		if (textures != null) {
			for (SDL_Texture tex : textures.values()) {
				SdlRender.SDL_DestroyTexture(tex);
			}
			textures.clear();
		}
	}
}

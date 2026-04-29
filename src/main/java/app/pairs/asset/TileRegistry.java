package app.pairs.asset;

import java.util.HashMap;
import java.util.Map;

import io.github.libsdl4j.api.render.*;
import io.github.libsdl4j.api.surface.*;

/**
 * Stores a flat list of tiles with a bidirectional int↔String type-ID mapping.
 * <p>
 * Tiles are passed in as parallel arrays of surfaces and their string IDs.
 * Each entry gets a sequential numeric ID (starting at 1) for use as the
 * game's tile type identifier. SDL_Textures can be created and retrieved
 * by numeric ID.
 */
public class TileRegistry implements AutoCloseable {
	private final int tileWidth;
	private final int tileHeight;
	private final SDL_Surface[] tiles;
	private final String[] intToStr;
	private final int typeCount;
	private final Map<String, Integer> strToInt;
	private SDL_Texture[] textures;

	public TileRegistry(
		int tileWidth, int tileHeight, SDL_Surface[] tiles, String[] stringIds
	) {
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.tiles = tiles;
		this.typeCount = tiles.length;
		this.intToStr = stringIds;
		this.strToInt = new HashMap<>(typeCount);
		for (int i = 0; i < typeCount; i++) {
			strToInt.put(stringIds[i], i + 1);
		}
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public int getTileHeight() {
		return tileHeight;
	}

	// ---- bidirectional int↔String mapping --------------------------------

	/** Returns the numeric ID for a given string tile ID, or -1 if unknown. */
	public int getNumericId(String stringId) {
		Integer id = strToInt.get(stringId);
		return id != null ? id : -1;
	}

	/**
	 * Returns the string tile ID for the given numeric ID (1-based),
	 * or null if unknown.
	 */
	public String getStringId(int numericId) {
		int idx = numericId - 1;
		return idx >= 0 && idx < typeCount ? intToStr[idx] : null;
	}

	/** Returns the number of registered tile types. */
	public int getTypeCount() {
		return typeCount;
	}

	// ---- texture management ----------------------------------------------

	/**
	 * Creates SDL_Textures for every registered tile type from this registry's
	 * surfaces. Must be called with a valid renderer before {@link #getTexture}
	 * is used.
	 */
	public void createTextures(SDL_Renderer renderer) {
		disposeTextures();
		if (typeCount == 0)
			return;
		textures = new SDL_Texture[typeCount];
		for (int i = 0; i < typeCount; i++) {
			if (tiles[i] != null) {
				textures[i] = SdlRender.SDL_CreateTextureFromSurface(
					renderer, tiles[i]
				);
			}
		}
	}

	/**
	 * Returns the SDL_Texture for the given numeric tile type ID (1-based),
	 * or null if {@link #createTextures} has not been called or the ID is
	 * unknown.
	 */
	public SDL_Texture getTexture(int typeId) {
		int idx = typeId - 1;
		return textures != null && idx >= 0 && idx < typeCount
			? textures[idx]
			: null;
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
			for (int i = 0; i < typeCount; i++) {
				if (textures[i] != null) {
					SdlRender.SDL_DestroyTexture(textures[i]);
				}
			}
			textures = null;
		}
	}
}

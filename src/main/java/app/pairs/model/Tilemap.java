package app.pairs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tilemap {
	private final int width;
	private final int height;
	private final int[][] id;
	private GameType difficulty = GameType.NORMAL;

	public Tilemap(int[][] id) {
		this.id = id;
		this.height = id.length;
		this.width = id[0].length;
	}

	public int getTile(int row, int col) {
		return id[row][col];
	}

	public void setTile(int row, int col, int newId) {
		id[row][col] = newId;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public GameType getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(GameType difficulty) {
		this.difficulty = difficulty;
	}

	/** A tile position on the map. */
	public record TilePos(int row, int col) {}

	/**
	 * Returns all non-empty tile positions grouped by tile id. Only ids {@code
	 * > 0} are included. The returned map and its lists are unmodifiable.
	 */
	public Map<Integer, List<TilePos>> tilesByIdMap() {
		Map<Integer, List<TilePos>> result = new HashMap<>();
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int tileId = id[r][c];
				if (tileId <= 0) {
					continue;
				}
				result.computeIfAbsent(tileId, k -> new ArrayList<>())
					.add(new TilePos(r, c));
			}
		}
		result.replaceAll((k, v) -> Collections.unmodifiableList(v));
		return Collections.unmodifiableMap(result);
	}
}

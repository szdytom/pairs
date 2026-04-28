package app.pairs.map;

import app.pairs.logic.TileTransition;
import app.pairs.model.Tilemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class TilemapGeneratorCore {
	static Tilemap generate(int[][] map, int types, Random random) {
		if (types < 1) {
			throw new IllegalArgumentException(
				"types must be >= 1, got: " + types
			);
		}
		int fillableTiles = 0;
		for (int[] row : map) {
			for (int cell : row) {
				if (cell == 0) {
					fillableTiles++;
				}
			}
		}
		if (fillableTiles % 2 != 0) {
			throw new IllegalArgumentException(
				"Fillable tile count must be even, got: " + fillableTiles
			);
		}
		if (fillableTiles / 2 < types) {
			throw new IllegalArgumentException(
				"Not enough fillable tiles for " + types + " types: fillable="
				+ fillableTiles + ", need at least " + (types * 2)
			);
		}

		int pairCount = fillSolvablePairs(map, random);
		if (pairCount < 0 || pairCount < types) {
			throw new IllegalStateException(
				"Not enough pairs to cover all tile types: pairs=" + pairCount
				+ ", types=" + types
			);
		}

		int[] pairToType = buildRandomTypeMapping(pairCount, types, random);
		return new Tilemap(idBuilder(map, pairToType));
	}

	private static int fillSolvablePairs(int[][] map, Random random) {
		List<TileIndex> remainingTiles = new ArrayList<>();
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				if (map[i][j] == 0) {
					map[i][j] = 1;
					remainingTiles.add(new TileIndex(i, j));
				}
			}
		}
		Collections.shuffle(remainingTiles, random);

		Tilemap tilemap = new Tilemap(map);
		List<TilePair> eliminationOrder = new ArrayList<>();
		while (!remainingTiles.isEmpty()) {
			int firstIdx = -1;
			int secondIdx = -1;
			for (int i = 0; i < remainingTiles.size(); i++) {
				List<Integer> candidates = connectableCandidates(
					tilemap, i, remainingTiles
				);
				if (!candidates.isEmpty()) {
					firstIdx = i;
					secondIdx = candidates.get(
						random.nextInt(candidates.size())
					);
					break;
				}
			}
			if (firstIdx < 0) {
				return -1;
			}
			TileIndex first = remainingTiles.get(firstIdx);
			TileIndex second = remainingTiles.get(secondIdx);
			eliminationOrder.add(new TilePair(first, second));
			map[first.row][first.col] = 0;
			map[second.row][second.col] = 0;
			swapRemove(remainingTiles, Math.max(firstIdx, secondIdx));
			swapRemove(remainingTiles, Math.min(firstIdx, secondIdx));
		}

		int pairValue = 1;
		for (TilePair pair : eliminationOrder) {
			map[pair.first.row][pair.first.col] = pairValue;
			map[pair.second.row][pair.second.col] = pairValue;
			pairValue++;
		}
		return eliminationOrder.size();
	}

	private static <T> void swapRemove(List<T> list, int index) {
		int last = list.size() - 1;
		if (index != last) {
			list.set(index, list.get(last));
		}
		list.remove(last);
	}

	private static List<Integer> connectableCandidates(
		Tilemap map, int firstIdx, List<TileIndex> candidatesPool
	) {
		TileIndex first = candidatesPool.get(firstIdx);
		List<Integer> candidates = new ArrayList<>();
		for (int j = 0; j < candidatesPool.size(); j++) {
			if (j == firstIdx) {
				continue;
			}
			TileIndex other = candidatesPool.get(j);
			if (TileTransition.transition(
					map, first.row, first.col, other.row, other.col
				)) {
				candidates.add(j);
			}
		}
		return candidates;
	}

	private static int[] buildRandomTypeMapping(
		int pairCount, int typeCount, Random random
	) {
		int[] pairToType = new int[pairCount + 1];
		for (int i = 0; i < typeCount; i++) {
			pairToType[i + 1] = i + 1;
		}
		for (int i = typeCount; i < pairCount; i++) {
			pairToType[i + 1] = random.nextInt(typeCount) + 1;
		}
		// Fisher-Yates over indexes 1..pairCount; pairToType[0] stays 0.
		for (int i = pairCount; i > 1; i--) {
			int j = random.nextInt(i) + 1;
			int tmp = pairToType[i];
			pairToType[i] = pairToType[j];
			pairToType[j] = tmp;
		}
		return pairToType;
	}

	private static int[][] idBuilder(int[][] map, int[] pairToType) {
		int[][] id = new int[map.length][map[0].length];
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				id[i][j] = pairToType[Math.max(0, map[i][j])];
			}
		}
		return id;
	}

	private static final class TileIndex {
		private final int row;
		private final int col;

		private TileIndex(int row, int col) {
			this.row = row;
			this.col = col;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof TileIndex)) {
				return false;
			}
			TileIndex other = (TileIndex)obj;
			return row == other.row && col == other.col;
		}

		@Override
		public int hashCode() {
			return 31 * row + col;
		}
	}

	private static final class TilePair {
		private final TileIndex first;
		private final TileIndex second;

		private TilePair(TileIndex first, TileIndex second) {
			this.first = first;
			this.second = second;
		}
	}
}

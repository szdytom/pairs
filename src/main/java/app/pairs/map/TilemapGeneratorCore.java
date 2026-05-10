package app.pairs.map;

import app.pairs.logic.TileTransition;
import app.pairs.model.Tilemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class TilemapGeneratorCore {
	static Tilemap generate(
		int[][] map, int types, Random random, PairingStrategy strategy
	) {
		if (types < 1) {
			throw new IllegalArgumentException(
				"types must be >= 1, got: " + types
			);
		}
		int fillableTiles = countFillableTiles(map);
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
		PairTypeMappingStrategy mapping = new RandomPairTypeMappingStrategy(
			types
		);
		return new Tilemap(generateIds(map, mapping, random, strategy));
	}

	static Tilemap generate(int[][] map, int types, Random random) {
		return generate(map, types, random, new BasePairingStrategy());
	}

	static int[][] generateIds(
		int[][] map, PairTypeMappingStrategy mapping, Random random
	) {
		return generateIds(map, mapping, random, new BasePairingStrategy());
	}

	static int[][] generateIds(
		int[][] map, PairTypeMappingStrategy mapping, Random random,
		PairingStrategy strategy
	) {
		int fillableTiles = countFillableTiles(map);
		if (fillableTiles % 2 != 0) {
			throw new IllegalArgumentException(
				"Fillable tile count must be even, got: " + fillableTiles
			);
		}

		int pairCount = fillSolvablePairs(map, random, strategy);
		if (pairCount < 0) {
			throw new IllegalStateException(
				"Failed to build solvable tile pairs"
			);
		}

		int[] pairToType = mapping.build(pairCount, random);
		return idBuilder(map, pairToType);
	}

	private static int countFillableTiles(int[][] map) {
		int fillableTiles = 0;
		for (int[] row : map) {
			for (int cell : row) {
				if (cell == 0) {
					fillableTiles++;
				}
			}
		}
		return fillableTiles;
	}

	private static int fillSolvablePairs(
		int[][] map, Random random, PairingStrategy strategy
	) {
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
					secondIdx = strategy.pickCandidate(
						candidates, i, remainingTiles, random
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

	public static int[][] idBuilder(int[][] map, int[] pairToType) {
		int[][] id = new int[map.length][map[0].length];
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				id[i][j] = pairToType[Math.max(0, map[i][j])];
			}
		}
		return id;
	}

	static final class TileIndex {
		final int row;
		final int col;

		TileIndex(int row, int col) {
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

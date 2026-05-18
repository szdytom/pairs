package app.pairs.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonObject;

public enum PairingStrategy {
	BASE(0) {
		@Override
		int pickCandidate(
			List<Integer> candidates, int firstIdx,
			List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
		) {
			return candidates.get(random.nextInt(candidates.size()));
		}
	},
	NON_ADJACENT(1) {
		@Override
		int pickCandidate(
			List<Integer> candidates, int firstIdx,
			List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
		) {
			var first = remainingTiles.get(firstIdx);
			List<Integer> far = new ArrayList<>();
			for (int candidate : candidates) {
				var other = remainingTiles.get(candidate);
				if (Math.abs(first.row - other.row) > 1
				    || Math.abs(first.col - other.col) > 1) {
					far.add(candidate);
				}
			}
			if (far.isEmpty()) {
				return candidates.get(random.nextInt(candidates.size()));
			}
			return far.get(random.nextInt(far.size()));
		}
	},
	DISTANT(2) {
		@Override
		int pickCandidate(
			List<Integer> candidates, int firstIdx,
			List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
		) {
			var first = remainingTiles.get(firstIdx);
			List<Integer> sorted = new ArrayList<>(candidates);
			sorted.sort(Comparator.comparingInt(candidate -> {
				var other = remainingTiles.get(candidate);
				return -(
					Math.abs(first.row - other.row)
					+ Math.abs(first.col - other.col)
				);
			}));
			int farCount = (sorted.size() + 1) / 2;
			return sorted.get(random.nextInt(farCount));
		}
	};

	private final int index;

	PairingStrategy(int index) {
		this.index = index;
	}

	abstract int pickCandidate(
		List<Integer> candidates, int firstIdx,
		List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
	);

	public int index() {
		return index;
	}

	public static PairingStrategy fromIndex(int index) {
		return switch (index) {
			case 1 -> NON_ADJACENT;
			case 2 -> DISTANT;
			default -> BASE;
		};
	}

	public static PairingStrategy parse(JsonObject root) {
		if (!root.has("pairStrategy")) {
			return BASE;
		}
		String name = root.get("pairStrategy").getAsString();
		return switch (name) {
			case "base", "BASE" -> BASE;
			case "nonAdjacent", "NON_ADJACENT" -> NON_ADJACENT;
			case "distant", "DISTANT" -> DISTANT;
			default ->
				throw new IllegalArgumentException(
					"Unknown pairStrategy: \"" + name
					+ "\" (expected base|nonAdjacent|distant)"
				);
		};
	}
}

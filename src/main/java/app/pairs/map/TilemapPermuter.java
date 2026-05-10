package app.pairs.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class TilemapPermuter {
	private static final int MAX_ATTEMPTS = 20;

	private TilemapPermuter() {}

	public static int[][] permute(
		int[][] legalPlacementShape, Map<Integer, Integer> tileCounts,
		Random random
	) {
		int totalTiles = totalTiles(tileCounts);
		if (totalTiles <= 0) {
			throw new IllegalStateException("No remaining tiles to repermute");
		}
		if (totalTiles % 2 != 0) {
			throw new IllegalStateException(
				"Remaining tile count must be even, got: " + totalTiles
			);
		}

		List<Position> legalPositions = legalPositions(legalPlacementShape);
		if (legalPositions.size() < totalTiles) {
			throw new IllegalStateException(
				"Not enough legal positions for repermute: legal="
				+ legalPositions.size() + ", tiles=" + totalTiles
			);
		}

		PairTypeMappingStrategy mapping = new PreservedPairTypeMappingStrategy(
			tileCounts
		);
		IllegalStateException lastFailure = null;
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			int[][] candidate = buildCandidate(
				legalPlacementShape, legalPositions, totalTiles, random
			);
			try {
				return TilemapGeneratorCore.generateIds(
					candidate, mapping, random
				);
			} catch (IllegalStateException ex) {
				lastFailure = ex;
			}
		}
		throw new IllegalStateException(
			"Failed to repermute a solvable map after " + MAX_ATTEMPTS
				+ " attempts",
			lastFailure
		);
	}

	private static int totalTiles(Map<Integer, Integer> tileCounts) {
		int total = 0;
		for (int count : tileCounts.values()) {
			total += count;
		}
		return total;
	}

	private static List<Position> legalPositions(int[][] shape) {
		if (shape.length == 0 || shape[0].length == 0) {
			throw new IllegalStateException("Legal placement shape is empty");
		}
		int width = shape[0].length;
		List<Position> positions = new ArrayList<>();
		for (int r = 0; r < shape.length; r++) {
			if (shape[r].length != width) {
				throw new IllegalStateException(
					"Legal placement shape must be rectangular"
				);
			}
			for (int c = 0; c < width; c++) {
				if (shape[r][c] == 0) {
					positions.add(new Position(r, c));
				}
			}
		}
		return positions;
	}

	private static int[][] buildCandidate(
		int[][] shape, List<Position> legalPositions, int totalTiles,
		Random random
	) {
		int[][] candidate = new int[shape.length][shape[0].length];
		for (int[] row : candidate) {
			Arrays.fill(row, -1);
		}

		List<Position> shuffled = new ArrayList<>(legalPositions);
		Collections.shuffle(shuffled, random);
		for (int i = 0; i < totalTiles; i++) {
			Position pos = shuffled.get(i);
			candidate[pos.row][pos.col] = 0;
		}
		return candidate;
	}

	private record Position(int row, int col) {}
}

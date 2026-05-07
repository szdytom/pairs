package app.pairs.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class PreservedPairTypeMappingStrategy
	implements PairTypeMappingStrategy {
	private final List<Integer> pairTypes;

	PreservedPairTypeMappingStrategy(Map<Integer, Integer> tileCounts) {
		this.pairTypes = buildPairTypes(tileCounts);
	}

	@Override
	public int[] build(int pairCount, Random random) {
		if (pairCount != pairTypes.size()) {
			throw new IllegalStateException(
				"Pair count mismatch: generated=" + pairCount
				+ ", expected=" + pairTypes.size()
			);
		}
		List<Integer> shuffled = new ArrayList<>(pairTypes);
		Collections.shuffle(shuffled, random);

		int[] pairToType = new int[pairCount + 1];
		for (int i = 0; i < pairCount; i++) {
			pairToType[i + 1] = shuffled.get(i);
		}
		return pairToType;
	}

	private static List<Integer> buildPairTypes(
		Map<Integer, Integer> tileCounts
	) {
		if (tileCounts.isEmpty()) {
			throw new IllegalStateException("No remaining tiles to repermute");
		}
		List<Integer> tileIds = new ArrayList<>(tileCounts.keySet());
		Collections.sort(tileIds);

		List<Integer> result = new ArrayList<>();
		for (int tileId : tileIds) {
			int count = tileCounts.get(tileId);
			if (tileId <= 0 || count <= 0 || count % 2 != 0) {
				throw new IllegalStateException(
					"Tile counts must be positive pairs, got id=" + tileId
					+ ", count=" + count
				);
			}
			for (int i = 0; i < count / 2; i++) {
				result.add(tileId);
			}
		}
		return result;
	}
}

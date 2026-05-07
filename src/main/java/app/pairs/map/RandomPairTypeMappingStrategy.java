package app.pairs.map;

import java.util.Random;

final class RandomPairTypeMappingStrategy implements PairTypeMappingStrategy {
	private final int typeCount;

	RandomPairTypeMappingStrategy(int typeCount) {
		if (typeCount < 1) {
			throw new IllegalArgumentException(
				"types must be >= 1, got: " + typeCount
			);
		}
		this.typeCount = typeCount;
	}

	@Override
	public int[] build(int pairCount, Random random) {
		if (pairCount < typeCount) {
			throw new IllegalStateException(
				"Not enough pairs to cover all tile types: pairs=" + pairCount
				+ ", types=" + typeCount
			);
		}
		int[] pairToType = new int[pairCount + 1];
		for (int i = 0; i < typeCount; i++) {
			pairToType[i + 1] = i + 1;
		}
		for (int i = typeCount; i < pairCount; i++) {
			pairToType[i + 1] = random.nextInt(typeCount) + 1;
		}
		for (int i = pairCount; i > 1; i--) {
			int j = random.nextInt(i) + 1;
			int tmp = pairToType[i];
			pairToType[i] = pairToType[j];
			pairToType[j] = tmp;
		}
		return pairToType;
	}
}

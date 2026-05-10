package app.pairs.map;

import java.util.Random;

interface PairTypeMappingStrategy {
	int[] build(int pairCount, Random random);
}

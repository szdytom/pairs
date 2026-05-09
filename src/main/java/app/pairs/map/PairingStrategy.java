package app.pairs.map;

import java.util.List;
import java.util.Random;

interface PairingStrategy {
	int pickCandidate(
		List<Integer> candidates, int firstIdx,
		List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
	);
}

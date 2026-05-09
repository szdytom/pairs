package app.pairs.map;

import java.util.List;
import java.util.Random;

final class BasePairingStrategy implements PairingStrategy {
	@Override
	public int pickCandidate(
		List<Integer> candidates, int firstIdx,
		List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
	) {
		return candidates.get(random.nextInt(candidates.size()));
	}
}

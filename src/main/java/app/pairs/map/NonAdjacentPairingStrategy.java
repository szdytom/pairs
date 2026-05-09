package app.pairs.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class NonAdjacentPairingStrategy implements PairingStrategy {
	@Override
	public int pickCandidate(
		List<Integer> candidates, int firstIdx,
		List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
	) {
		var first = remainingTiles.get(firstIdx);
		List<Integer> far = new ArrayList<>();
		for (int c : candidates) {
			var other = remainingTiles.get(c);
			if (Math.abs(first.row - other.row) > 1
			    || Math.abs(first.col - other.col) > 1) {
				far.add(c);
			}
		}
		if (far.isEmpty()) {
			return candidates.get(random.nextInt(candidates.size()));
		}
		return far.get(random.nextInt(far.size()));
	}
}

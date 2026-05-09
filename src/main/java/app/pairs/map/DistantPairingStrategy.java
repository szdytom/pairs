package app.pairs.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class DistantPairingStrategy implements PairingStrategy {
	@Override
	public int pickCandidate(
		List<Integer> candidates, int firstIdx,
		List<TilemapGeneratorCore.TileIndex> remainingTiles, Random random
	) {
		var first = remainingTiles.get(firstIdx);
		List<Integer> sorted = new ArrayList<>(candidates);
		sorted.sort(Comparator.comparingInt(c -> {
			var other = remainingTiles.get(c);
			return -(
				Math.abs(first.row - other.row)
				+ Math.abs(first.col - other.col)
			);
		}));
		int farCount = (sorted.size() + 1) / 2;
		return sorted.get(random.nextInt(farCount));
	}
}

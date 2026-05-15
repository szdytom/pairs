package app.pairs.map;

import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;

public record DifficultyParams(
	int width, int height, int types, boolean slabs,
	TileSelectionPolicy.Spread spread, PairingStrategy strategy,
	int totalPoints, Tilemap.Difficulty tier, Seed seed
) {
	public String sizeLabel() {
		return width + "\u00D7" + height;
	}

	public String spreadLabel() {
		return switch (spread) {
			case NO_DUPLICATES -> "Kind";
			case FREE -> "Neutral";
			case PREFER_DUPLICATES -> "Mean";
		};
	}

	public String strategyLabel() {
		if (strategy instanceof DistantPairingStrategy) return "Mean";
		if (strategy instanceof NonAdjacentPairingStrategy) return "Neutral";
		return "Kind";
	}

	public String tierLabel() {
		return tier.name();
	}
}

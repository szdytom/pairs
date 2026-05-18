package app.pairs.map;

import app.pairs.model.Tilemap;

public record TilemapFactorySnapshot(
	Long seedS0, Long seedS1, int width, int height, int types,
	boolean registryPalette, boolean includeSlabs,
	TileSelectionPolicy.Spread spread, PairingStrategy pairingStrategy,
	Tilemap.Difficulty difficulty, int[][] initial, int[] subset,
	int[][] fixedGrid
) {
	public TilemapFactorySnapshot {
		if (spread == null) {
			spread = TileSelectionPolicy.Spread.NO_DUPLICATES;
		}
		if (pairingStrategy == null) {
			pairingStrategy = PairingStrategy.BASE;
		}
		if (difficulty == null) {
			difficulty = Tilemap.Difficulty.NORMAL;
		}
	}
}

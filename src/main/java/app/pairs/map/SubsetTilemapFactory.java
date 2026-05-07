package app.pairs.map;

import app.pairs.model.Tilemap;

/**
 * Decorates a {@link TilemapFactory} by remapping each generated tile type
 * (1..N produced by {@link TilemapGeneratorCore}) to a chosen subset of
 * registry numeric IDs. The mapping is fixed for the lifetime of this factory,
 * so repeated {@link #generate()} calls produce different layouts but share
 * the same tile palette.
 */
public final class SubsetTilemapFactory implements TilemapFactory {
	private final TilemapFactory inner;
	private final int[] subset;

	/**
	 * @param subset 1-based array; {@code subset[i]} is the registry numeric
	 *               ID that should replace generator output {@code i}.
	 *               Index 0 is unused.
	 */
	public SubsetTilemapFactory(TilemapFactory inner, int[] subset) {
		this.inner = inner;
		this.subset = subset.clone();
	}

	@Override
	public Tilemap generate() {
		Tilemap raw = inner.generate();
		for (int r = 0; r < raw.getHeight(); r++) {
			for (int c = 0; c < raw.getWidth(); c++) {
				int v = raw.getTile(r, c);
				if (v <= 0) {
					continue;
				}
				raw.setTile(r, c, subset[v]);
			}
		}
		return raw;
	}

	@Override
	public int[][] buildLegalPlacementShape() {
		return inner.buildLegalPlacementShape();
	}
}

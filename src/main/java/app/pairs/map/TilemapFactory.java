package app.pairs.map;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.model.Tilemap;
import app.pairs.model.TilemapSnapshot;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

public final class TilemapFactory {
	private static final String TILE_REGISTRY = "tiles/typed";
	private static final String TILE_GROUPS = "tile-groups/default";
	private static final TileSelectionPolicy
		.Spread DEFAULT_SPREAD = TileSelectionPolicy.Spread.NO_DUPLICATES;

	private final Seed seed;
	private final int width;
	private final int height;
	private final int types;
	private final boolean registryPalette;
	private final boolean includeSlabs;
	private final TileSelectionPolicy.Spread spread;
	private final PairingStrategy pairingStrategy;
	private final Tilemap.Difficulty difficulty;
	private final int[][] initial;
	private final int[] subset;
	private final int[][] fixedGrid;

	private TilemapFactory(
		Seed seed, int width, int height, int types, boolean registryPalette,
		boolean includeSlabs, TileSelectionPolicy.Spread spread,
		PairingStrategy pairingStrategy, Tilemap.Difficulty difficulty,
		int[][] initial, int[] subset, int[][] fixedGrid
	) {
		this.seed = seed;
		this.width = width;
		this.height = height;
		this.types = types;
		this.registryPalette = registryPalette;
		this.includeSlabs = includeSlabs;
		this.spread = spread == null ? DEFAULT_SPREAD : spread;
		this.pairingStrategy = pairingStrategy == null
			? PairingStrategy.BASE
			: pairingStrategy;
		this.difficulty = difficulty == null
			? Tilemap.Difficulty.NORMAL
			: difficulty;
		this.initial = copy(initial);
		this.subset = subset == null ? null : subset.clone();
		this.fixedGrid = copy(fixedGrid);
	}

	public Tilemap generate() {
		if (fixedGrid != null) {
			return Tilemap.fromSnapshot(
				new TilemapSnapshot(difficulty, fixedGrid)
			);
		}
		if (seed == null) {
			throw new IllegalStateException("seed is required to generate map");
		}
		int[][] map = initial == null ? new int[height][width] : copy(initial);
		Tilemap tilemap = TilemapGeneratorCore.generate(
			map, types, new Xoroshiro128PP(seed), pairingStrategy
		);
		if (registryPalette) {
			applySubset(tilemap, subsetOrSelect());
		}
		tilemap.setDifficulty(difficulty);
		return tilemap;
	}

	public TilemapFactorySnapshot toSnapshot() {
		Long seedS0 = seed == null ? null : seed.s0();
		Long seedS1 = seed == null ? null : seed.s1();
		return new TilemapFactorySnapshot(
			seedS0, seedS1, width, height, types, registryPalette, includeSlabs,
			spread, pairingStrategy, difficulty, copy(initial),
			subset == null ? null : subset.clone(), copy(fixedGrid)
		);
	}

	public static TilemapFactory fromSnapshot(TilemapFactorySnapshot snapshot) {
		if (snapshot == null) {
			throw new IllegalStateException("missing tilemap factory snapshot");
		}
		Seed seed = snapshot.seedS0() == null || snapshot.seedS1() == null
			? null
			: new Seed(snapshot.seedS0(), snapshot.seedS1());
		int[] subset = snapshot.subset();
		if (snapshot.registryPalette() && subset == null && seed != null) {
			subset = selectSubset(
				snapshot.includeSlabs(), snapshot.spread(), snapshot.types(),
				seed
			);
		}
		return new TilemapFactory(
			seed, snapshot.width(), snapshot.height(), snapshot.types(),
			snapshot.registryPalette(), snapshot.includeSlabs(),
			snapshot.spread(), snapshot.pairingStrategy(),
			snapshot.difficulty(), snapshot.initial(), subset,
			snapshot.fixedGrid()
		);
	}

	public static TilemapFactory fromPreset(String presetId) {
		return fromPreset(presetId, Seed.deviceRandom());
	}

	public static TilemapFactory fromPreset(String presetId, Seed seed) {
		PresetConfig config = AssetManager.instance().get(presetId);
		TilemapPreset preset = config.preset();
		TileSelectionPolicy policy = config.policy();
		return registryBacked(
			preset.width(), preset.height(), preset.types(), seed,
			policy.includeSlabs(), policy.spread(), preset.pairingStrategy(),
			config.difficulty(), preset.initial()
		);
	}

	public static TilemapFactory fromPreset(TilemapPreset preset, Seed seed) {
		return new TilemapFactory(
			seed, preset.width(), preset.height(), preset.types(), false, false,
			DEFAULT_SPREAD, preset.pairingStrategy(), Tilemap.Difficulty.NORMAL,
			preset.initial(), null, null
		);
	}

	public static TilemapFactory customized(
		int width, int height, int types, Seed seed
	) {
		return new TilemapFactory(
			seed, width, height, types, false, false, DEFAULT_SPREAD,
			PairingStrategy.BASE, Tilemap.Difficulty.NORMAL, null, null, null
		);
	}

	public static TilemapFactory customized(
		int width, int height, int types, boolean includeSlabs,
		TileSelectionPolicy.Spread spread, PairingStrategy pairingStrategy,
		Seed seed, Tilemap.Difficulty difficulty
	) {
		return registryBacked(
			width, height, types, seed, includeSlabs, spread, pairingStrategy,
			difficulty, null
		);
	}

	public static TilemapFactory fixed(TilemapSnapshot snapshot) {
		return new TilemapFactory(
			null, 0, 0, 0, false, false, DEFAULT_SPREAD, PairingStrategy.BASE,
			snapshot.difficulty(), null, null, snapshot.grid()
		);
	}

	private static TilemapFactory registryBacked(
		int width, int height, int types, Seed seed, boolean includeSlabs,
		TileSelectionPolicy.Spread spread, PairingStrategy pairingStrategy,
		Tilemap.Difficulty difficulty, int[][] initial
	) {
		int[] subset = selectSubset(includeSlabs, spread, types, seed);
		return new TilemapFactory(
			seed, width, height, types, true, includeSlabs, spread,
			pairingStrategy, difficulty, initial, subset, null
		);
	}

	private int[] subsetOrSelect() {
		if (subset != null) {
			return subset;
		}
		return selectSubset(includeSlabs, spread, types, seed);
	}

	private static int[] selectSubset(
		boolean includeSlabs, TileSelectionPolicy.Spread spread, int types,
		Seed seed
	) {
		TileSelectionPolicy policy = new TileSelectionPolicy(
			includeSlabs, spread
		);
		TileRegistry registry = AssetManager.instance().get(TILE_REGISTRY);
		TileGroupRegistry groups = AssetManager.instance().get(TILE_GROUPS);
		return policy.selectFor(
			registry, groups, types, new Xoroshiro128PP(seed)
		);
	}

	private static void applySubset(Tilemap tilemap, int[] subset) {
		for (int row = 0; row < tilemap.getHeight(); row++) {
			for (int col = 0; col < tilemap.getWidth(); col++) {
				int value = tilemap.getTile(row, col);
				if (value <= 0) {
					continue;
				}
				tilemap.setTile(row, col, subset[value]);
			}
		}
	}

	private static int[][] copy(int[][] source) {
		if (source == null) {
			return null;
		}
		int[][] result = new int[source.length][];
		for (int row = 0; row < source.length; row++) {
			result[row] = source[row].clone();
		}
		return result;
	}
}

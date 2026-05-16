package app.pairs.map;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.logic.GameState;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import java.util.Random;

public class RogueDifficultyGenerator {
	private static final int MIN_SIZE = 6;
	private static final int MAX_SIZE = 14;
	private static final int MIN_TYPES = 3;
	private static final int MAX_TYPES = 20;
	private static final int MAX_ATTEMPTS = 200;
	private static final int TOLERANCE = 2;
	private static final int MAX_DIM_DIFF = 2;

	private static final TileSelectionPolicy
		.Spread[] SPREADS = TileSelectionPolicy.Spread.values();

	public static DifficultyParams preview(int level) {
		Seed seed = Seed.deviceRandom();
		return searchParams(level, seed);
	}

	public static GameState generate(DifficultyParams params) {
		return buildGameState(params);
	}

	private static DifficultyParams searchParams(int level, Seed seed) {
		int target = targetForLevel(level);
		var rng = new Xoroshiro128PP(seed);

		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			int w = randomDimension(rng);
			int h = randomDimensionCloseTo(rng, w);

			int types = MIN_TYPES + rng.nextInt(MAX_TYPES - MIN_TYPES + 1);

			boolean slabs = rng.nextBoolean();
			TileSelectionPolicy
				.Spread spread = SPREADS[rng.nextInt(SPREADS.length)];
			PairingStrategy strategy = pickStrategy(rng);

			int total = computePoints(w, h, types, slabs, spread, strategy);

			if (Math.abs(total - target) <= TOLERANCE) {
				return new DifficultyParams(
					w, h, types, slabs, spread, strategy, total,
					tierForPoints(total), seed
				);
			}
		}

		return fallbackParams(level, seed);
	}

	private static int randomDimension(Random rng) {
		return MIN_SIZE + rng.nextInt(MAX_SIZE - MIN_SIZE + 1);
	}

	private static int randomDimensionCloseTo(Random rng, int w) {
		int lo = Math.max(MIN_SIZE, w - MAX_DIM_DIFF);
		int hi = Math.min(MAX_SIZE, w + MAX_DIM_DIFF);
		for (int attempt = 0; attempt < 10; attempt++) {
			int h = lo + rng.nextInt(hi - lo + 1);
			if ((w * h) % 2 == 0)
				return h;
		}
		return (w * lo) % 2 == 0 ? lo : lo + 1;
	}

	private static DifficultyParams fallbackParams(int level, Seed seed) {
		int w = Math.min(MIN_SIZE + (level - 1), MAX_SIZE);
		int h = Math.min(MIN_SIZE + (level - 1), MAX_SIZE);
		if ((w * h) % 2 != 0)
			h++;
		if (h > MAX_SIZE) {
			w--;
			h--;
		}

		int types = Math.min(6 + (level - 1) * 2, MAX_TYPES);
		boolean slabs = level >= 5;
		TileSelectionPolicy.Spread spread = level >= 8
			? TileSelectionPolicy.Spread.PREFER_DUPLICATES
			: level >= 4 ? TileSelectionPolicy.Spread.FREE
						 : TileSelectionPolicy.Spread.NO_DUPLICATES;
		PairingStrategy strategy = level >= 10 ? new DistantPairingStrategy()
			: level >= 6 ? new NonAdjacentPairingStrategy()
						 : new BasePairingStrategy();
		int total = computePoints(w, h, types, slabs, spread, strategy);
		return new DifficultyParams(
			w, h, types, slabs, spread, strategy, total, tierForPoints(total),
			seed
		);
	}

	private static GameState buildGameState(DifficultyParams params) {
		TileSelectionPolicy policy = new TileSelectionPolicy(
			params.slabs(), params.spread()
		);
		TileRegistry registry = AssetManager.instance().get("tiles/typed");
		TileGroupRegistry groups = AssetManager.instance().get(
			"tile-groups/default"
		);
		int[] subset = policy.selectFor(
			registry, groups, params.types(), new Xoroshiro128PP(params.seed())
		);

		TilemapPreset preset = new TilemapPreset(
			params.width(), params.height(), params.types(), null,
			params.strategy()
		);
		TilemapFactory inner = CustomizedTilemapFactory.fromPreset(
			preset, params.seed()
		);

		TilemapFactory factory = () -> {
			Tilemap t = new SubsetTilemapFactory(inner, subset).generate();
			t.setDifficulty(params.tier());
			return t;
		};

		return new GameState(factory);
	}

	private static int targetForLevel(int level) {
		return Math.min(3 + (level - 1) * 3, 34);
	}

	static int computePoints(
		int w, int h, int types, boolean slabs,
		TileSelectionPolicy.Spread spread, PairingStrategy strategy
	) {
		int tileCount = w * h;
		int sizePts = Math.round((float)tileCount / 10) + 1;
		int typePts = typePoints(types);
		int visualPts = (slabs ? 2 : 0) + spreadPoints(spread);
		int pairingPts = pairingPoints(strategy);
		return sizePts + typePts + visualPts + pairingPts;
	}

	private static int typePoints(int types) {
		if (types <= 8)
			return 0;
		if (types <= 12)
			return 1;
		if (types <= 16)
			return 2;
		return 3;
	}

	private static int spreadPoints(TileSelectionPolicy.Spread s) {
		return switch (s) {
			case NO_DUPLICATES -> 0;
			case FREE -> 3;
			case PREFER_DUPLICATES -> 6;
		};
	}

	private static int pairingPoints(PairingStrategy s) {
		if (s instanceof DistantPairingStrategy)
			return 2;
		if (s instanceof NonAdjacentPairingStrategy)
			return 1;
		return 0;
	}

	private static Tilemap.Difficulty tierForPoints(int pts) {
		if (pts <= 5)
			return Tilemap.Difficulty.EASY;
		if (pts <= 14)
			return Tilemap.Difficulty.NORMAL;
		if (pts <= 24)
			return Tilemap.Difficulty.HARD;
		return Tilemap.Difficulty.EXTREME;
	}

	private static PairingStrategy pickStrategy(Random rng) {
		int n = rng.nextInt(3);
		if (n == 0)
			return new BasePairingStrategy();
		if (n == 1)
			return new NonAdjacentPairingStrategy();
		return new DistantPairingStrategy();
	}
}

package app.pairs.map;

import app.pairs.logic.GameState;
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
		return CustomGameBuilder.build(
			params.width(), params.height(), params.types(), params.slabs(),
			params.spread(), params.strategy(), params.seed(), params.tier()
		);
	}

	private static DifficultyParams searchParams(int level, Seed seed) {
		int target = targetForLevel(level);
		var rng = new Xoroshiro128PP(seed);

		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			int w = randomDimension(rng);
			int h = randomDimensionCloseTo(rng, w);

			int types = MIN_TYPES + rng.nextInt(MAX_TYPES - MIN_TYPES + 1);

			if (types > w * h / 2)
				continue;

			boolean slabs = rng.nextBoolean();
			TileSelectionPolicy
				.Spread spread = SPREADS[rng.nextInt(SPREADS.length)];
			PairingStrategy strategy = pickStrategy(rng);

			int total = CustomGameBuilder.computePoints(
				w, h, types, slabs, spread, strategy
			);

			if (Math.abs(total - target) <= TOLERANCE) {
				return new DifficultyParams(
					w, h, types, slabs, spread, strategy, total,
					CustomGameBuilder.tierForPoints(total), seed
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
		PairingStrategy strategy = level >= 10 ? PairingStrategy.DISTANT
			: level >= 6                       ? PairingStrategy.NON_ADJACENT
											   : PairingStrategy.BASE;
		int total = CustomGameBuilder.computePoints(
			w, h, types, slabs, spread, strategy
		);
		return new DifficultyParams(
			w, h, types, slabs, spread, strategy, total,
			CustomGameBuilder.tierForPoints(total), seed
		);
	}

	private static int targetForLevel(int level) {
		return Math.min(3 + (level - 1) * 3, 34);
	}

	private static PairingStrategy pickStrategy(Random rng) {
		int n = rng.nextInt(3);
		if (n == 0)
			return PairingStrategy.BASE;
		if (n == 1)
			return PairingStrategy.NON_ADJACENT;
		return PairingStrategy.DISTANT;
	}
}

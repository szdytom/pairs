package app.pairs.map;

import app.pairs.logic.GameState;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;

public class CustomGameBuilder {
	private CustomGameBuilder() {}

	public static GameState build(
		int width, int height, int types, boolean slabs,
		TileSelectionPolicy.Spread spread, int strategyIndex
	) {
		validate(width, height, types);
		PairingStrategy strategy = PairingStrategy.fromIndex(strategyIndex);
		Seed seed = Seed.deviceRandom();
		int points = computePoints(
			width, height, types, slabs, spread, strategyIndex
		);
		return build(
			width, height, types, slabs, spread, strategy, seed,
			tierForPoints(points)
		);
	}

	public static GameState build(
		int width, int height, int types, boolean slabs,
		TileSelectionPolicy.Spread spread, PairingStrategy strategy, Seed seed,
		Tilemap.Difficulty tier
	) {
		return new GameState(TilemapFactory.customized(
			width, height, types, slabs, spread, strategy, seed, tier
		));
	}

	public static String validationError(int width, int height, int types) {
		int area = width * height;
		if (area % 2 != 0)
			return "Area " + width + "\u00D7" + height + " must be even";
		if (types > area / 2)
			return "Too many types (max " + (area / 2) + ")";
		return null;
	}

	private static void validate(int width, int height, int types) {
		String err = validationError(width, height, types);
		if (err != null)
			throw new IllegalArgumentException(err);
	}

	public static int computePoints(
		int w, int h, int types, boolean slabs,
		TileSelectionPolicy.Spread spread, int strategyIndex
	) {
		int tileCount = w * h;
		int sizePts = Math.round((float)tileCount / 10) + 1;
		int typePts = typePoints(types);
		int visualPts = (slabs ? 2 : 0) + spreadPoints(spread);
		int pairingPts = switch (strategyIndex) {
			case 2 -> 2;
			case 1 -> 1;
			default -> 0;
		};
		return sizePts + typePts + visualPts + pairingPts;
	}

	static int computePoints(
		int w, int h, int types, boolean slabs,
		TileSelectionPolicy.Spread spread, PairingStrategy strategy
	) {
		return computePoints(w, h, types, slabs, spread, strategy.index());
	}

	public static Tilemap.Difficulty tierForPoints(int pts) {
		if (pts <= 5)
			return Tilemap.Difficulty.EASY;
		if (pts <= 14)
			return Tilemap.Difficulty.NORMAL;
		if (pts <= 24)
			return Tilemap.Difficulty.HARD;
		return Tilemap.Difficulty.EXTREME;
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
}

package app.pairs.map;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.logic.GameState;
import app.pairs.model.GameType;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

public class CustomGameBuilder {
	private CustomGameBuilder() {}

	public static GameState build(
		int width, int height, int types, boolean slabs,
		TileSelectionPolicy.Spread spread, int strategyIndex
	) {
		validate(width, height, types);
		PairingStrategy strategy = switch (strategyIndex) {
			case 0 -> new BasePairingStrategy();
			case 1 -> new NonAdjacentPairingStrategy();
			case 2 -> new DistantPairingStrategy();
			default -> new BasePairingStrategy();
		};
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
		GameType tier
	) {
		var policy = new TileSelectionPolicy(slabs, spread);
		var registry = AssetManager.instance().<TileRegistry>get("tiles/typed");
		var groups = AssetManager.instance().<TileGroupRegistry>get(
			"tile-groups/default"
		);
		int[] subset = policy.selectFor(
			registry, groups, types, new Xoroshiro128PP(seed)
		);
		var preset = new TilemapPreset(width, height, types, null, strategy);
		TilemapFactory inner = CustomizedTilemapFactory.fromPreset(
			preset, seed
		);
		TilemapFactory factory = () -> {
			Tilemap t = new SubsetTilemapFactory(inner, subset).generate();
			t.setDifficulty(tier);
			return t;
		};
		return new GameState(factory);
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
		int idx = strategy instanceof DistantPairingStrategy ? 2
			: strategy instanceof NonAdjacentPairingStrategy
			? 1
			: 0;
		return computePoints(w, h, types, slabs, spread, idx);
	}

	public static GameType tierForPoints(int pts) {
		if (pts <= 5)
			return GameType.EASY;
		if (pts <= 14)
			return GameType.NORMAL;
		if (pts <= 24)
			return GameType.HARD;
		return GameType.EXTREME;
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

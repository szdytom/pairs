package app.pairs.map;

import com.google.gson.JsonObject;

/**
 * Data-driven tilemap configuration consumed by {@link
 * TilemapFactory#fromPreset}.
 *
 * <p>
 * {@code initial} is the seed grid for the generator: cells with value
 * {@code 0} are fillable, {@code -1} marks blocked/empty cells. May be
 * {@code null}, in which case a fresh all-zero grid of size
 * {@code height x width} is used (every cell fillable).
 */
public record TilemapPreset(
	int width, int height, int types, int[][] initial,
	PairingStrategy pairingStrategy
) {
	public TilemapPreset {
		if (pairingStrategy == null) {
			pairingStrategy = new BasePairingStrategy();
		}
	}

	/**
	 * Parse a {@link PairingStrategy} from a JSON field. Accepts
	 * {@code "base"}, {@code "nonAdjacent"}, {@code "distant"}. Returns the
	 * base strategy when the field is absent.
	 */
	public static PairingStrategy parseStrategy(JsonObject root) {
		if (!root.has("pairStrategy")) {
			return new BasePairingStrategy();
		}
		String name = root.get("pairStrategy").getAsString();
		return switch (name) {
			case "base" -> new BasePairingStrategy();
			case "nonAdjacent" -> new NonAdjacentPairingStrategy();
			case "distant" -> new DistantPairingStrategy();
			default ->
				throw new IllegalArgumentException(
					"Unknown pairStrategy: \"" + name
					+ "\" (expected base|nonAdjacent|distant)"
				);
		};
	}

	/** Build a fresh mutable seed grid for the generator. */
	public int[][] buildInitial() {
		if (initial == null) {
			return new int[height][width];
		}
		int[][] copy = new int[height][width];
		for (int i = 0; i < height; i++) {
			copy[i] = initial[i].clone();
		}
		return copy;
	}
}

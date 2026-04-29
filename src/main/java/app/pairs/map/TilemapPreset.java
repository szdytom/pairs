package app.pairs.map;

/**
 * Data-driven tilemap configuration consumed by {@link PresetTilemapFactory}.
 *
 * <p>
 * {@code initial} is the seed grid for the generator: cells with value
 * {@code 0} are fillable, {@code -1} marks blocked/empty cells. May be
 * {@code null}, in which case a fresh all-zero grid of size
 * {@code height x width} is used (every cell fillable).
 */
public record TilemapPreset(int width, int height, int types, int[][] initial) {
	/** Build a fresh mutable seed grid for the generator. */
	public int[][] buildInitial() {
		if (initial == null) {
			return new int[height][width];
		}
		if (initial.length != height) {
			throw new IllegalArgumentException(
				"initial row count (" + initial.length
				+ ") does not match height (" + height + ")"
			);
		}
		int[][] copy = new int[height][width];
		for (int i = 0; i < height; i++) {
			if (initial[i] == null) {
				throw new IllegalArgumentException(
					"initial row " + i + " is null"
				);
			}
			if (initial[i].length != width) {
				throw new IllegalArgumentException(
					"initial row " + i + " column count (" + initial[i].length
					+ ") does not match width (" + width + ")"
				);
			}
			copy[i] = initial[i].clone();
		}
		return copy;
	}
}

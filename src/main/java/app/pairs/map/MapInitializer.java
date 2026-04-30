package app.pairs.map;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import java.util.Random;

/**
 * Facade for building difficulty-specific {@link TilemapFactory}s. Each
 * factory consists of:
 * <ol>
 * <li>a layout generator (a {@link PresetTilemapFactory} for fixed
 * difficulties, a {@link CustomizedTilemapFactory} for custom games);</li>
 * <li>a tile-palette chosen by a {@link TileSelectionPolicy}, applied
 * through {@link SubsetTilemapFactory}.</li>
 * </ol>
 *
 * Each method has a no-arg form that uses a fresh device-random {@link Seed}
 * and an overload that accepts a specific seed for reproducibility.
 */
public final class MapInitializer {
	private static final String EASY_PRESET = "tilemap/easy";
	private static final String HARD_PRESET = "tilemap/hard";
	private static final String EXTREME_PRESET = "tilemap/extreme";
	private static final String TILE_REGISTRY = "tiles/typed";
	private static final String TILE_GROUPS = "tile-groups/default";

	private MapInitializer() {}

	public static TilemapFactory easy() {
		return easy(Seed.deviceRandom());
	}

	public static TilemapFactory easy(Seed seed) {
		return fromPreset(EASY_PRESET, TileSelectionPolicy.easy(), seed);
	}

	public static TilemapFactory hard() {
		return hard(Seed.deviceRandom());
	}

	public static TilemapFactory hard(Seed seed) {
		return fromPreset(HARD_PRESET, TileSelectionPolicy.hard(), seed);
	}

	public static TilemapFactory extreme() {
		return extreme(Seed.deviceRandom());
	}

	public static TilemapFactory extreme(Seed seed) {
		return fromPreset(EXTREME_PRESET, TileSelectionPolicy.extreme(), seed);
	}

	public static TilemapFactory custom(
		int width, int height, int types, boolean includeSlabs,
		TileSelectionPolicy.Spread spread
	) {
		return custom(
			width, height, types, includeSlabs, spread, Seed.deviceRandom()
		);
	}

	public static TilemapFactory custom(
		int width, int height, int types, boolean includeSlabs,
		TileSelectionPolicy.Spread spread, Seed seed
	) {
		TileSelectionPolicy policy = new TileSelectionPolicy(
			includeSlabs, spread
		);
		int[] subset = pickSubset(policy, types, seed);
		TilemapFactory inner = new CustomizedTilemapFactory(seed)
								   .setWidth(width)
								   .setHeight(height)
								   .setTypes(types);
		return new SubsetTilemapFactory(inner, subset);
	}

	private static TilemapFactory fromPreset(
		String presetId, TileSelectionPolicy policy, Seed seed
	) {
		TilemapPreset preset = AssetManager.instance().get(presetId);
		int[] subset = pickSubset(policy, preset.types(), seed);
		TilemapFactory inner = new PresetTilemapFactory(preset, seed);
		return new SubsetTilemapFactory(inner, subset);
	}

	private static int[] pickSubset(
		TileSelectionPolicy policy, int count, Seed seed
	) {
		TileRegistry registry = AssetManager.instance().get(TILE_REGISTRY);
		TileGroupRegistry groups = AssetManager.instance().get(TILE_GROUPS);
		Random rnd = new Xoroshiro128PP(seed);
		return policy.selectFor(registry, groups, count, rnd);
	}
}

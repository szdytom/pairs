package app.pairs.map;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

public interface TilemapFactory {
	Tilemap generate();

	/**
	 * Build from a preset asset ID, inferring the policy from the ID suffix.
	 */
	static TilemapFactory fromPreset(String presetId) {
		return fromPreset(presetId, policyFor(presetId), Seed.deviceRandom());
	}

	/** Build from a preset asset ID with an explicit seed. */
	static TilemapFactory fromPreset(String presetId, Seed seed) {
		return fromPreset(presetId, policyFor(presetId), seed);
	}

	/** Build from a preset asset ID with an explicit policy and seed. */
	static TilemapFactory fromPreset(
		String presetId, TileSelectionPolicy policy, Seed seed
	) {
		return () -> {
			TilemapPreset preset = AssetManager.instance().get(presetId);
			TileRegistry registry = AssetManager.instance().get("tiles/typed");
			TileGroupRegistry groups = AssetManager.instance().get(
				"tile-groups/default"
			);
			int[] subset = policy.selectFor(
				registry, groups, preset.types(), new Xoroshiro128PP(seed)
			);
			TilemapFactory inner = CustomizedTilemapFactory.fromPreset(
				preset, seed
			);
			return new SubsetTilemapFactory(inner, subset).generate();
		};
	}

	private static TileSelectionPolicy policyFor(String presetId) {
		String suffix = presetId.substring(presetId.lastIndexOf('/') + 1);
		return switch (suffix) {
			case "easy" -> TileSelectionPolicy.easy();
			case "hard" -> TileSelectionPolicy.hard();
			case "extreme" -> TileSelectionPolicy.extreme();
			default ->
				throw new IllegalArgumentException(
					"no default TileSelectionPolicy for preset: " + presetId
					+ " (use the explicit-policy overload)"
				);
		};
	}
}

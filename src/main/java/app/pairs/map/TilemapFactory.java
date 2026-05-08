package app.pairs.map;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

public interface TilemapFactory {
	Tilemap generate();

	static TilemapFactory fromPreset(String presetId) {
		return fromPreset(presetId, Seed.deviceRandom());
	}

	static TilemapFactory fromPreset(String presetId, Seed seed) {
		PresetConfig config = AssetManager.instance().get(presetId);
		return () -> {
			TileRegistry registry = AssetManager.instance().get("tiles/typed");
			TileGroupRegistry groups = AssetManager.instance().get(
				"tile-groups/default"
			);
			int[] subset = config.policy().selectFor(
				registry, groups, config.types(), new Xoroshiro128PP(seed)
			);
			TilemapFactory inner = CustomizedTilemapFactory.fromPreset(
				config.preset(), seed
			);
			Tilemap tilemap = new SubsetTilemapFactory(inner, subset)
								  .generate();
			tilemap.setDifficulty(config.difficulty());
			return tilemap;
		};
	}
}

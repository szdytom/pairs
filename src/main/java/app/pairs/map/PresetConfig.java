package app.pairs.map;

import app.pairs.model.Tilemap;

public record PresetConfig(
	TilemapPreset preset, Tilemap.Difficulty difficulty,
	TileSelectionPolicy policy
) {
	public int types() {
		return preset.types();
	}
}

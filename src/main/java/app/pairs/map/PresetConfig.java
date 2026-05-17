package app.pairs.map;

import app.pairs.model.TilemapType;

public record PresetConfig(
	TilemapPreset preset, TilemapType difficulty, TileSelectionPolicy policy
) {
	public int types() {
		return preset.types();
	}
}

package app.pairs.map;

import app.pairs.model.GameType;

public record PresetConfig(
	TilemapPreset preset, GameType difficulty, TileSelectionPolicy policy
) {
	public int types() {
		return preset.types();
	}
}

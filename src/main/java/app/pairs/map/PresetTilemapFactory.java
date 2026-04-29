package app.pairs.map;

import app.pairs.model.Tilemap;

import java.util.Random;

/** Tilemap factory driven by a {@link TilemapPreset}. */
public class PresetTilemapFactory implements TilemapFactory {
	private final TilemapPreset preset;
	private final Random random = new Random();

	public PresetTilemapFactory(TilemapPreset preset) {
		this.preset = preset;
	}

	@Override
	public Tilemap generate() {
		return TilemapGeneratorCore.generate(
			preset.buildInitial(), preset.types(), random
		);
	}
}

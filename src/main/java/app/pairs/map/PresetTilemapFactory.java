package app.pairs.map;

import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import java.util.Random;

/** Tilemap factory driven by a {@link TilemapPreset}. */
public class PresetTilemapFactory implements TilemapFactory {
	private final TilemapPreset preset;
	private Seed seed;
	private Random random;

	public PresetTilemapFactory(TilemapPreset preset) {
		this.preset = preset;
		setSeed(Seed.deviceRandom());
	}

	public PresetTilemapFactory(TilemapPreset preset, Seed seed) {
		this.preset = preset;
		setSeed(seed);
	}

	public PresetTilemapFactory(TilemapPreset preset, String seed) {
		this.preset = preset;
		setSeed(seed);
	}

	public Seed getSeed() {
		return seed;
	}

	/** Use a deterministic Xoroshiro128++ stream derived from the seed. */
	public PresetTilemapFactory setSeed(Seed seed) {
		this.seed = seed;
		this.random = new Xoroshiro128PP(seed);
		return this;
	}

	/** Use a deterministic stream derived from a string seed. */
	public PresetTilemapFactory setSeed(String seed) {
		return setSeed(Seed.fromString(seed));
	}

	@Override
	public Tilemap generate() {
		return TilemapGeneratorCore.generate(
			preset.buildInitial(), preset.types(), random
		);
	}
}

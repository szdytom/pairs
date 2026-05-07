package app.pairs.map;

import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

public class CustomizedTilemapFactory implements TilemapFactory {
	private static final int DEFAULT_WIDTH = 10;
	private static final int DEFAULT_HEIGHT = 10;
	private static final int DEFAULT_TYPES = 12;

	private final Seed seed;
	private TilemapPreset preset;

	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private int types = DEFAULT_TYPES;

	public CustomizedTilemapFactory(Seed seed) {
		this.seed = seed;
	}

	/** Build a factory whose layout is driven by the given preset. */
	public static CustomizedTilemapFactory fromPreset(
		TilemapPreset preset, Seed seed
	) {
		CustomizedTilemapFactory f = new CustomizedTilemapFactory(seed);
		f.preset = preset;
		f.types = preset.types();
		return f;
	}

	public CustomizedTilemapFactory setWidth(int width) {
		this.width = width;
		return this;
	}

	public CustomizedTilemapFactory setHeight(int height) {
		this.height = height;
		return this;
	}

	public CustomizedTilemapFactory setTypes(int types) {
		this.types = types;
		return this;
	}

	@Override
	public Tilemap generate() {
		int[][] map = buildLegalPlacementShape();
		var strategy = preset != null ? preset.pairingStrategy()
									  : new BasePairingStrategy();
		return TilemapGeneratorCore.generate(
			map, types, new Xoroshiro128PP(seed), strategy
		);
	}

	@Override
	public int[][] buildLegalPlacementShape() {
		return (preset != null) ? preset.buildInitial()
								: new int[height][width];
	}
}

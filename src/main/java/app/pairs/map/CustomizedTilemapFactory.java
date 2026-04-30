package app.pairs.map;

import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import java.util.Random;

public class CustomizedTilemapFactory implements TilemapFactory {
	private static final int DEFAULT_WIDTH = 10;
	private static final int DEFAULT_HEIGHT = 10;
	private static final int DEFAULT_TYPES = 12;

	private Seed seed;
	private Random random;

	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private int types = DEFAULT_TYPES;

	public CustomizedTilemapFactory() {
		setSeed(Seed.deviceRandom());
	}

	public CustomizedTilemapFactory(Seed seed) {
		setSeed(seed);
	}

	public CustomizedTilemapFactory(String seed) {
		setSeed(seed);
	}

	public Seed getSeed() {
		return seed;
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

	/** Use a deterministic Xoroshiro128++ stream derived from the seed. */
	public CustomizedTilemapFactory setSeed(Seed seed) {
		this.seed = seed;
		this.random = new Xoroshiro128PP(seed);
		return this;
	}

	/** Use a deterministic stream derived from a string seed. */
	public CustomizedTilemapFactory setSeed(String seed) {
		return setSeed(Seed.fromString(seed));
	}

	@Override
	public Tilemap generate() {
		int[][] map = new int[height][width];
		return TilemapGeneratorCore.generate(map, types, random);
	}
}

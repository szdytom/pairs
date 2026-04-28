package app.pairs.map;

import app.pairs.model.Tilemap;

import java.util.Random;

public class CustomizedTilemapFactory implements TilemapFactory {
	private static final int DEFAULT_WIDTH = 10;
	private static final int DEFAULT_HEIGHT = 10;
	private static final int DEFAULT_TYPES = 12;

	private final Random random = new Random();

	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private int types = DEFAULT_TYPES;

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
		int[][] map = new int[height][width];
		return TilemapGeneratorCore.generate(map, types, random);
	}
}

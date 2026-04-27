package app.pairs.map;

import java.util.Random;

import app.pairs.model.Tilemap;

public class HardTilemapFactory implements TilemapFactory {
	private static final int WIDTH = 10;
	private static final int HEIGHT = 10;
	private static final int TYPES = 12;

	private final Random random = new Random();

	@Override
	public Tilemap generate() {
		int[][] map = new int[HEIGHT][WIDTH];
		return TilemapGeneratorCore.generate(map, TYPES, random);
	}
}

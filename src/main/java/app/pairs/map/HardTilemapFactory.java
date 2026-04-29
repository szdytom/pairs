package app.pairs.map;

import app.pairs.model.Tilemap;

import java.util.Random;

public class HardTilemapFactory implements TilemapFactory {
	private static final int WIDTH = 12;
	private static final int HEIGHT = 12;
	private static final int TYPES = 20;

	private final Random random = new Random();

	@Override
	public Tilemap generate() {
		int[][] map = new int[HEIGHT][WIDTH];
		return TilemapGeneratorCore.generate(map, TYPES, random);
	}
}

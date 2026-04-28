package app.pairs.map;

import app.pairs.model.Tilemap;

import java.util.Random;

public class EasyTilemapFactory implements TilemapFactory {
	private static final int WIDTH = 9;
	private static final int HEIGHT = 9;
	private static final int TYPES = 6;

	private final Random random = new Random();

	@Override
	public Tilemap generate() {
		int[][] map = new int[HEIGHT][WIDTH];
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				map[i][j] = -1;
			}
		}
		for (int i = 0; i <= 3; i++) {
			for (int j = 0; j <= 3; j++) {
				map[i][j] = 0;
			}
		}
		for (int i = map.length - 4; i <= map.length - 1; i++) {
			for (int j = map[i].length - 4; j <= map[i].length - 1; j++) {
				map[i][j] = 0;
			}
		}
		return TilemapGeneratorCore.generate(map, TYPES, random);
	}
}

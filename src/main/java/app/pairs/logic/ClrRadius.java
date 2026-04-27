package app.pairs.logic;

import app.pairs.model.Tilemap;

public final class ClrRadius {
	private ClrRadius() {}

	public static int getUr(Tilemap map, int row, int col) {
		int i = row;
		while (i + 1 < map.getHeight() && map.getTile(i + 1, col) <= 0) {
			i++;
		}
		if (i + 1 == map.getHeight()) {
			i++;
		}
		return i;
	}

	public static int getLor(Tilemap map, int row, int col) {
		int i = row;
		while (i - 1 >= 0 && map.getTile(i - 1, col) <= 0) {
			i--;
		}
		if (i == 0) {
			i--;
		}
		return i;
	}

	public static int getUc(Tilemap map, int row, int col) {
		int i = col;
		while (i + 1 < map.getWidth() && map.getTile(row, i + 1) <= 0) {
			i++;
		}
		if (i + 1 == map.getWidth()) {
			i++;
		}
		return i;
	}

	public static int getLoc(Tilemap map, int row, int col) {
		int i = col;
		while (i - 1 >= 0 && map.getTile(row, i - 1) <= 0) {
			i--;
		}
		if (i == 0) {
			i--;
		}
		return i;
	}
}

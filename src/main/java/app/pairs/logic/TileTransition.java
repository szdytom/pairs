package app.pairs.logic;

import app.pairs.model.Tilemap;

public final class TileTransition {
	private TileTransition() {}

	public static boolean transition(
		Tilemap map, int startRow, int startCol, int targetRow, int targetCol
	) {
		if (map.getTile(startRow, startCol)
		    != map.getTile(targetRow, targetCol)) {
			return false;
		}
		int lr = Math.min(startRow, targetRow);
		int rr = Math.max(startRow, targetRow);
		int lc = Math.min(startCol, targetCol);
		int rc = Math.max(startCol, targetCol);
		int ur = Math.min(
			ClrRadius.getUr(map, startRow, startCol),
			ClrRadius.getUr(map, targetRow, targetCol)
		);
		int lor = Math.max(
			ClrRadius.getLor(map, startRow, startCol),
			ClrRadius.getLor(map, targetRow, targetCol)
		);
		int uc = Math.min(
			ClrRadius.getUc(map, startRow, startCol),
			ClrRadius.getUc(map, targetRow, targetCol)
		);
		int loc = Math.max(
			ClrRadius.getLoc(map, startRow, startCol),
			ClrRadius.getLoc(map, targetRow, targetCol)
		);
		for (int i = lor; i <= ur; i++) {
			if (isRowClear(map, i, lc, rc)) {
				return true;
			}
		}
		for (int i = loc; i <= uc; i++) {
			if (isColClear(map, i, lr, rr)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isRowClear(
		Tilemap map, int row, int colStart, int colEnd
	) {
		if (row < 0 || row >= map.getHeight()) {
			return true;
		}
		int left = Math.min(colStart, colEnd);
		int right = Math.max(colStart, colEnd);
		for (int col = left + 1; col < right; col++) {
			if (map.getTile(row, col) > 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean isColClear(
		Tilemap map, int col, int rowStart, int rowEnd
	) {
		if (col < 0 || col >= map.getWidth()) {
			return true;
		}
		int top = Math.min(rowStart, rowEnd);
		int bottom = Math.max(rowStart, rowEnd);
		for (int row = top + 1; row < bottom; row++) {
			if (map.getTile(row, col) > 0) {
				return false;
			}
		}
		return true;
	}
}

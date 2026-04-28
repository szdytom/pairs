package app.pairs.logic;

import java.util.function.Consumer;

import app.pairs.model.Tilemap;

public class OpElimination implements Operation {
	private final Tilemap tilemap;
	private final int row1;
	private final int col1;
	private final int row2;
	private final int col2;
	private final int tileId;
	private final Consumer<Operation> pushFn;
	// `executed` is true after `operate()` and false after `undo()`. Calling
	// `operate()` twice in a row or `undo()` before `operate()` is illegal and
	// indicates a caller bug (e.g. replaying a history entry). Re-executing
	// after a successful undo is supported.
	private boolean executed;

	public OpElimination(
			Tilemap tilemap, int row1, int col1, int row2, int col2,
			Consumer<Operation> pushFn) {
		int t1 = tilemap.getTile(row1, col1);
		int t2 = tilemap.getTile(row2, col2);
		if (t1 <= 0 || t1 != t2) {
			throw new IllegalStateException(
					"OpElimination requires two equal non-empty tiles, got: ("
							+ row1 + "," + col1 + ")=" + t1 + ", (" + row2 + "," + col2
							+ ")=" + t2);
		}
		this.tilemap = tilemap;
		this.row1 = row1;
		this.col1 = col1;
		this.row2 = row2;
		this.col2 = col2;
		this.tileId = t1;
		this.pushFn = pushFn;
	}

	@Override
	public void operate() {
		if (executed) {
			throw new IllegalStateException(
					"OpElimination already executed; replaying history entries is"
							+ " not allowed");
		}
		tilemap.setTile(row1, col1, 0);
		tilemap.setTile(row2, col2, 0);
		executed = true;
		pushFn.accept(this);
	}

	@Override
	public void undo() {
		if (!executed) {
			throw new IllegalStateException(
					"OpElimination has not been executed; nothing to undo");
		}
		tilemap.setTile(row1, col1, tileId);
		tilemap.setTile(row2, col2, tileId);
		executed = false;
	}

	public int getRow1() {
		return row1;
	}

	public int getCol1() {
		return col1;
	}

	public int getRow2() {
		return row2;
	}

	public int getCol2() {
		return col2;
	}

	public int getTileId() {
		return tileId;
	}
}

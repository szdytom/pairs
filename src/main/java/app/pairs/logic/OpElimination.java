package app.pairs.logic;

import app.pairs.model.Tilemap;

import java.util.function.Consumer;

public class OpElimination implements Operation {
	private final Tilemap tilemap;
	private final int row1;
	private final int col1;
	private final int row2;
	private final int col2;
	private final int tileId;
	private final Consumer<Operation> pushFn;

	public OpElimination(
		Tilemap tilemap, int row1, int col1, int row2, int col2,
		Consumer<Operation> pushFn
	) {
		this.tilemap = tilemap;
		this.row1 = row1;
		this.col1 = col1;
		this.row2 = row2;
		this.col2 = col2;
		this.tileId = tilemap.getTile(row1, col1);
		this.pushFn = pushFn;
	}

	@Override
	public void operate() {
		tilemap.setTile(row1, col1, 0);
		tilemap.setTile(row2, col2, 0);
		pushFn.accept(this);
	}

	@Override
	public void undo() {
		tilemap.setTile(row1, col1, tileId);
		tilemap.setTile(row2, col2, tileId);
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

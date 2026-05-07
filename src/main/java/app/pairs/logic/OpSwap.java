package app.pairs.logic;
import app.pairs.model.Tilemap;

import java.util.function.Consumer;

public class OpSwap implements Operation {
	private final int row1;
	private final int col1;
	private final int row2;
	private final int col2;
	private final Tilemap tilemap;
	private final java.util.function.Consumer<Operation> pushFn;
	public OpSwap(
		Tilemap tilemap, int row1, int col1, int row2, int col2,
		Consumer<Operation> pushFn
	) {
		int t1 = tilemap.getTile(row1, col1);
		int t2 = tilemap.getTile(row2, col2);
		if (t1 <= 0 || t1 != t2) {
			throw new IllegalStateException(
				"OpElimination requires two equal non-empty tiles, got: ("
				+ row1 + "," + col1 + ")=" + t1 + ", (" + row2 + "," + col2
				+ ")=" + t2
			);
		}
		this.tilemap = tilemap;
		this.row1 = row1;
		this.col1 = col1;
		this.row2 = row2;
		this.col2 = col2;
		this.pushFn = pushFn;
	}
	@Override
	public void operate() {
		int t1 = tilemap.getTile(row1, col1);
		int t2 = tilemap.getTile(row2, col2);
		tilemap.setTile(row1, col1, t2);
		tilemap.setTile(row2, col2, t1);
		pushFn.accept(this);
	}

	@Override
	public void undo() {
		operate();
	}
}

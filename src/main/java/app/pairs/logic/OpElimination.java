package app.pairs.logic;

import app.pairs.model.GameStatus;
import app.pairs.model.Tilemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class OpElimination implements Operation {
	public static final int SCORE_PER_PAIR = 500;

	private final GameStatus gameStatus;
	private final Tilemap tilemap;
	private final int row1;
	private final int col1;
	private final int row2;
	private final int col2;
	private final int tileId;
	private final int time;
	private final ArrayList<Integer> path;
	private final Consumer<Operation> pushFn;
	private int deltaScore;
	// `executed` is true after `operate()` and false after `undo()`. Calling
	// `operate()` twice in a row or `undo()` before `operate()` is illegal and
	// indicates a caller bug (e.g. replaying a history entry). Re-executing
	// after a successful undo is supported.
	private boolean executed;

	public OpElimination(
		GameStatus gameStatus, Tilemap tilemap, int row1, int col1, int row2,
		int col2, int time, Consumer<Operation> pushFn
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
		this.gameStatus = gameStatus;
		this.tilemap = tilemap;
		this.row1 = row1;
		this.col1 = col1;
		this.row2 = row2;
		this.col2 = col2;
		this.tileId = t1;
		this.time = time;
		this.path = Path.path(tilemap, row1, col1, row2, col2);
		this.pushFn = pushFn;
	}

	@Override
	public void operate() {
		if (executed) {
			throw new IllegalStateException(
				"OpElimination already executed; replaying history entries is"
				+ " not allowed"
			);
		}
		tilemap.setTile(row1, col1, 0);
		tilemap.setTile(row2, col2, 0);
		executed = true;
		pushFn.accept(this);
		switch (tilemap.getDifficulty()) {
		case EASY -> deltaScore = SCORE_PER_PAIR;
		case HARD -> deltaScore = SCORE_PER_PAIR * 2;
		case EXTREME -> deltaScore = SCORE_PER_PAIR * 3;
		default -> deltaScore = SCORE_PER_PAIR;
		}
		deltaScore *= Math.max(1, 5 - time / 1_000);
		gameStatus.changeScore(deltaScore);
	}

	@Override
	public void undo() {
		if (!executed) {
			throw new IllegalStateException(
				"OpElimination has not been executed; nothing to undo"
			);
		}
		tilemap.setTile(row1, col1, tileId);
		tilemap.setTile(row2, col2, tileId);
		executed = false;
		gameStatus.changeScore(-deltaScore);
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

	public List<Integer> getPath() {
		return Collections.unmodifiableList(path);
	}

	public int getTime() {
		return time;
	}
}

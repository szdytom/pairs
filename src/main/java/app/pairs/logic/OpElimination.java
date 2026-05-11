package app.pairs.logic;

import java.util.Collections;
import java.util.List;

public class OpElimination {
	public static final int SCORE_PER_PAIR = 500;

	private final int row1;
	private final int col1;
	private final int row2;
	private final int col2;
	private final int tileId;
	private final int time;
	private final List<Integer> path;
	private final int deltaScore;

	public OpElimination(
		int tileId, int row1, int col1, int row2, int col2, int time,
		List<Integer> path, int deltaScore
	) {
		if (tileId <= 0) {
			throw new IllegalArgumentException(
				"tileId must be positive: " + tileId
			);
		}
		this.tileId = tileId;
		this.row1 = row1;
		this.col1 = col1;
		this.row2 = row2;
		this.col2 = col2;
		this.time = time;
		this.path = Collections.unmodifiableList(path);
		this.deltaScore = deltaScore;
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
		return path;
	}

	public int getTime() {
		return time;
	}

	public int getDeltaScore() {
		return deltaScore;
	}
}

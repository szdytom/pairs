package app.pairs.save;

import app.pairs.model.*;

public record LeaderBoardEntry(
	String username, GameType difficulty, long score, long updatedAt
) implements Comparable<LeaderBoardEntry> {
	@Override
	public int compareTo(LeaderBoardEntry other) {
		int cmp = Long.compare(other.score(), this.score()); // descending
		if (cmp != 0)
			return cmp;
		return Long.compare(
			this.updatedAt(), other.updatedAt()
		); // earlier first
	}
}

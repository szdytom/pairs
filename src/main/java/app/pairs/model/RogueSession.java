package app.pairs.model;

public class RogueSession {
	public static final long TOTAL_TIME_MS = 10 * 60 * 1000; // 10 minutes

	public long remainingMs = TOTAL_TIME_MS;
	public int totalScore;
	public int level;
	public final ItemCountMap items = new ItemCountMap();

	public boolean isTimeUp() {
		return remainingMs <= 0;
	}
}

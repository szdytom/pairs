package app.pairs.model;

public class GameStatus {
	public int score;
	public int combo;
	public long remainingMs;
	public final ItemCountMap items = new ItemCountMap();

	public GameStatus() {
		this.score = 0;
		items.set(ItemType.AUTO_SOLVER, 0);
		items.set(ItemType.TNT, 0);
	}

	public void changeScore(int delta) {
		score += delta;
	}

	public void reset() {
		score = 0;
		combo = 0;
	}

	public int getCount(ItemType type) {
		return items.get(type);
	}

	public boolean reduceItem(ItemType type) {
		return items.reduce(type);
	}
}

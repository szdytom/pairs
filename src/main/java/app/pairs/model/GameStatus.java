package app.pairs.model;

public class GameStatus {
	public int score;
	public final ItemCountMap items = new ItemCountMap();

	public GameStatus() {
		this.score = 0;
		items.set(ItemType.AUTO_SOLVER, 1);
		items.set(ItemType.TNT, 1);
	}

	public void changeScore(int delta) {
		score += delta;
	}

	public void reset() {
		score = 0;
	}

	public int getCount(ItemType type) {
		return items.get(type);
	}

	public boolean reduceItem(ItemType type) {
		return items.reduce(type);
	}
}

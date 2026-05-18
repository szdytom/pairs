package app.pairs.model;

public class GameStatus {
	public int score;
	public int combo;
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

	public GameStatusSnapshot toSnapshot() {
		return new GameStatusSnapshot(score, combo, items.toSnapshot());
	}

	public static GameStatus fromSnapshot(GameStatusSnapshot snapshot) {
		GameStatus status = new GameStatus();
		if (snapshot == null) {
			return status;
		}
		status.score = snapshot.score();
		status.combo = snapshot.combo();
		status.items.restore(snapshot.items());
		return status;
	}
}

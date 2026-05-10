package app.pairs.model;

public class GameStatus {
	public int score;
	public int autoSolvers;

	public GameStatus() {
		this.score = 0;
		this.autoSolvers = 1;
	}

	public void changeScore(int delta) {
		score += delta;
	}
	public void reset() {
		score = 0;
	}

	public int getCount(ItemType type) {
		return switch (type) {
			case AUTO_SOLVER -> autoSolvers;
		};
	}

	public boolean reduceItem(ItemType type) {
		int count = getCount(type);
		if (count <= 0) {
			return false;
		}
		switch (type) {
		case AUTO_SOLVER -> autoSolvers--;
		}
		return true;
	}
}

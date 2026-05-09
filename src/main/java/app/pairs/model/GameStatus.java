package app.pairs.model;

public class GameStatus {
	public int score;
	public boolean timeFrozen = false;

	private int swap = 0;
	private int repermute = 0;
	private int autosolve = 0;
	private int timeFreezer = 0;

	public GameStatus() {
		this.score = 0;
	}

	public void changeScore(int delta) {
		score += delta;
	}

	public int getSwap() {
		return swap;
	}
	public int getRepermute() {
		return repermute;
	}
	public int getAutosolve() {
		return autosolve;
	}
	public int getTimeFreezer() {
		return timeFreezer;
	}

	public void addItem(ItemType type, int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("count must be positive");
		}
		switch (type) {
		case SWAP -> swap += count;
		case REPERMUTE -> repermute += count;
		case AUTOSOLVE -> autosolve += count;
		case TIMEFREEZER -> timeFreezer += count;
		}
	}

	public void requireItem(ItemType type) {
		switch (type) {
		case SWAP -> {
			if (swap <= 0) {
				throw new IllegalStateException("No swap item");
			}
		}
		case REPERMUTE -> {
			if (repermute <= 0) {
				throw new IllegalStateException("No repermute item");
			}
		}
		case AUTOSOLVE -> {
			if (autosolve <= 0) {
				throw new IllegalStateException("No autosolve item");
			}
		}
		case TIMEFREEZER -> {
			if (timeFreezer <= 0) {
				throw new IllegalStateException("No time freezer item");
			}
		}
		}
	}

	public void consumeItem(ItemType type) {
		requireItem(type);
		switch (type) {
		case SWAP -> swap--;
		case REPERMUTE -> repermute--;
		case AUTOSOLVE -> autosolve--;
		case TIMEFREEZER -> timeFreezer--;
		}
	}

	public ItemType canRevive() {
		if (swap > 0) {
			return ItemType.SWAP;
		}
		if (repermute > 0) {
			return ItemType.REPERMUTE;
		}
		return null;
	}
}

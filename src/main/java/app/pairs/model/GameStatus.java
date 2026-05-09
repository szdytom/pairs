package app.pairs.model;

public class GameStatus {
	public int score;
	public boolean timeFrozen = false;

	private int swap = 0;
	private int repromute = 0;
	private int autosolve = 0;
	private int timefrozer = 0;

	public GameStatus() {
		this.score = 0;
	}

	public void changeScore(int delta) {
		score += delta;
	}

	public int getSwap() {
		return swap;
	}
	public int getRepromute() {
		return repromute;
	}
	public int getAutosolve() {
		return autosolve;
	}
	public int getTimefrozer() {
		return timefrozer;
	}

	public void addItem(ItemType type, int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("count must be positive");
		}
		switch (type) {
		case SWAP -> swap += count;
		case REPROMUTE -> repromute += count;
		case AUTOSOLVE -> autosolve += count;
		case TIMEFROZER -> timefrozer += count;
		}
	}

	public void consumeItem(ItemType type) {
		switch (type) {
		case SWAP -> {
			if (swap <= 0)
				throw new IllegalStateException("No swap item");
			swap--;
		}
		case REPROMUTE -> {
			if (repromute <= 0)
				throw new IllegalStateException("No repromute item");
			repromute--;
		}
		case AUTOSOLVE -> {
			if (autosolve <= 0)
				throw new IllegalStateException("No autosolve item");
			autosolve--;
		}
		case TIMEFROZER -> {
			if (timefrozer <= 0)
				throw new IllegalStateException("No timefrozer item");
			timefrozer--;
		}
		}
	}

	public ItemType canRevive() {
		if (swap > 0) {
			return ItemType.SWAP;
		}
		if (repromute > 0) {
			return ItemType.REPROMUTE;
		}
		return null;
	}
}

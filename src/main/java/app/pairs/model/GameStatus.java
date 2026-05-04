package app.pairs.model;

public class GameStatus {
	private int score;

	public GameStatus() {
		this.score = 0;
	}

	public int getScore() {
		return score;
	}

	public void changeScore(int delta) {
		score += delta;
	}
}

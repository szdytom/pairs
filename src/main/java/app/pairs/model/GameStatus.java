package app.pairs.model;

public class GameStatus {
	public int score;

	public GameStatus() {
		this.score = 0;
	}

	public void changeScore(int delta) {
		score += delta;
	}
}

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
}

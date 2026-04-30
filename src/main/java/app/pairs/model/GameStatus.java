package app.pairs.model;

public class GameStatus {
	private int score;
	private String difficulty;

	public GameStatus() {
		this.score = 0;
		this.difficulty = "Normal";
	}

	public int getScore() {
		return score;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void changeScore(int delta) {
		score += delta;
	}
}

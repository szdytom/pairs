package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.logic.GameState;
import app.pairs.model.CountdownState;

public class LevelInfo extends GridLayout {
	private static final int PADDING = 4;
	private static final int TEXT_SIZE = 1;
	private static final int COLOR = rgb(120, 120, 120);

	private final TextComponent timeLabel;
	private final TextComponent timeValue;
	private final TextComponent scoreLabel;
	private final TextComponent scoreValue;
	private int lastScore = -1;

	public LevelInfo() {
		super(2, 0, 0, PADDING);
		this.timeLabel = new TextComponent("Time", TEXT_SIZE, COLOR);
		this.timeValue = new TextComponent("--:--.--", TEXT_SIZE, COLOR);
		this.scoreLabel = new TextComponent("Score", TEXT_SIZE, COLOR);
		this.scoreValue = new TextComponent("0", TEXT_SIZE, COLOR);
		addChild(timeLabel);
		addChild(timeValue);
		addChild(scoreLabel);
		addChild(scoreValue);
	}

	@Override
	public void update(long deltaTimeMs) {
		super.update(deltaTimeMs);

		CountdownState countdown = blackboard().get(CountdownState.class);
		GameState gameState = blackboard().get(GameState.class);

		long mins = countdown.remainingMs / 60_000;
		long secs = (countdown.remainingMs % 60_000) / 1_000;
		long centis = (countdown.remainingMs % 1_000) / 10;
		timeValue.setText(String.format("%02d:%02d.%02d", mins, secs, centis));

		boolean changed = false;

		int score = gameState.gameStatus.score;
		if (score != lastScore) {
			scoreValue.setText(formatScore(score));
			lastScore = score;
			changed = true;
		}

		if (changed) {
			blackboard().layoutDirty = true;
		}
	}

	private static String formatScore(int score) {
		if (score >= 1_000_000) {
			int m = score / 1_000_000;
			int d = (score / 100_000) % 10;
			return m + "." + d + "M";
		}
		if (score >= 1_000) {
			int k = score / 1_000;
			int d = (score / 100) % 10;
			return k + "." + d + "K";
		}
		return Integer.toString(score);
	}
}

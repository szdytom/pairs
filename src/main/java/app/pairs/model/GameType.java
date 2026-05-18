package app.pairs.model;

public enum GameType {
	EASY,
	HARD,
	EXTREME,
	NORMAL,
	ROGUE,
	CUSTOMIZE;

	public static GameType from(Tilemap.Difficulty difficulty) {
		return switch (difficulty) {
			case EASY -> EASY;
			case HARD -> HARD;
			case EXTREME -> EXTREME;
			case NORMAL -> NORMAL;
		};
	}
}

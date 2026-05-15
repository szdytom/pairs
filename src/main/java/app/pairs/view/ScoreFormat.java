package app.pairs.view;

public class ScoreFormat {
	public static String format(int score) {
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

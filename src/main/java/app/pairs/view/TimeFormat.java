package app.pairs.view;

public class TimeFormat {
	public static String format(long ms) {
		long mins = ms / 60_000;
		long secs = (ms % 60_000) / 1_000;
		return String.format("%02d:%02d", mins, secs);
	}

	public static String formatWithCentis(long ms) {
		long mins = ms / 60_000;
		long secs = (ms % 60_000) / 1_000;
		long centis = (ms % 1_000) / 10;
		return String.format("%02d:%02d.%02d", mins, secs, centis);
	}
}

package app.pairs.model;

public class CountdownState {
	public long remainingMs;
	private long pausedAtMs;
	private long totalPausedMs;

	public void pause() {
		if (pausedAtMs != 0)
			return;
		pausedAtMs = System.currentTimeMillis();
	}

	public void resume() {
		if (pausedAtMs == 0)
			return;
		totalPausedMs += System.currentTimeMillis() - pausedAtMs;
		pausedAtMs = 0;
	}

	public boolean isPaused() {
		return pausedAtMs != 0;
	}

	/**
	 * Returns the "effective" current time with all pause durations
	 * subtracted.
	 */
	public long now() {
		long currentPause = pausedAtMs != 0
			? System.currentTimeMillis() - pausedAtMs
			: 0;
		return System.currentTimeMillis() - totalPausedMs - currentPause;
	}

	public void resetPause() {
		pausedAtMs = 0;
		totalPausedMs = 0;
	}
}

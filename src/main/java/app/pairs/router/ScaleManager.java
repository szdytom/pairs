package app.pairs.router;

public class ScaleManager {
	private static final int MIN_SCALE = 1;
	private static final int MAX_SCALE = 12;
	private static final int TARGET_WIDTH = 320;
	private static final int TARGET_HEIGHT = 200;
	private static final long DEBOUNCE_MS = 200;

	private int scale;
	private int autoScale;
	private int zoomOffset;
	private long lastLogTime;

	public void updateAutoScale(int windowWidth, int windowHeight) {
		autoScale = Math.max(
			MIN_SCALE,
			Math.min(
				MAX_SCALE,
				Math.min(
					windowWidth / TARGET_WIDTH,
					// Allow height insufficiency
					windowHeight / (TARGET_HEIGHT * 19 / 20)
				)
			)
		);
		updateEffectiveScale();
	}

	public void zoomIn() {
		zoomOffset++;
		updateEffectiveScale();
	}

	public void zoomOut() {
		zoomOffset--;
		updateEffectiveScale();
	}

	public void zoomReset() {
		zoomOffset = 0;
		updateEffectiveScale();
	}

	public int getScale() {
		return scale;
	}

	public int getAutoScale() {
		return autoScale;
	}

	public int getZoomOffset() {
		return zoomOffset;
	}

	public boolean logScale(int windowWidth, int windowHeight) {
		long now = System.currentTimeMillis();
		if (now - lastLogTime < DEBOUNCE_MS) {
			return false;
		}
		lastLogTime = now;
		System.out.printf(
			"Scale: %d (auto %d %s%d) logical %dx%d%n", scale, autoScale,
			zoomOffset >= 0 ? "+" : "", zoomOffset,
			(windowWidth + scale - 1) / scale,
			(windowHeight + scale - 1) / scale
		);
		return true;
	}

	private void updateEffectiveScale() {
		scale = Math.max(
			MIN_SCALE, Math.min(MAX_SCALE, autoScale + zoomOffset)
		);
	}
}

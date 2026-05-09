package app.pairs.view;

import java.util.ArrayDeque;

class AutoPairingState {
	private final ArrayDeque<AutoPairingStep> steps = new ArrayDeque<>();

	void push(AutoPairingStep step) {
		steps.add(step);
	}

	boolean isActive() {
		return !steps.isEmpty();
	}

	void update(long deltaTimeMs) {
		if (steps.isEmpty()) {
			return;
		}
		AutoPairingStep current = steps.peek();
		if (current.update(deltaTimeMs)) {
			steps.poll();
		}
	}

	void clear() {
		steps.clear();
	}

	static class WaitStep implements AutoPairingStep {
		private final long durationMs;
		private long elapsed;

		WaitStep(long durationMs) {
			this.durationMs = durationMs;
		}

		@Override
		public boolean update(long deltaTimeMs) {
			elapsed += deltaTimeMs;
			return elapsed >= durationMs;
		}
	}
}

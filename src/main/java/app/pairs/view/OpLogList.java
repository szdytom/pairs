package app.pairs.view;

import app.pairs.logic.GameState;
import app.pairs.logic.OpElimination;
import app.pairs.logic.Operation;

import java.util.List;

/**
 * Displays the chronological list of {@link Operation eliminations} read
 * directly from {@link GameState} via the widget hierarchy's {@link
 * Blackboard}.
 *
 * <p>Arranges entries in a single-column {@link GridLayout}. Syncs when
 * a {@link Event.Type#GAME_STATE_UPDATED} event is received.
 */
public class OpLogList extends GridLayout {
	private int lastOpCount = -1;

	public OpLogList() {
		super(1, 0, 0);
	}

	@Override
	public void update(long deltaTimeMs) {
		super.update(deltaTimeMs);
		if (lastOpCount < 0) {
			sync();
		}
	}

	@Override
	public boolean onEvent(Event event) {
		if (event.type() == Event.Type.GAME_STATE_UPDATED) {
			sync();
			return true;
		}
		return false;
	}

	private void sync() {
		GameState gameState = blackboard().get(GameState.class);
		List<Operation> ops = gameState.getOpLogs();

		if (ops.size() == lastOpCount) {
			return;
		}
		if (ops.size() < lastOpCount || lastOpCount < 0) {
			removeAllChildren();
			lastOpCount = 0;
		}

		for (int i = lastOpCount; i < ops.size(); i++) {
			Operation op = ops.get(i);
			if (op instanceof OpElimination e) {
				addChild(new OpLogEntry(
					e.getTileId(), e.getPath(), gameState.getHeight(),
					gameState.getWidth()
				));
			}
		}

		lastOpCount = ops.size();
		blackboard().layoutDirty = true;
	}
}

package app.pairs.view;

import app.pairs.logic.GameState;
import app.pairs.logic.OpElimination;

import java.util.List;

/**
 * Displays the chronological list of eliminations read directly from
 * {@link GameState} via the widget hierarchy's {@link Blackboard}.
 *
 * <p>Arranges entries in a vertically scrolling list. Syncs when a
 * {@link Event.Type#GAME_STATE_UPDATED} event is received.
 */
public class OpLogList extends ScrollListLayout {
	private int lastOpCount = -1;

	public OpLogList() {
		super(0, 0);
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
		List<OpElimination> ops = gameState.getOpLogs();

		if (ops.size() == lastOpCount) {
			return;
		}

		boolean atBottom = isAtBottom();

		if (ops.size() < lastOpCount || lastOpCount < 0) {
			removeAllChildren();
			lastOpCount = 0;
			atBottom = true;
		}

		for (int i = lastOpCount; i < ops.size(); i++) {
			OpElimination op = ops.get(i);
			addChild(new OpLogEntry(
				i + 1, op.getTileId(), op.getPath(), gameState.getHeight(),
				gameState.getWidth(), op.getTime()
			));
		}

		lastOpCount = ops.size();

		if (atBottom) {
			scrollToBottom();
		}
		blackboard().layoutDirty = true;
	}
}

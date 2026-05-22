package app.pairs.save;

import app.pairs.logic.GameState;
import app.pairs.model.RogueSession;

public record RogueLoadData(RogueSession session, GameState gameState) {
	public boolean hasMap() {
		return gameState != null;
	}
}

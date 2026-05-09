package app.pairs.logic;
import app.pairs.model.GameStatus;
public class OpTimeFrozer {
	private final GameStatus gameStatus;
	public OpTimeFrozer(GameStatus gameStatus) {
		this.gameStatus = gameStatus;
	}
	public void operate() {
		gameStatus.timeFrozen = true;
	}
}

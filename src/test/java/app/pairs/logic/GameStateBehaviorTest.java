package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.model.GameStatus;
import app.pairs.model.Tilemap;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class GameStateBehaviorTest {
	// `GameState` constructors only expose factory presets; behavior tests
	// drive `OpElimination` directly against a controlled `Tilemap` so the
	// outcomes are deterministic.

	@Test
	void operateClearsTilesAndUndoRestores() {
		int[][] map = {{1, 0, 1}};
		Tilemap tm = new Tilemap(map);
		OpElimination op = new OpElimination(
			new GameStatus(), tm, 0, 0, 0, 2, 5_000, ignored -> {}
		);

		op.operate();
		assertThat(tm.getTile(0, 0)).isZero();
		assertThat(tm.getTile(0, 2)).isZero();

		op.undo();
		assertThat(tm.getTile(0, 0)).isEqualTo(1);
		assertThat(tm.getTile(0, 2)).isEqualTo(1);
	}

	@Test
	void doubleOperateThrows() {
		Tilemap tm = new Tilemap(new int[][] {{1, 0, 1}});
		OpElimination op = new OpElimination(
			new GameStatus(), tm, 0, 0, 0, 2, 5_000, ignored -> {}
		);
		op.operate();
		assertThatThrownBy(op::operate)
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void undoBeforeOperateThrows() {
		Tilemap tm = new Tilemap(new int[][] {{1, 0, 1}});
		OpElimination op = new OpElimination(
			new GameStatus(), tm, 0, 0, 0, 2, 5_000, ignored -> {}
		);
		assertThatThrownBy(op::undo).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void constructionRejectsMismatchedTiles() {
		Tilemap tm = new Tilemap(new int[][] {{1, 0, 2}});
		Consumer<Operation> noop = ignored -> {};
		GameStatus gs = new GameStatus();
		assertThatThrownBy(
			() -> new OpElimination(gs, tm, 0, 0, 0, 2, 5_000, noop)
		)
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void constructionRejectsEmptyTiles() {
		Tilemap tm = new Tilemap(new int[][] {{0, 0, 0}});
		Consumer<Operation> noop = ignored -> {};
		GameStatus gs = new GameStatus();
		assertThatThrownBy(
			() -> new OpElimination(gs, tm, 0, 0, 0, 2, 5_000, noop)
		)
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void transitionRejectsEmptyEndpoint() {
		Tilemap tm = new Tilemap(new int[][] {{0, 0, 1}});
		assertThatThrownBy(() -> TileTransition.transition(tm, 0, 0, 0, 1))
			.isInstanceOf(IllegalStateException.class);
	}

	// ---- `GameState` facade tests ----------------------------------------
	// `customized(2, 2, 1)` deterministically yields a 2x2 board where every
	// cell holds tile id 1, so any pair is eliminable.

	@Test
	void gameStateOperateClearsAndLogs() {
		GameState state = GameState.customized(2, 2, 1);
		assertThat(state.getOpLogs()).isEmpty();

		state.operate(0, 0, 0, 1, 5_000);
		assertThat(state.getTile(0, 0)).isZero();
		assertThat(state.getTile(0, 1)).isZero();
		assertThat(state.getOpLogs()).hasSize(1);
	}

	@Test
	void gameStateUndoRestoresAndPops() {
		GameState state = GameState.customized(2, 2, 1);
		state.operate(0, 0, 0, 1, 5_000);

		state.undo();
		assertThat(state.getTile(0, 0)).isEqualTo(1);
		assertThat(state.getTile(0, 1)).isEqualTo(1);
		assertThat(state.getOpLogs()).isEmpty();
	}

	@Test
	void gameStateUndoOnEmptyHistoryThrows() {
		GameState state = GameState.customized(2, 2, 1);
		assertThatThrownBy(state::undo)
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void gameStateOpLogsAreChronological() {
		GameState state = GameState.customized(2, 2, 1);
		state.operate(0, 0, 0, 1, 5_000);
		state.operate(1, 0, 1, 1, 5_000);

		var logs = state.getOpLogs();
		assertThat(logs).hasSize(2);
		OpElimination first = (OpElimination)logs.get(0);
		OpElimination second = (OpElimination)logs.get(1);
		assertThat(first.getRow1()).isZero();
		assertThat(second.getRow1()).isEqualTo(1);
	}

	@Test
	void gameStateRejectsIllegalSelections() {
		GameState state = GameState.customized(2, 2, 1);
		// Same cell.
		assertThat(state.canEliminate(0, 0, 0, 0)).isFalse();
		assertThatThrownBy(() -> state.operate(0, 0, 0, 0, 5_000))
			.isInstanceOf(IllegalStateException.class);
		assertThat(state.getOpLogs()).isEmpty();

		// Empty cell after a successful elimination.
		state.operate(0, 0, 0, 1, 5_000);
		assertThat(state.canEliminate(0, 0, 1, 0)).isFalse();
	}

	@Test
	void gameStateOutOfBoundsThrows() {
		GameState state = GameState.customized(2, 2, 1);
		assertThatThrownBy(() -> state.canEliminate(-1, 0, 0, 0))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> state.canEliminate(0, 0, 5, 5))
			.isInstanceOf(IllegalStateException.class);
	}

	// ---- `isCleared` tests ------------------------------------------------

	@Test
	void clearedEmptyBoardReturnsTrue() {
		GameState state = GameState.customized(2, 2, 1);
		state.operate(0, 0, 0, 1, 5_000);
		state.operate(1, 0, 1, 1, 5_000);
		assertThat(state.isCleared()).isTrue();
	}

	@Test
	void freshGameIsNotCleared() {
		GameState state = GameState.customized(2, 2, 1);
		assertThat(state.isCleared()).isFalse();
	}

	@Test
	void partialEliminationNotCleared() {
		GameState state = GameState.customized(2, 2, 1);
		state.operate(0, 0, 0, 1, 5_000);
		assertThat(state.isCleared()).isFalse();
	}

	// ---- scoring tests ---------------------------------------------------

	@Test
	void freshGameStateHasZeroScore() {
		GameState state = GameState.customized(2, 2, 1);
		assertThat(state.gameStatus.score).isZero();
	}

	@Test
	void eachEliminationAddsScore() {
		GameState state = GameState.customized(2, 2, 1);
		state.operate(0, 0, 0, 1, 5_000);
		assertThat(state.gameStatus.score)
			.isEqualTo(OpElimination.SCORE_PER_PAIR);
		state.operate(1, 0, 1, 1, 5_000);
		assertThat(state.gameStatus.score)
			.isEqualTo(2 * OpElimination.SCORE_PER_PAIR);
	}

	@Test
	void undoRollsBackScore() {
		GameState state = GameState.customized(2, 2, 1);
		state.operate(0, 0, 0, 1, 5_000);
		state.undo();
		assertThat(state.gameStatus.score).isZero();
	}
}

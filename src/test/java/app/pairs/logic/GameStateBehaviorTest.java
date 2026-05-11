package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.model.Tilemap;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameStateBehaviorTest {
	// ---- `OpElimination` construction tests -------------------------------

	@Test
	void opEliminationRejectsNonPositiveTileId() {
		assertThatThrownBy(
			() -> new OpElimination(0, 0, 0, 0, 0, 0, List.of(), 0)
		)
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void opEliminationStoresFields() {
		var op = new OpElimination(
			42, 1, 2, 3, 4, 5_000, List.of(0, 0, 0, 2), 1_000
		);
		assertThat(op.getTileId()).isEqualTo(42);
		assertThat(op.getRow1()).isEqualTo(1);
		assertThat(op.getCol1()).isEqualTo(2);
		assertThat(op.getRow2()).isEqualTo(3);
		assertThat(op.getCol2()).isEqualTo(4);
		assertThat(op.getTime()).isEqualTo(5_000);
		assertThat(op.getPath()).containsExactly(0, 0, 0, 2);
		assertThat(op.getDeltaScore()).isEqualTo(1_000);
	}

	// ---- `TileTransition` tests -------------------------------------------

	@Test
	void transitionRejectsEmptyEndpoint() {
		Tilemap tm = new Tilemap(new int[][] {{0, 0, 1}});
		assertThatThrownBy(() -> TileTransition.transition(tm, 0, 0, 0, 1))
			.isInstanceOf(IllegalStateException.class);
	}

	// ---- `GameState` facade tests -----------------------------------------
	// `customized(2, 2, 1)` deterministically yields a 2x2 board where every
	// cell holds tile id 1, so any pair is eliminable.

	@Test
	void gameStateOperateClearsAndLogs() {
		GameState state = GameState.customized(2, 2, 1);
		assertThat(state.getOpLogs()).isEmpty();

		state.eliminate(0, 0, 0, 1, 5_000);
		assertThat(state.getTile(0, 0)).isZero();
		assertThat(state.getTile(0, 1)).isZero();
		assertThat(state.getOpLogs()).hasSize(1);
	}

	@Test
	void gameStateUndoRestoresAndPops() {
		GameState state = GameState.customized(2, 2, 1);
		state.eliminate(0, 0, 0, 1, 5_000);

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
		state.eliminate(0, 0, 0, 1, 5_000);
		state.eliminate(1, 0, 1, 1, 5_000);

		var logs = state.getOpLogs();
		assertThat(logs).hasSize(2);
		assertThat(logs.get(0).getRow1()).isZero();
		assertThat(logs.get(1).getRow1()).isEqualTo(1);
	}

	@Test
	void gameStateRejectsIllegalSelections() {
		GameState state = GameState.customized(2, 2, 1);
		assertThat(state.canEliminate(0, 0, 0, 0)).isFalse();
		assertThatThrownBy(() -> state.eliminate(0, 0, 0, 0, 5_000))
			.isInstanceOf(IllegalStateException.class);
		assertThat(state.getOpLogs()).isEmpty();

		state.eliminate(0, 0, 0, 1, 5_000);
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
		state.eliminate(0, 0, 0, 1, 5_000);
		state.eliminate(1, 0, 1, 1, 5_000);
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
		state.eliminate(0, 0, 0, 1, 5_000);
		assertThat(state.isCleared()).isFalse();
	}

	// ---- scoring tests ----------------------------------------------------

	@Test
	void freshGameStateHasZeroScore() {
		GameState state = GameState.customized(2, 2, 1);
		assertThat(state.gameStatus.score).isZero();
	}

	@Test
	void eachEliminationAddsScore() {
		GameState state = GameState.customized(2, 2, 1);
		state.eliminate(0, 0, 0, 1, 5_000);
		assertThat(state.gameStatus.score)
			.isEqualTo(OpElimination.SCORE_PER_PAIR);
		state.eliminate(1, 0, 1, 1, 5_000);
		assertThat(state.gameStatus.score)
			.isEqualTo(2 * OpElimination.SCORE_PER_PAIR);
	}

	@Test
	void undoRollsBackScore() {
		GameState state = GameState.customized(2, 2, 1);
		state.eliminate(0, 0, 0, 1, 5_000);
		state.undo();
		assertThat(state.gameStatus.score).isZero();
	}

	@Test
	void undoToUndoesToSpecifiedIndex() {
		GameState state = GameState.customized(2, 2, 1);
		state.eliminate(0, 0, 0, 1, 5_000);
		state.eliminate(1, 0, 1, 1, 5_000);
		assertThat(state.getOpLogCount()).isEqualTo(2);

		state.undoTo(0);
		assertThat(state.getOpLogCount()).isZero();
		assertThat(state.gameStatus.score).isZero();
		assertThat(state.isCleared()).isFalse();
	}
}

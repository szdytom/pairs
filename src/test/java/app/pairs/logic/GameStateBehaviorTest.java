package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		OpElimination op = new OpElimination(tm, 0, 0, 0, 2, ignored -> {});

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
		OpElimination op = new OpElimination(tm, 0, 0, 0, 2, ignored -> {});
		op.operate();
		assertThatThrownBy(op::operate)
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void undoBeforeOperateThrows() {
		Tilemap tm = new Tilemap(new int[][] {{1, 0, 1}});
		OpElimination op = new OpElimination(tm, 0, 0, 0, 2, ignored -> {});
		assertThatThrownBy(op::undo).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void constructionRejectsMismatchedTiles() {
		Tilemap tm = new Tilemap(new int[][] {{1, 0, 2}});
		Consumer<Operation> noop = ignored -> {};
		assertThatThrownBy(() -> new OpElimination(tm, 0, 0, 0, 2, noop))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void constructionRejectsEmptyTiles() {
		Tilemap tm = new Tilemap(new int[][] {{0, 0, 0}});
		Consumer<Operation> noop = ignored -> {};
		assertThatThrownBy(() -> new OpElimination(tm, 0, 0, 0, 2, noop))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void transitionRejectsEmptyEndpoint() {
		Tilemap tm = new Tilemap(new int[][] {{0, 0, 1}});
		assertThatThrownBy(() -> TileTransition.transition(tm, 0, 0, 0, 1))
			.isInstanceOf(IllegalStateException.class);
	}
}

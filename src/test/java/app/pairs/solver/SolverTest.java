package app.pairs.solver;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.logic.TileTransition;
import app.pairs.map.CustomizedTilemapFactory;
import app.pairs.map.TilemapPreset;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;

import org.junit.jupiter.api.Test;

class SolverTest {
	@Test
	void emptyMapIsAlreadySolved() {
		Tilemap map = new Tilemap(new int[][] {
			{0, 0, 0},
			{0, 0, 0},
		});
		SolverResult r = Solver.solve(map);
		assertThat(r.isComplete()).isTrue();
		assertThat(r.moves()).isEmpty();
	}

	@Test
	void singlePairTrivialClear() {
		Tilemap map = new Tilemap(new int[][] {
			{1, 0, 0, 1},
		});
		SolverResult r = Solver.solve(map);
		assertThat(r.isComplete()).isTrue();
		assertThat(r.moves()).hasSize(1);
	}

	@Test
	void twoPairsAnyOrder() {
		// 4-tile single row; both pairs connect (2's directly, 1's via
		// off-board row -1), so solver clears in 2 moves regardless of order.
		Tilemap map = new Tilemap(new int[][] {
			{1, 2, 2, 1},
		});
		SolverResult r = Solver.solve(map);
		assertThat(r.isComplete()).isTrue();
		assertThat(r.moves()).hasSize(2);
	}

	@Test
	void unsolvableLeavesRemainingPairs() {
		// 2x2 with diagonals: off-board corridors collapse because each
		// off-board reach is blocked by the other type, so neither pair
		// connects.
		Tilemap map = new Tilemap(new int[][] {
			{1, 2},
			{2, 1},
		});
		SolverResult r = Solver.solve(map);
		assertThat(r.isComplete()).isFalse();
		assertThat(r.remainingPairs()).isEqualTo(2);
		assertThat(r.moves()).isEmpty();
	}

	@Test
	void fixedMapIsFullySolvable() {
		Tilemap generated = new Tilemap(new int[][] {
			{1, 2, 3, 3, 2, 1},
			{4, 0, 0, 0, 0, 4},
			{1, 2, 3, 3, 2, 1},
			{4, 0, 0, 0, 0, 4},
		});
		SolverResult r = Solver.solve(generated);
		assertThat(r.isComplete())
			.as("solver should fully clear a fixed solvable map")
			.isTrue();
		// Verify the move sequence is internally consistent by replaying it.
		Tilemap replay = copy(generated);
		for (Move m : r.moves()) {
			assertThat(TileTransition.transition(
						   replay, m.r1(), m.c1(), m.r2(), m.c2()
					   ))
				.as("move %s should be valid at its turn", m)
				.isTrue();
			assertThat(replay.getTile(m.r1(), m.c1()))
				.isEqualTo(replay.getTile(m.r2(), m.c2()));
			replay.setTile(m.r1(), m.c1(), 0);
			replay.setTile(m.r2(), m.c2(), 0);
		}
	}

	@Test
	void hardLikeMapSolveTimeBatch() {
		TilemapPreset hardLike = new TilemapPreset(12, 12, 20, null);
		int runs = 5;
		long totalNanos = 0L;

		for (int i = 0; i < runs; i++) {
			Seed seed = Seed.fromString("solver-hard-batch-" + i);
			Tilemap map = CustomizedTilemapFactory.fromPreset(hardLike, seed)
							  .generate();

			long started = System.nanoTime();
			SolverResult result = Solver.solve(map);
			long elapsed = System.nanoTime() - started;

			totalNanos += elapsed;
			System.out.printf(
				"[Solver hard-like] run=%d elapsed=%.3f ms complete=%s "
					+ "remainingPairs=%d%n",
				i + 1, elapsed / 1_000_000.0, result.isComplete(),
				result.remainingPairs()
			);
			assertThat(result.isComplete())
				.as("hard-like generated map should be fully solvable")
				.isTrue();
		}

		double avgMs = totalNanos / 1_000_000.0 / runs;
		double totalMs = totalNanos / 1_000_000.0;
		System.out.printf(
			"[Solver hard-like] runs=%d total=%.3f ms avg=%.3f ms%n", runs,
			totalMs, avgMs
		);
	}

	private static Tilemap copy(Tilemap src) {
		int[][] g = new int[src.getHeight()][src.getWidth()];
		for (int r = 0; r < src.getHeight(); r++) {
			for (int c = 0; c < src.getWidth(); c++) {
				g[r][c] = src.getTile(r, c);
			}
		}
		return new Tilemap(g);
	}
}

package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.model.Tilemap;
import app.pairs.solver.Solver;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class TilemapPermuterTest {
	@Test
	void preservesTileCountsAndRespectsShape() {
		int[][] shape = {
			{0, -1, 0, 0},
			{0, 0, -1, 0},
			{0, 0, 0, 0},
		};
		Map<Integer, Integer> counts = new HashMap<>();
		counts.put(7, 4);
		counts.put(9, 2);

		int[][] result = TilemapPermuter.permute(shape, counts, new Random(1L));

		assertThat(counts(result)).isEqualTo(counts);
		for (int r = 0; r < shape.length; r++) {
			for (int c = 0; c < shape[r].length; c++) {
				if (shape[r][c] != 0) {
					assertThat(result[r][c]).isZero();
				}
			}
		}
	}

	@Test
	void generatedPermutationIsSolvable() {
		int[][] shape = new int[4][4];
		Map<Integer, Integer> counts = new HashMap<>();
		counts.put(1, 4);
		counts.put(2, 4);

		int[][] result = TilemapPermuter.permute(shape, counts, new Random(2L));

		assertThat(Solver.solve(new Tilemap(result)).isComplete()).isTrue();
	}

	@Test
	void rejectsTooFewLegalPositions() {
		int[][] shape = {{0, -1}, {-1, 0}};
		Map<Integer, Integer> counts = new HashMap<>();
		counts.put(1, 4);

		assertThatThrownBy(
			() -> TilemapPermuter.permute(shape, counts, new Random(3L))
		)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Not enough legal positions");
	}

	private static Map<Integer, Integer> counts(int[][] map) {
		Map<Integer, Integer> result = new HashMap<>();
		for (int[] row : map) {
			for (int tileId : row) {
				if (tileId > 0) {
					result.merge(tileId, 1, Integer::sum);
				}
			}
		}
		return result;
	}
}

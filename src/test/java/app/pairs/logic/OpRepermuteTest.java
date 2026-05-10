package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.model.Tilemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class OpRepermuteTest {
	@Test
	void operatePreservesCountsAndUndoRestoresMap() {
		int[][] original = {
			{1, 0, 1, 0},
			{2, 0, 2, 0},
			{1, 0, 1, 0},
			{0, 0, 0, 0},
		};
		Tilemap tilemap = new Tilemap(copy(original));
		List<Operation> pushed = new ArrayList<>();
		OpRepermute op = new OpRepermute(
			tilemap, new int[4][4], new Random(1L), pushed::add
		);

		op.operate();

		assertThat(pushed).containsExactly(op);
		assertThat(counts(tilemap)).isEqualTo(counts(original));

		op.undo();

		assertThat(snapshot(tilemap)).isDeepEqualTo(original);
	}

	@Test
	void guardsOperationLifecycle() {
		Tilemap tilemap = new Tilemap(new int[][] {{1, 1}, {0, 0}});
		OpRepermute op = new OpRepermute(
			tilemap, new int[2][2], new Random(2L), ignored -> {}
		);

		assertThatThrownBy(op::undo).isInstanceOf(IllegalStateException.class);

		op.operate();

		assertThatThrownBy(op::operate)
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsEmptyMap() {
		Tilemap tilemap = new Tilemap(new int[][] {{0, 0}, {0, 0}});
		OpRepermute op = new OpRepermute(
			tilemap, new int[2][2], new Random(3L), ignored -> {}
		);

		assertThatThrownBy(op::operate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No remaining tiles");
	}

	private static Map<Integer, Integer> counts(Tilemap tilemap) {
		Map<Integer, Integer> result = new HashMap<>();
		for (var entry : tilemap.tilesByIdMap().entrySet()) {
			result.put(entry.getKey(), entry.getValue().size());
		}
		return result;
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

	private static int[][] snapshot(Tilemap tilemap) {
		int[][] result = new int[tilemap.getHeight()][tilemap.getWidth()];
		for (int r = 0; r < tilemap.getHeight(); r++) {
			for (int c = 0; c < tilemap.getWidth(); c++) {
				result[r][c] = tilemap.getTile(r, c);
			}
		}
		return result;
	}

	private static int[][] copy(int[][] source) {
		int[][] result = new int[source.length][source[0].length];
		for (int r = 0; r < source.length; r++) {
			result[r] = source[r].clone();
		}
		return result;
	}
}

package app.pairs.logic;

import app.pairs.map.TilemapPermuter;
import app.pairs.model.Tilemap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class OpRepermute implements Operation {
	private final Tilemap tilemap;
	private final int[][] legalPlacementShape;
	private final Random random;
	private final Consumer<Operation> pushFn;

	private int[][] previousMap;
	private boolean executed;

	public OpRepermute(
		Tilemap tilemap, int[][] legalPlacementShape, Random random,
		Consumer<Operation> pushFn
	) {
		validateShape(tilemap, legalPlacementShape);
		this.tilemap = tilemap;
		this.legalPlacementShape = copy(legalPlacementShape);
		this.random = random;
		this.pushFn = pushFn;
	}

	@Override
	public void operate() {
		if (executed) {
			throw new IllegalStateException(
				"OpRepermute already executed; replaying history entries is"
				+ " not allowed"
			);
		}
		previousMap = copy(tilemap);
		int[][] nextMap = TilemapPermuter.permute(
			legalPlacementShape, tileCounts(), random
		);
		tilemap.setMap(nextMap);
		executed = true;
		pushFn.accept(this);
	}

	@Override
	public void undo() {
		if (!executed) {
			throw new IllegalStateException(
				"OpRepermute has not been executed; nothing to undo"
			);
		}
		tilemap.setMap(copy(previousMap));
		executed = false;
	}

	private Map<Integer, Integer> tileCounts() {
		Map<Integer, Integer> counts = new HashMap<>();
		for (var entry : tilemap.tilesByIdMap().entrySet()) {
			counts.put(entry.getKey(), entry.getValue().size());
		}
		return counts;
	}

	private static void validateShape(Tilemap tilemap, int[][] shape) {
		if (shape.length != tilemap.getHeight()) {
			throw new IllegalStateException(
				"Legal placement shape height mismatch: shape=" + shape.length
				+ ", map=" + tilemap.getHeight()
			);
		}
		for (int r = 0; r < shape.length; r++) {
			if (shape[r].length != tilemap.getWidth()) {
				throw new IllegalStateException(
					"Legal placement shape width mismatch at row " + r
					+ ": shape=" + shape[r].length
					+ ", map=" + tilemap.getWidth()
				);
			}
		}
	}

	private static int[][] copy(Tilemap source) {
		int[][] result = new int[source.getHeight()][source.getWidth()];
		for (int r = 0; r < source.getHeight(); r++) {
			for (int c = 0; c < source.getWidth(); c++) {
				result[r][c] = source.getTile(r, c);
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

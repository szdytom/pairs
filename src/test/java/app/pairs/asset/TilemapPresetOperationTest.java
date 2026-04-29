package app.pairs.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.map.TilemapPreset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

class TilemapPresetOperationTest {
	private static JsonObject buildItem(
		int width, int height, int types, int[][] initial
	) {
		JsonObject item = new JsonObject();
		item.addProperty("id", "test/preset");
		item.addProperty("type", "tilemap-preset");
		item.addProperty("width", width);
		item.addProperty("height", height);
		item.addProperty("types", types);
		if (initial != null) {
			JsonArray rows = new JsonArray();
			for (int[] row : initial) {
				JsonArray rowArr = new JsonArray();
				for (int v : row) {
					rowArr.add(v);
				}
				rows.add(rowArr);
			}
			item.add("initial", rows);
		}
		return item;
	}

	@Test
	void noInitialProducesAllFillableGrid() {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem(4, 3, 5, null));

		TilemapPreset preset = new TilemapPreset(4, 3, 5, null);
		int[][] grid = preset.buildInitial();
		assertThat(grid).hasDimensions(3, 4);
		for (int[] row : grid) {
			for (int cell : row) {
				assertThat(cell).isEqualTo(0);
			}
		}
	}

	@Test
	void validInitialGridIsAccepted() {
		int[][] initial = {
			{0, -1, 0},
			{-1, 0, -1},
		};
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem(3, 2, 4, initial));
		// No exception means configure accepted the grid
	}

	@Test
	void initialRowCountMismatchThrows() {
		// Provide 1 row but height=2
		int[][] initial = {{0, -1, 0}};
		TilemapPresetOperation op = new TilemapPresetOperation();
		assertThatThrownBy(() -> op.configure(buildItem(3, 2, 4, initial)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("row count");
	}

	@Test
	void jaggedInitialRowThrows() {
		// Row 0 has 3 cols, row 1 has 2 — width=3 so row 1 mismatches
		JsonObject item = buildItem(3, 2, 4, new int[][] {{0, 0, 0}, {0, 0}});
		TilemapPresetOperation op = new TilemapPresetOperation();
		assertThatThrownBy(() -> op.configure(item))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("column count");
	}
}

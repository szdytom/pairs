package app.pairs.asset;

import app.pairs.map.TilemapPreset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Builds a {@link TilemapPreset} from a manifest entry. Required fields:
 * {@code width}, {@code height}, {@code types}. Optional {@code initial}
 * is a 2D int array seed grid (0 = fillable, -1 = blocked); when omitted
 * an all-fillable grid of the given size is used.
 */
public class TilemapPresetOperation implements AssetOperation {
	private int width;
	private int height;
	private int types;
	private int[][] initial;

	@Override
	public String type() {
		return "tilemap-preset";
	}

	@Override
	public void configure(JsonObject item) {
		width = item.get("width").getAsInt();
		height = item.get("height").getAsInt();
		types = item.get("types").getAsInt();
		if (item.has("initial")) {
			initial = parseGrid(item.getAsJsonArray("initial"));
			validateGrid(initial, width, height);
		}
	}

	private static void validateGrid(int[][] grid, int width, int height) {
		if (grid.length != height) {
			throw new IllegalArgumentException(
				"initial row count (" + grid.length
				+ ") does not match height (" + height + ")"
			);
		}
		for (int r = 0; r < grid.length; r++) {
			if (grid[r] == null || grid[r].length != width) {
				int len = grid[r] == null ? 0 : grid[r].length;
				throw new IllegalArgumentException(
					"initial row " + r + " column count (" + len
					+ ") does not match width (" + width + ")"
				);
			}
		}
	}

	@Override
	public void process(Context ctx) {
		System.out.println(
			"  Building tilemap preset: " + ctx.id() + " (" + width + "x"
			+ height + ", types=" + types + ")"
		);
		ctx.put(ctx.id(), new TilemapPreset(width, height, types, initial));
	}

	private static int[][] parseGrid(JsonArray rows) {
		int[][] grid = new int[rows.size()][];
		for (int r = 0; r < rows.size(); r++) {
			JsonArray row = rows.get(r).getAsJsonArray();
			grid[r] = new int[row.size()];
			for (int c = 0; c < row.size(); c++) {
				grid[r][c] = row.get(c).getAsInt();
			}
		}
		return grid;
	}
}

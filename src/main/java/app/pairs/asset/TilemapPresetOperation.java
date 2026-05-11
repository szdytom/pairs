package app.pairs.asset;

import app.pairs.map.PresetConfig;
import app.pairs.map.Shape;
import app.pairs.map.TileSelectionPolicy;
import app.pairs.map.TilemapPreset;
import app.pairs.model.Tilemap;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Builds a {@link PresetConfig} from an external JSON file. Required fields
 * in the file: {@code width}, {@code height}, {@code types},
 * {@code difficulty}, {@code includeSlabs}, {@code spread}. Optional
 * {@code shape} references a {@link Shape} asset ID for the seed grid
 * (0 = fillable, -1 = blocked); when omitted an all-fillable grid of the
 * given size is used. Optional {@code pairStrategy} selects the pair-matching
 * strategy ({@code "base"}|{@code "nonAdjacent"}|{@code "distant"}); defaults
 * to {@code "base"} when absent.
 */
public class TilemapPresetOperation implements AssetOperation {
	private String file;

	@Override
	public String type() {
		return "tilemap-preset";
	}

	@Override
	public void configure(JsonObject item) {
		file = item.get("file").getAsString();
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading tilemap preset: " + file);
		byte[] bytes;
		try (InputStream is = ctx.loader().load(file)) {
			bytes = is.readAllBytes();
		}

		String json = new String(bytes, StandardCharsets.UTF_8);
		JsonObject root = new Gson().fromJson(json, JsonObject.class);

		int width = root.get("width").getAsInt();
		int height = root.get("height").getAsInt();
		int types = root.get("types").getAsInt();
		Tilemap.Difficulty difficulty = Tilemap.Difficulty.valueOf(
			root.get("difficulty").getAsString()
		);
		boolean includeSlabs = root.get("includeSlabs").getAsBoolean();
		TileSelectionPolicy.Spread spread = TileSelectionPolicy.Spread.valueOf(
			root.get("spread").getAsString()
		);

		int[][] initial = null;
		if (root.has("shape")) {
			String shapeId = root.get("shape").getAsString();
			Object raw;
			try {
				raw = ctx.getInput(shapeId);
			} catch (Exception e) {
				throw new IllegalArgumentException(
					"Shape \"" + shapeId + "\" not found for preset \""
						+ ctx.id() + "\"",
					e
				);
			}
			if (!(raw instanceof Shape shape)) {
				throw new IllegalArgumentException(
					"Asset \"" + shapeId + "\" referenced by preset \""
					+ ctx.id() + "\" is not a Shape (got "
					+ raw.getClass().getSimpleName() + ")"
				);
			}
			initial = shape.grid();
			validateGrid(initial, width, height);
		}

		System.out.println(
			"  Building tilemap preset: " + ctx.id() + " (" + width + "x"
			+ height + ", types=" + types + ", " + difficulty + ")"
		);

		var pairStrategy = TilemapPreset.parseStrategy(root);
		TilemapPreset preset = new TilemapPreset(
			width, height, types, initial, pairStrategy
		);
		TileSelectionPolicy policy = new TileSelectionPolicy(
			includeSlabs, spread
		);
		ctx.put(ctx.id(), new PresetConfig(preset, difficulty, policy));
	}

	private static void validateGrid(int[][] grid, int width, int height) {
		if (grid.length != height) {
			throw new IllegalArgumentException(
				"seed grid row count (" + grid.length
				+ ") does not match height (" + height + ")"
			);
		}
		for (int r = 0; r < grid.length; r++) {
			if (grid[r] == null || grid[r].length != width) {
				int len = grid[r] == null ? 0 : grid[r].length;
				throw new IllegalArgumentException(
					"seed grid row " + r + " column count (" + len
					+ ") does not match width (" + width + ")"
				);
			}
		}
	}
}

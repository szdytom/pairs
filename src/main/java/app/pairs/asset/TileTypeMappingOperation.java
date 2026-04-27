package app.pairs.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Assigns a 2D type mapping to a TileRegistry.
 * Each cell maps a tilesheet position to a string tile ID, or null (empty).
 */
public class TileTypeMappingOperation implements AssetOperation {
	private String id;
	private String input;
	private String[][] mapping;

	@Override
	public String type() {
		return "tile-type-mapping";
	}

	@Override
	public void configure(JsonObject item) {
		id(item.get("id").getAsString());
		input(item.get("input").getAsString());

		JsonArray mappingArray = item.getAsJsonArray("mapping");
		int rows = mappingArray.size();
		String[][] mapping = new String[rows][];
		for (int r = 0; r < rows; r++) {
			JsonArray rowArray = mappingArray.get(r).getAsJsonArray();
			int cols = rowArray.size();
			mapping[r] = new String[cols];
			for (int c = 0; c < cols; c++) {
				JsonElement elem = rowArray.get(c);
				mapping[r][c] = elem.isJsonNull() ? null : elem.getAsString();
			}
		}
		mapping(mapping);
	}

	@Override
	public void process(Context ctx) throws Exception {
		TileRegistry registry = ctx.getInput(input);
		registry.setTypeMapping(mapping);
		ctx.put(id, registry);
	}

	public TileTypeMappingOperation id(String id) {
		this.id = id;
		return this;
	}

	public TileTypeMappingOperation input(String input) {
		this.input = input;
		return this;
	}

	public TileTypeMappingOperation mapping(String[][] mapping) {
		this.mapping = mapping;
		return this;
	}
}

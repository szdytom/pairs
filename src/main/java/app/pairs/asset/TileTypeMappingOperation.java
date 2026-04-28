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
	private String mappingId;

	@Override
	public String type() {
		return "tile-type-mapping";
	}

	@Override
	public void configure(JsonObject item) {
		id(item.get("id").getAsString());
		input(item.get("input").getAsString());

		if (item.has("mapping-id")) {
			mappingId(item.get("mapping-id").getAsString());
		} else {
			JsonArray mappingArray = item.getAsJsonArray("mapping");
			int rows = mappingArray.size();
			String[][] mapping = new String[rows][];
			for (int r = 0; r < rows; r++) {
				JsonArray rowArray = mappingArray.get(r).getAsJsonArray();
				int cols = rowArray.size();
				mapping[r] = new String[cols];
				for (int c = 0; c < cols; c++) {
					JsonElement elem = rowArray.get(c);
					mapping[r][c] = elem.isJsonNull()
						? null
						: elem.getAsString();
				}
			}
			mapping(mapping);
		}
	}

	@Override
	public void process(Context ctx) throws Exception {
		String[][] resolved = mapping != null
			? mapping
			: ctx.getInput(mappingId);
		TileRegistry registry = ctx.getInput(input);
		registry.setTypeMapping(resolved);
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

	public TileTypeMappingOperation mappingId(String mappingId) {
		this.mappingId = mappingId;
		return this;
	}
}

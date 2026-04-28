package app.pairs.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Stores a 2D string mapping in the asset map for later reference
 * by {@link TileTypeMappingOperation} via {@code mapping-id}.
 */
public class MappingOperation implements AssetOperation {
	private String id;
	private String[][] value;

	@Override
	public String type() {
		return "mapping";
	}

	@Override
	public void configure(JsonObject item) {
		id(item.get("id").getAsString());

		JsonArray valueArray = item.getAsJsonArray("value");
		int rows = valueArray.size();
		String[][] value = new String[rows][];
		for (int r = 0; r < rows; r++) {
			JsonArray rowArray = valueArray.get(r).getAsJsonArray();
			int cols = rowArray.size();
			value[r] = new String[cols];
			for (int c = 0; c < cols; c++) {
				JsonElement elem = rowArray.get(c);
				value[r][c] = elem.isJsonNull() ? null : elem.getAsString();
			}
		}
		value(value);
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Storing mapping: " + id);
		ctx.put(id, value);
	}

	public MappingOperation id(String id) {
		this.id = id;
		return this;
	}

	public MappingOperation value(String[][] value) {
		this.value = value;
		return this;
	}
}

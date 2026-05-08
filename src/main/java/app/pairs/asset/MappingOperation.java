package app.pairs.asset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Stores a 2D string mapping in the asset map for later reference
 * by {@link TileTypeMappingOperation} via {@code mapping-id}.
 */
public class MappingOperation implements AssetOperation {
	private String id;
	private String file;

	@Override
	public String type() {
		return "mapping";
	}

	@Override
	public void configure(JsonObject item) {
		id = item.get("id").getAsString();
		file = item.get("file").getAsString();
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading mapping: " + file);
		byte[] bytes;
		try (InputStream is = ctx.loader().load(file)) {
			bytes = is.readAllBytes();
		}

		String json = new String(bytes, StandardCharsets.UTF_8);
		JsonObject root = new Gson().fromJson(json, JsonObject.class);
		JsonArray mappingArray = root.getAsJsonArray("mapping");

		int rows = mappingArray.size();
		String[][] value = new String[rows][];
		for (int r = 0; r < rows; r++) {
			JsonArray rowArray = mappingArray.get(r).getAsJsonArray();
			int cols = rowArray.size();
			value[r] = new String[cols];
			for (int c = 0; c < cols; c++) {
				JsonElement elem = rowArray.get(c);
				value[r][c] = elem.isJsonNull() ? null : elem.getAsString();
			}
		}

		System.out.println("  Storing mapping: " + id);
		ctx.put(id, value);
	}
}

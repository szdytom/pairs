package app.pairs.asset;

import app.pairs.map.TileGroupRegistry;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Loads a {@link TileGroupRegistry} from an external JSON file. Required
 * field: {@code file} (relative to the manifest). The JSON file must have a
 * top-level {@code groups} array; each entry has a {@code members} string
 * array and an optional {@code slab} boolean (defaults to {@code false}).
 */
public class TileGroupsOperation implements AssetOperation {
	private String file;

	@Override
	public String type() {
		return "tile-groups";
	}

	@Override
	public void configure(JsonObject item) {
		file = item.get("file").getAsString();
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading tile groups: " + file);
		byte[] bytes;
		try (InputStream is = ctx.loader().load(file)) {
			bytes = is.readAllBytes();
		}

		String json = new String(bytes, StandardCharsets.UTF_8);
		JsonObject root = new Gson().fromJson(json, JsonObject.class);
		JsonArray arr = root.getAsJsonArray("groups");

		List<List<String>> nonSlabGroups = new ArrayList<>(arr.size());
		List<List<String>> slabGroups = new ArrayList<>();
		for (JsonElement el : arr) {
			JsonObject obj = el.getAsJsonObject();
			boolean slab = obj.has("slab") && obj.get("slab").getAsBoolean();
			JsonArray members = obj.getAsJsonArray("members");
			List<String> ids = new ArrayList<>(members.size());
			for (JsonElement m : members) {
				ids.add(m.getAsString());
			}
			if (slab) {
				slabGroups.add(ids);
			} else {
				nonSlabGroups.add(ids);
			}
		}

		int total = nonSlabGroups.size() + slabGroups.size();
		System.out.println(
			"  Loaded tile groups: " + ctx.id() + " (" + total + " groups)"
		);
		ctx.put(ctx.id(), new TileGroupRegistry(nonSlabGroups, slabGroups));
	}
}

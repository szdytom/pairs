package app.pairs.asset;

import app.pairs.model.ItemType;
import app.pairs.model.ShopItemConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ShopItemOperation implements AssetOperation {
	private String file;

	@Override
	public String type() {
		return "shop-item";
	}

	@Override
	public void configure(JsonObject item) {
		file = item.get("file").getAsString();
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading shop item: " + file);
		byte[] bytes;
		try (InputStream is = ctx.loader().load(file)) {
			bytes = is.readAllBytes();
		}
		String json = new String(bytes, StandardCharsets.UTF_8);
		JsonObject root = new Gson().fromJson(json, JsonObject.class);

		String title = root.get("title").getAsString();
		String icon = root.get("icon").getAsString();
		int k = root.get("k").getAsInt();
		double alpha = root.get("alpha").getAsDouble();
		String kind = root.get("kind").getAsString();
		String itemType = root.has("itemType")
			? root.get("itemType").getAsString()
			: null;

		if (!"item".equals(kind) && !"time".equals(kind)) {
			throw new IllegalArgumentException(
				"Shop item \"" + file + "\": invalid kind \"" + kind
				+ "\" (expected \"item\" or \"time\")"
			);
		}
		if ("item".equals(kind)) {
			if (itemType == null) {
				throw new IllegalArgumentException(
					"Shop item \"" + file
					+ "\": kind=\"item\" but itemType is missing"
				);
			}
			try {
				ItemType.valueOf(itemType);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
					"Shop item \"" + file + "\": unknown itemType \"" + itemType
					+ "\" (expected one of: "
					+ java.util.Arrays.toString(ItemType.values()) + ")"
				);
			}
		}

		List<String> description = new ArrayList<>();
		if (root.has("description")) {
			JsonArray arr = root.getAsJsonArray("description");
			for (int i = 0; i < arr.size(); i++) {
				description.add(arr.get(i).getAsString());
			}
		}

		List<List<String>> taglines = new ArrayList<>();
		if (root.has("taglines")) {
			JsonArray outer = root.getAsJsonArray("taglines");
			for (int i = 0; i < outer.size(); i++) {
				List<String> group = new ArrayList<>();
				JsonArray inner = outer.get(i).getAsJsonArray();
				for (int j = 0; j < inner.size(); j++) {
					group.add(inner.get(j).getAsString());
				}
				taglines.add(group);
			}
		}

		ctx.put(
			ctx.id(),
			new ShopItemConfig(
				title, icon, k, alpha, kind, itemType, description, taglines
			)
		);
	}
}

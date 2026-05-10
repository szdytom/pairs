package app.pairs.asset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class AudioRegistry {
	private final Map<String, Category> categories;

	private AudioRegistry(Map<String, Category> categories) {
		this.categories = categories;
	}

	public static AudioRegistry load(AssetLoader loader, String path)
		throws Exception {
		JsonObject root = readJson(loader, path);
		JsonObject categoryRoot = root.getAsJsonObject("categories");
		Map<String, Category> categories = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : categoryRoot.entrySet()) {
			categories.put(
				entry.getKey(),
				loadCategory(loader, entry.getValue().getAsJsonObject())
			);
		}
		return new AudioRegistry(categories);
	}

	public String resolve(String category, String object) {
		Category config = categories.get(category);
		if (config == null) {
			throw new IllegalArgumentException(
				"Unknown audio category: " + category
			);
		}
		String file = config.resolve(object);
		return config.basePath() + "/" + file;
	}

	private static Category loadCategory(AssetLoader loader, JsonObject config)
		throws Exception {
		JsonObject source = readJson(loader, config.get("file").getAsString());
		String objectRoot = config.get("objectRoot").getAsString();
		String event = config.get("event").getAsString();
		String basePath = config.get("basePath").getAsString();
		return new Category(
			source.getAsJsonObject(objectRoot), event, basePath
		);
	}

	private static JsonObject readJson(AssetLoader loader, String path)
		throws Exception {
		try (InputStream stream = loader.load(path)) {
			String json = new String(
				stream.readAllBytes(), StandardCharsets.UTF_8
			);
			return new Gson().fromJson(json, JsonObject.class);
		}
	}

	private record Category(JsonObject objects, String event, String basePath) {
		String resolve(String object) {
			JsonObject events = objects.getAsJsonObject(object);
			if (events == null || !events.has(event)) {
				throw new IllegalArgumentException(
					"Unknown audio object: " + object
				);
			}
			return events.get(event).getAsString();
		}
	}
}

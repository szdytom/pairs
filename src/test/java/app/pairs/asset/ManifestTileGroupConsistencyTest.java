package app.pairs.asset;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Regression: every member declared in {@code assets/tile-groups.json} must
 * appear in the {@code tile-mapping} entry of {@code assets/manifest.json}.
 *
 * <p>If this test fails it means {@code TileSelectionPolicy} will silently
 * fall back to "no group constraints" because {@code TileGroupRegistry
 * .findGroup()} cannot match the registry's string IDs. See PR #18 review.
 */
class ManifestTileGroupConsistencyTest {
	@Test
	void everyTileGroupMemberAppearsInManifestMapping() throws Exception {
		Set<String> mappingIds = readManifestMappingIds();
		JsonArray groups = readTileGroups();

		for (JsonElement g : groups) {
			JsonArray members = g.getAsJsonObject().getAsJsonArray("members");
			for (JsonElement m : members) {
				String name = m.getAsString();
				assertThat(mappingIds)
					.as("tile-groups member '%s' missing from "
				            + "manifest.json#tile-mapping",
				        name)
					.contains(name);
			}
		}
	}

	private Set<String> readManifestMappingIds() throws Exception {
		AssetLoader loader = new ClspAssetLoader(getClass().getClassLoader());
		try (InputStream is = loader.load("manifest.json")) {
			JsonObject root = JsonParser
								  .parseReader(new InputStreamReader(
									  is, StandardCharsets.UTF_8
								  ))
								  .getAsJsonObject();
			JsonArray seq = root.getAsJsonArray("sequence");
			for (JsonElement e : seq) {
				JsonObject item = e.getAsJsonObject();
				if ("tile-mapping".equals(item.get("id").getAsString())) {
					String mappingFile = item.get("file").getAsString();
					return loadMappingFromFile(mappingFile);
				}
			}
			throw new IllegalStateException(
				"manifest.json has no 'tile-mapping' entry"
			);
		}
	}

	private Set<String> loadMappingFromFile(String path) throws Exception {
		AssetLoader loader = new ClspAssetLoader(getClass().getClassLoader());
		try (InputStream is = loader.load(path)) {
			byte[] bytes = is.readAllBytes();
			String json = new String(bytes, StandardCharsets.UTF_8);
			JsonObject root = new Gson().fromJson(json, JsonObject.class);
			JsonArray rows = root.getAsJsonArray("mapping");
			return flattenMapping(rows);
		}
	}

	private static Set<String> flattenMapping(JsonArray rows) {
		Set<String> out = new HashSet<>();
		for (JsonElement row : rows) {
			for (JsonElement cell : row.getAsJsonArray()) {
				if (!cell.isJsonNull()) {
					out.add(cell.getAsString());
				}
			}
		}
		return out;
	}

	private JsonArray readTileGroups() throws Exception {
		AssetLoader loader = new ClspAssetLoader(getClass().getClassLoader());
		try (InputStream is = loader.load("tile-groups.json")) {
			JsonObject root = JsonParser
								  .parseReader(new InputStreamReader(
									  is, StandardCharsets.UTF_8
								  ))
								  .getAsJsonObject();
			return root.getAsJsonArray("groups");
		}
	}
}

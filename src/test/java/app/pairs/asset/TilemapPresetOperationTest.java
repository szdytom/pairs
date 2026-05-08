package app.pairs.asset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

class TilemapPresetOperationTest {
	private static final String ALL_FILLABLE_JSON = ""
		+ "{\n"
		+ "	\"width\": 4,\n"
		+ "	\"height\": 3,\n"
		+ "	\"types\": 5,\n"
		+ "	\"difficulty\": \"EASY\",\n"
		+ "	\"includeSlabs\": false,\n"
		+ "	\"spread\": \"NO_DUPLICATES\"\n"
		+ "}";

	private static final String VALID_INITIAL_JSON = ""
		+ "{\n"
		+ "	\"width\": 3,\n"
		+ "	\"height\": 2,\n"
		+ "	\"types\": 4,\n"
		+ "	\"difficulty\": \"HARD\",\n"
		+ "	\"includeSlabs\": false,\n"
		+ "	\"spread\": \"FREE\",\n"
		+ "	\"initial\": [[0, -1, 0], [-1, 0, -1]]\n"
		+ "}";

	private static final String ROW_MISMATCH_JSON = ""
		+ "{\n"
		+ "	\"width\": 3,\n"
		+ "	\"height\": 2,\n"
		+ "	\"types\": 4,\n"
		+ "	\"difficulty\": \"EASY\",\n"
		+ "	\"includeSlabs\": false,\n"
		+ "	\"spread\": \"NO_DUPLICATES\",\n"
		+ "	\"initial\": [[0, -1, 0]]\n"
		+ "}";

	private static final String JAGGED_ROW_JSON = ""
		+ "{\n"
		+ "	\"width\": 3,\n"
		+ "	\"height\": 2,\n"
		+ "	\"types\": 4,\n"
		+ "	\"difficulty\": \"EASY\",\n"
		+ "	\"includeSlabs\": false,\n"
		+ "	\"spread\": \"NO_DUPLICATES\",\n"
		+ "	\"initial\": [[0, 0, 0], [0, 0]]\n"
		+ "}";

	private static JsonObject buildItem(String file) {
		JsonObject item = new JsonObject();
		item.addProperty("id", "test/preset");
		item.addProperty("type", "tilemap-preset");
		item.addProperty("file", file);
		return item;
	}

	private static AssetOperation.Context mockContext(String presetJson) {
		AssetLoader loader = path
			-> new ByteArrayInputStream(
				presetJson.getBytes(StandardCharsets.UTF_8)
			);
		return new AssetOperation.Context() {
			@Override
			public String id() {
				return "test/preset";
			}
			@Override
			public AssetLoader loader() {
				return loader;
			}
			@Override
			public AssetOperation.Registry registry() {
				return null;
			}
			@Override
			public void put(String id, Object asset) {}
			@Override
			public <T> T getInput(String ref) {
				return null;
			}
		};
	}

	@Test
	void noInitialProducesAllFillableGrid() throws Exception {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("no-init.json"));
		op.process(mockContext(ALL_FILLABLE_JSON));
	}

	@Test
	void validInitialGridIsAccepted() throws Exception {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("valid.json"));
		op.process(mockContext(VALID_INITIAL_JSON));
	}

	@Test
	void initialRowCountMismatchThrows() {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("bad-rows.json"));
		assertThatThrownBy(() -> op.process(mockContext(ROW_MISMATCH_JSON)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("row count");
	}

	@Test
	void jaggedInitialRowThrows() {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("jagged.json"));
		assertThatThrownBy(() -> op.process(mockContext(JAGGED_ROW_JSON)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("column count");
	}
}

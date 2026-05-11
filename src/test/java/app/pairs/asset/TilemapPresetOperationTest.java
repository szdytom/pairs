package app.pairs.asset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.map.Shape;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

	private static final String VALID_SHAPE_JSON = ""
		+ "{\n"
		+ "	\"width\": 3,\n"
		+ "	\"height\": 2,\n"
		+ "	\"types\": 4,\n"
		+ "	\"difficulty\": \"HARD\",\n"
		+ "	\"includeSlabs\": false,\n"
		+ "	\"spread\": \"FREE\",\n"
		+ "	\"shape\": \"shape/test\"\n"
		+ "}";

	private static final String ROW_MISMATCH_JSON = ""
		+ "{\n"
		+ "	\"width\": 3,\n"
		+ "	\"height\": 2,\n"
		+ "	\"types\": 4,\n"
		+ "	\"difficulty\": \"EASY\",\n"
		+ "	\"includeSlabs\": false,\n"
		+ "	\"spread\": \"NO_DUPLICATES\",\n"
		+ "	\"shape\": \"shape/test\"\n"
		+ "}";

	private static final Shape VALID_SHAPE = new Shape(
		new int[][] {{0, -1, 0}, {-1, 0, -1}}
	);

	private static final Shape ROW_MISMATCH_SHAPE = new Shape(
		new int[][] {{0, -1, 0}}
	);

	private static JsonObject buildItem(String file) {
		JsonObject item = new JsonObject();
		item.addProperty("id", "test/preset");
		item.addProperty("type", "tilemap-preset");
		item.addProperty("file", file);
		return item;
	}

	private static AssetOperation.Context mockContext(
		String presetJson, Map<String, Shape> shapes
	) {
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
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getInput(String ref) {
				return (T)shapes.get(ref);
			}
		};
	}

	@Test
	void noShapeProducesAllFillableGrid() throws Exception {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("no-shape.json"));
		op.process(mockContext(ALL_FILLABLE_JSON, Map.of()));
	}

	@Test
	void validShapeGridIsAccepted() throws Exception {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("valid.json"));
		op.process(
			mockContext(VALID_SHAPE_JSON, Map.of("shape/test", VALID_SHAPE))
		);
	}

	@Test
	void shapeRowCountMismatchThrows() {
		TilemapPresetOperation op = new TilemapPresetOperation();
		op.configure(buildItem("bad-rows.json"));
		assertThatThrownBy(
			()
				-> op.process(mockContext(
					ROW_MISMATCH_JSON, Map.of("shape/test", ROW_MISMATCH_SHAPE)
				))
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("row count");
	}
}

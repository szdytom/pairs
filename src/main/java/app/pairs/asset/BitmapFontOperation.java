package app.pairs.asset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BitmapFontOperation implements AssetOperation {
	private String id;
	private String file;

	@Override
	public String type() {
		return "bitmap-font";
	}

	@Override
	public void configure(JsonObject item) {
		id = item.get("id").getAsString();
		file = item.get("file").getAsString();
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading bitmap font: " + file);
		InputStream is = ctx.loader().load(file);
		byte[] jsonBytes = is.readAllBytes();
		is.close();

		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		Gson gson = new Gson();
		JsonObject root = gson.fromJson(json, JsonObject.class);

		Map<Integer, int[]> glyphData = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			int codePoint = entry.getKey().codePointAt(0);
			JsonArray arr = entry.getValue().getAsJsonArray();
			int[] rows = new int[BitmapFont.GLYPH_HEIGHT];
			for (int i = 0; i < BitmapFont.GLYPH_HEIGHT; i++) {
				rows[i] = arr.get(i).getAsInt();
			}
			glyphData.put(codePoint, rows);
		}

		BitmapFont font = new BitmapFont(glyphData);
		font.prebuildTextures(AssetManager.instance().renderer());

		System.out.println(
			"  Loaded font: " + id + " (" + glyphData.size() + " glyphs)"
		);
		ctx.put(id, font);
	}
}

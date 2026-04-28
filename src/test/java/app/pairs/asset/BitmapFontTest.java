package app.pairs.asset;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.view.BitmapFontRenderer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

class BitmapFontTest {
	@Test
	void parsesAllGlyphsFromMonogramJson() throws Exception {
		var glyphData = loadGlyphData();

		try (BitmapFont font = new BitmapFont(glyphData)) {
			assertThat(font.hasGlyph('0')).isTrue();
			assertThat(font.hasGlyph('A')).isTrue();
			assertThat(font.hasGlyph('z')).isTrue();
			assertThat(font.hasGlyph('.')).isTrue();
			assertThat(font.hasGlyph(' ')).isTrue();
			assertThat(font.hasGlyph('ď')).isTrue();
		}
	}

	@Test
	void hasGlyphReturnsFalseForMissing() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			assertThat(font.hasGlyph('�')).isFalse();
		}
	}

	@Test
	void standardGlyphWidthIsDefaultAdvanceOrMore() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			assertThat(font.getGlyphWidth('A'))
				.isEqualTo(BitmapFont.DEFAULT_ADVANCE);
			assertThat(font.getGlyphWidth('0'))
				.isEqualTo(BitmapFont.DEFAULT_ADVANCE);
			assertThat(font.getGlyphWidth('.'))
				.isEqualTo(BitmapFont.DEFAULT_ADVANCE);
			assertThat(font.getGlyphWidth(' '))
				.isEqualTo(BitmapFont.DEFAULT_ADVANCE);
		}
	}

	@Test
	void wideGlyphUsesLargerWidth() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			assertThat(font.getGlyphWidth('ď'))
				.isGreaterThan(BitmapFont.DEFAULT_ADVANCE);
		}
	}

	@Test
	void missingGlyphReturnsDefaultAdvance() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			assertThat(font.getGlyphWidth('�'))
				.isEqualTo(BitmapFont.DEFAULT_ADVANCE);
		}
	}

	@Test
	void measureTextReturnsLogicalWidth() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			// "Hi" – two 6-wide standard glyphs at size=1
			assertThat(BitmapFontRenderer.measureText(font, "Hi", 1))
				.isEqualTo(12);

			// Same text at size=2
			assertThat(BitmapFontRenderer.measureText(font, "Hi", 2))
				.isEqualTo(24);
		}
	}

	@Test
	void measureTextWithNewlineReturnsMinusOne() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			assertThat(BitmapFontRenderer.measureText(font, "A\nB", 1))
				.isEqualTo(-1);
		}
	}

	@Test
	void measureTextEmptyStringReturnsZero() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			assertThat(BitmapFontRenderer.measureText(font, "", 1)).isZero();
		}
	}

	@Test
	void measureTextWithWideGlyphAccountsForWidth() throws Exception {
		try (BitmapFont font = new BitmapFont(loadGlyphData())) {
			int greaterWidth = font.getGlyphWidth('ď');
			assertThat(greaterWidth).isGreaterThan(BitmapFont.DEFAULT_ADVANCE);

			int measured = BitmapFontRenderer.measureText(font, "ď", 1);
			assertThat(measured).isEqualTo(greaterWidth);
		}
	}

	@Test
	void defaultAdvanceIsSix() {
		assertThat(BitmapFont.DEFAULT_ADVANCE).isEqualTo(6);
		assertThat(BitmapFont.GLYPH_HEIGHT).isEqualTo(12);
	}

	private static Map<Integer, int[]> loadGlyphData() throws Exception {
		var loader = new ClspAssetLoader(BitmapFontTest.class.getClassLoader());
		InputStream is = loader.load("monogram-bitmap.json");
		byte[] bytes = is.readAllBytes();
		is.close();

		String json = new String(bytes, StandardCharsets.UTF_8);
		JsonObject root = new Gson().fromJson(json, JsonObject.class);

		Map<Integer, int[]> data = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			int cp = entry.getKey().codePointAt(0);
			JsonArray arr = entry.getValue().getAsJsonArray();
			int[] rows = new int[BitmapFont.GLYPH_HEIGHT];
			for (int i = 0; i < BitmapFont.GLYPH_HEIGHT; i++) {
				rows[i] = arr.get(i).getAsInt();
			}
			data.put(cp, rows);
		}
		return data;
	}
}

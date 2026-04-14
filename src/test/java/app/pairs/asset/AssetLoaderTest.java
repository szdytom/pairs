package app.pairs.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

class AssetLoaderTest {
	@Test
	void loadManifestJsonAndFailMissingAsset() throws Exception {
		AssetLoader loader = new ClspAssetLoader(getClass().getClassLoader());

		try (InputStream stream = loader.load("manifest.json")) {
			assertThat(stream).isNotNull();
			assertThat(stream.read()).isNotEqualTo(-1);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load manifest.json", e);
		}

		assertThatThrownBy(() -> loader.load("does_not_exists.txt"))
			.isInstanceOf(Exception.class);
	}
}

package app.pairs.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.asset.AudioRegistry;
import app.pairs.asset.ClspAssetLoader;

import org.junit.jupiter.api.Test;

class AudioRegistryTest {
	@Test
	void resolvesEliminateTileToBreakSound() throws Exception {
		AudioRegistry registry = AudioRegistry.load(
			new ClspAssetLoader(getClass().getClassLoader()),
			"audio_mapping.json"
		);

		assertThat(registry.resolve("eliminate", "rose_quartz_bircks"))
			.isEqualTo("EliminateSound/amethyst_block.break.wav");
		assertThat(registry.resolve("eliminate", "withered_leaves"))
			.isEqualTo("EliminateSound/grass.break.wav");
	}

	@Test
	void rejectsUnknownCategoryOrObject() throws Exception {
		AudioRegistry registry = AudioRegistry.load(
			new ClspAssetLoader(getClass().getClassLoader()),
			"audio_mapping.json"
		);

		assertThatThrownBy(() -> registry.resolve("ui", "rose_quartz_bircks"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Unknown audio category");
		assertThatThrownBy(() -> registry.resolve("eliminate", "missing_tile"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Unknown audio object");
	}
}

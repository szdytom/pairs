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
	void resolvesMusicCategories() throws Exception {
		AudioRegistry registry = AudioRegistry.load(
			new ClspAssetLoader(getClass().getClassLoader()),
			"audio_mapping.json"
		);

		assertThat(registry.resolve("LevelMusic", "minecraft"))
			.isEqualTo("BgMusicSound/minecraft_remix.wav");
		assertThat(registry.resolve("MainMusic", "wet_hand"))
			.isEqualTo("BgMusicSound/wet_hand_remix.wav");
	}

	@Test
	void recognizesMusicCategories() throws Exception {
		AudioRegistry registry = AudioRegistry.load(
			new ClspAssetLoader(getClass().getClassLoader()),
			"audio_mapping.json"
		);

		assertThat(registry.isMusic("LevelMusic")).isTrue();
		assertThat(registry.isMusic("MainMusic")).isTrue();
		assertThat(registry.isMusic("eliminate")).isFalse();
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

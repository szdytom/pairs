package app.pairs.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

class AudioWavAccessTest {
	@Test
	void wavSoundsAreAccessibleOnClasspath() throws Exception {
		try (
			InputStream
				stream = getClass().getClassLoader().getResourceAsStream(
					"EliminateSound/grass.break.wav"
				)
		) {
			assertThat(stream).isNotNull();

			// Verify RIFF header so we know it's a real WAV, not a zero-byte
			// file.
			byte[] header = stream.readNBytes(4);
			assertThat(new String(header)).isEqualTo("RIFF");
		}
	}

	@Test
	void musicIsAccessibleOnClasspath() throws Exception {
		try (
			InputStream
				stream = getClass().getClassLoader().getResourceAsStream(
					"BgMusicSound/minecraft_remix.wav"
				)
		) {
			assertThat(stream).isNotNull();

			byte[] header = stream.readNBytes(4);
			assertThat(new String(header)).isEqualTo("RIFF");
		}
	}
}

package app.pairs.audio;

final class AudioClip {
	private final byte[] data;
	private final int frequency;
	private final int channels;
	private final int sdlFormat;

	AudioClip(byte[] data, int frequency, int channels, int sdlFormat) {
		this.data = data;
		this.frequency = frequency;
		this.channels = channels;
		this.sdlFormat = sdlFormat;
	}

	byte[] data() {
		return data;
	}

	int frequency() {
		return frequency;
	}

	int channels() {
		return channels;
	}

	int sdlFormat() {
		return sdlFormat;
	}
}

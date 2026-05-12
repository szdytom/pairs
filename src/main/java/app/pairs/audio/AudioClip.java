package app.pairs.audio;

import com.sun.jna.Memory;

final class AudioClip {
	private final Memory mem;
	private final int length;
	private final int frequency;
	private final int channels;
	private final int sdlFormat;

	AudioClip(byte[] data, int frequency, int channels, int sdlFormat) {
		this.mem = new Memory(data.length);
		this.mem.write(0, data, 0, data.length);
		this.length = data.length;
		this.frequency = frequency;
		this.channels = channels;
		this.sdlFormat = sdlFormat;
	}

	Memory mem() {
		return mem;
	}

	int length() {
		return length;
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

	void copyBytes(int srcOffset, byte[] dest, int destOffset, int len) {
		mem.read(srcOffset, dest, destOffset, len);
	}
}

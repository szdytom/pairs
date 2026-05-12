package app.pairs.audio;

import static io.github.libsdl4j.api.audio.SdlAudio.*;
import static io.github.libsdl4j.api.error.SdlError.*;

import com.sun.jna.Memory;

import io.github.libsdl4j.api.audio.*;

/** Streaming music player with per-frame volume fade. Always loops the clip. */
final class MusicPlayer implements AutoCloseable {
	private static final int BUFFER_SAMPLES = 2_048;
	// ~50 ms of audio queued per refill
	private static final int CHUNK_MS = 50;
	// refill when queue drops below this threshold
	private static final int REFILL_AHEAD_MS = 150;

	private SDL_AudioDeviceID device;
	private AudioFormat openedFormat;

	private byte[] pcmBuf;
	private Memory pcmMem;

	private AudioClip clip;
	private int playPos; // byte offset into clip PCM

	private boolean active;
	private float curVolume = 0f;
	private float volumeDelta = 0f; // per ms

	/** Start playing clip from the beginning at volume 0. Always loops. */
	void play(AudioClip clip) {
		ensureDevice(AudioFormat.from(clip));
		this.clip = clip;
		this.playPos = 0;
		this.active = true;
		this.curVolume = 0f;
		this.volumeDelta = 0f;
		SDL_ClearQueuedAudio(device);
	}

	/**
	 * Begin a fade-in over {@code durationMs} ms, starting from
	 * {@code startingVolume}.
	 */
	void fadeIn(float durationMs, float startingVolume) {
		curVolume = startingVolume;
		if (durationMs <= 0f) {
			curVolume = 1f;
			volumeDelta = 0f;
		} else {
			volumeDelta = (1f - startingVolume) / durationMs;
		}
	}

	/** Begin a fade-out over {@code durationMs} ms. Stops playback at zero. */
	void fadeOut(float durationMs) {
		if (durationMs <= 0f) {
			stop();
		} else {
			volumeDelta = -curVolume / durationMs;
		}
	}

	/** Must be called every frame from the main thread. */
	void update(long deltaMs) {
		if (!active || clip == null || device == null) {
			return;
		}

		curVolume = clamp(curVolume + volumeDelta * deltaMs, 0f, 1f);

		if (volumeDelta < 0f && curVolume <= 0f) {
			stop();
			return;
		}

		int bytesPerMs = bytesPerMs();
		if (SDL_GetQueuedAudioSize(device) < REFILL_AHEAD_MS * bytesPerMs) {
			refill(CHUNK_MS * bytesPerMs);
		}
	}

	@Override
	public void close() {
		closeDevice();
		clip = null;
		active = false;
	}

	// -------------------------------------------------------------------------

	private void stop() {
		curVolume = 0f;
		volumeDelta = 0f;
		active = false;
		SDL_ClearQueuedAudio(device);
	}

	private int bytesPerMs() {
		// 16-bit PCM: 2 bytes per sample
		return openedFormat.channels() * 2 * openedFormat.frequency() / 1_000;
	}

	private void refill(int maxBytes) {
		int frameSize = openedFormat.channels() * 2;
		maxBytes = (maxBytes / frameSize) * frameSize;
		if (maxBytes <= 0) {
			return;
		}

		byte[] out = pcmBuf;
		if (out == null || out.length < maxBytes) {
			pcmBuf = out = new byte[maxBytes];
			pcmMem = new Memory(maxBytes);
		}
		int written = 0;

		// Copy looping chunks until the buffer is full.
		while (written < maxBytes) {
			int available = clip.length() - playPos;
			int toCopy = Math.min(available, maxBytes - written);
			clip.copyBytes(playPos, out, written, toCopy);
			written += toCopy;
			playPos += toCopy;
			if (playPos >= clip.length()) {
				playPos = 0;
			}
		}

		scaleS16(out, written, curVolume);

		pcmMem.write(0, out, 0, written);
		if (SDL_QueueAudio(device, pcmMem, written) != 0) {
			throw new IllegalStateException(
				"Unable to queue music audio: " + SDL_GetError()
			);
		}
	}

	/** Scale 16-bit signed little-endian PCM samples in-place. */
	private static void scaleS16(byte[] buf, int len, float volume) {
		for (int i = 0; i + 1 < len; i += 2) {
			int sample = (short)((buf[i] & 0xFF) | ((buf[i + 1] & 0xFF) << 8));
			sample = Math.max(
				-32_768, Math.min(32_767, (int)(sample * volume))
			);
			buf[i] = (byte)(sample & 0xFF);
			buf[i + 1] = (byte)((sample >> 8) & 0xFF);
		}
	}

	private void ensureDevice(AudioFormat format) {
		if (device != null && format.equals(openedFormat)) {
			return;
		}
		closeDevice();
		device = openDevice(format);
		openedFormat = format;
	}

	private void closeDevice() {
		if (device != null && device.longValue() != 0) {
			SDL_ClearQueuedAudio(device);
			SDL_CloseAudioDevice(device);
			device = null;
		}
		pcmBuf = null;
		if (pcmMem != null) {
			pcmMem.close();
			pcmMem = null;
		}
	}

	private static SDL_AudioDeviceID openDevice(AudioFormat format) {
		SDL_AudioSpec desired = new SDL_AudioSpec();
		desired.freq = format.frequency();
		desired.format = new SDL_AudioFormat(format.sdlFormat());
		desired.channels = (byte)format.channels();
		desired.samples = BUFFER_SAMPLES;

		SDL_AudioSpec obtained = new SDL_AudioSpec();
		SDL_AudioDeviceID dev = SDL_OpenAudioDevice(
			null, 0, desired, obtained, 0
		);
		if (dev == null || dev.longValue() == 0) {
			throw new IllegalStateException(
				"Unable to open music audio device: " + SDL_GetError()
			);
		}
		SDL_PauseAudioDevice(dev, 0);
		return dev;
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}

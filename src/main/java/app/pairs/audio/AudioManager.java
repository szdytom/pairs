package app.pairs.audio;

import static io.github.libsdl4j.api.audio.SdlAudio.*;
import static io.github.libsdl4j.api.error.SdlError.*;
import static io.github.libsdl4j.api.rwops.SdlRWops.*;

import app.pairs.asset.AssetLoader;
import app.pairs.asset.AssetLoaderInstance;
import app.pairs.asset.AudioRegistry;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import io.github.libsdl4j.api.audio.*;
import io.github.libsdl4j.api.rwops.SDL_RWops;

public final class AudioManager implements AutoCloseable {
	private static final String DEFAULT_MAPPING = "audio_mapping.json";
	private static final int BUFFER_SAMPLES = 2_048;
	// Limit the queue so stale sounds don't pile up if the game lags.
	private static final int QUEUE_CAPACITY = 8;
	private static AudioManager INSTANCE;

	// Accessed only from the audio thread after init().
	private final Map<String, AudioClip> clips = new HashMap<>();
	private SDL_AudioDeviceID device;
	private int deviceFrequency;
	private int deviceChannels;
	private int deviceSdlFormat;

	// Written once by init(), then read-only from both threads.
	private AssetLoader loader;
	private AudioRegistry mapping;

	// play() drops new requests when full rather than blocking the caller.
	private final BlockingQueue<String> pendingPaths = new ArrayBlockingQueue<>(
		QUEUE_CAPACITY
	);
	private Thread audioThread;

	public static AudioManager instance() {
		if (INSTANCE == null) {
			INSTANCE = new AudioManager();
		}
		return INSTANCE;
	}

	private AudioManager() {}

	public void init() throws Exception {
		init(DEFAULT_MAPPING);
	}

	public void init(String mappingPath) throws Exception {
		loader = AssetLoaderInstance.getInstance();
		mapping = AudioRegistry.load(loader, mappingPath);

		audioThread = new Thread(this::audioLoop, "audio-worker");
		audioThread.setDaemon(true);
		audioThread.start();
	}

	/** Called from the main thread — non-blocking. */
	public void play(String category, String object) {
		if (mapping == null || loader == null) {
			throw new IllegalStateException("AudioManager is not initialized");
		}
		if (object == null) {
			return; // empty tile, no audio
		}
		if (!mapping.has(category, object)) {
			throw new IllegalArgumentException(
				"No audio mapping for " + category + "/" + object
			);
		}
		String path = mapping.resolve(category, object);
		pendingPaths.offer(path); // drops silently if queue is full
	}

	@Override
	public void close() {
		if (audioThread != null) {
			audioThread.interrupt();
			try {
				audioThread.join(2_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		clips.clear();
	}

	// ---- audio thread -------------------------------------------------------

	private void audioLoop() {
		// Pre-load every clip and open the device before waiting for requests.
		// This moves all I/O and SDL_OpenAudioDevice overhead to startup time,
		// so subsequent play() calls have negligible latency.
		for (String path : mapping.allPaths()) {
			clips.computeIfAbsent(path, this::loadClip);
		}
		if (!clips.isEmpty()) {
			openDeviceFor(clips.values().iterator().next());
		}

		try {
			while (!Thread.currentThread().isInterrupted()) {
				String path = pendingPaths.take(); // blocks until work arrives
				AudioClip clip = clips.computeIfAbsent(path, this::loadClip);
				openDeviceFor(clip);
				queueAudio(clip);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			closeDevice();
		}
	}

	private void closeDevice() {
		if (device != null && device.longValue() != 0) {
			SDL_CloseAudioDevice(device);
			device = null;
		}
	}

	private AudioClip loadClip(String path) {
		try (InputStream stream = loader.load(path)) {
			byte[] bytes = stream.readAllBytes();
			Memory mem = new Memory(bytes.length);
			mem.write(0, bytes, 0, bytes.length);

			SDL_RWops rw = SDL_RWFromMem(mem, bytes.length);
			SDL_AudioSpec spec = new SDL_AudioSpec();
			PointerByReference bufRef = new PointerByReference();
			IntByReference lenRef = new IntByReference();

			if (SDL_LoadWAV_RW(rw, 1, spec, bufRef, lenRef) == null) {
				throw new IllegalStateException(
					"Unable to load WAV '" + path + "': " + SDL_GetError()
				);
			}

			int len = lenRef.getValue();
			byte[] data = bufRef.getValue().getByteArray(0, len);
			SDL_FreeWAV(bufRef.getValue());

			return new AudioClip(
				data, spec.freq, spec.channels, spec.format.intValue()
			);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Unable to load audio: " + path, e);
		}
	}

	private void openDeviceFor(AudioClip clip) {
		if (device != null && device.longValue() != 0
		    && deviceFrequency == clip.frequency()
		    && deviceChannels == clip.channels()
		    && deviceSdlFormat == clip.sdlFormat()) {
			return;
		}
		closeDevice();

		SDL_AudioSpec desired = new SDL_AudioSpec();
		desired.freq = clip.frequency();
		desired.format = new SDL_AudioFormat(clip.sdlFormat());
		desired.channels = (byte)clip.channels();
		desired.samples = BUFFER_SAMPLES;

		SDL_AudioSpec obtained = new SDL_AudioSpec();
		device = SDL_OpenAudioDevice(null, 0, desired, obtained, 0);
		if (device == null || device.longValue() == 0) {
			throw new IllegalStateException(
				"Unable to open audio device: " + SDL_GetError()
			);
		}

		deviceFrequency = obtained.freq;
		deviceChannels = obtained.channels;
		deviceSdlFormat = obtained.format.intValue();
		SDL_PauseAudioDevice(device, 0);
	}

	private void queueAudio(AudioClip clip) {
		SDL_ClearQueuedAudio(device);
		if (SDL_QueueAudio(device, clip.mem(), clip.length()) != 0) {
			throw new IllegalStateException(
				"Unable to queue audio: " + SDL_GetError()
			);
		}
	}
}

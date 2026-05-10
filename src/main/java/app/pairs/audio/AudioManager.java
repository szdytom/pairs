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

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import io.github.libsdl4j.api.audio.*;
import io.github.libsdl4j.api.rwops.SDL_RWops;

public final class AudioManager implements AutoCloseable {
	private static final String DEFAULT_MAPPING = "audio_mapping.json";
	private static final int BUFFER_SAMPLES = 2_048;
	private static AudioManager INSTANCE;

	private final Map<String, AudioClip> clips = new HashMap<>();
	private AssetLoader loader;
	private AudioRegistry mapping;
	private SDL_AudioDeviceID device;
	private int deviceFrequency;
	private int deviceChannels;
	private int deviceSdlFormat;

	public static AudioManager instance() {
		if (INSTANCE == null) {
			INSTANCE = new AudioManager();
		}
		return INSTANCE;
	}

	private AudioManager() {}

	public synchronized void init() throws Exception {
		init(DEFAULT_MAPPING);
	}

	public synchronized void init(String mappingPath) throws Exception {
		loader = AssetLoaderInstance.getInstance();
		mapping = AudioRegistry.load(loader, mappingPath);
	}

	public synchronized void play(String category, String object) {
		requireInitialized();
		String path = mapping.resolve(category, object);
		AudioClip clip = clips.computeIfAbsent(path, this::loadClip);
		openDeviceFor(clip);
		queue(clip);
	}

	@Override
	public synchronized void close() {
		closeDevice();
		clips.clear();
	}

	private void closeDevice() {
		if (device != null && device.longValue() != 0) {
			SDL_CloseAudioDevice(device);
			device = null;
		}
	}

	private void requireInitialized() {
		if (mapping == null || loader == null) {
			throw new IllegalStateException("AudioManager is not initialized");
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

	private void queue(AudioClip clip) {
		byte[] data = clip.data();
		Memory memory = new Memory(data.length);
		memory.write(0, data, 0, data.length);
		if (SDL_QueueAudio(device, memory, data.length) != 0) {
			throw new IllegalStateException(
				"Unable to queue audio: " + SDL_GetError()
			);
		}
	}
}

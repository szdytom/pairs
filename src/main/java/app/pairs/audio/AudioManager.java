package app.pairs.audio;

import static io.github.libsdl4j.api.audio.SdlAudio.*;
import static io.github.libsdl4j.api.error.SdlError.*;
import static io.github.libsdl4j.api.rwops.SdlRWops.*;

import app.pairs.asset.AssetLoader;
import app.pairs.asset.AssetLoaderInstance;
import app.pairs.asset.AudioRegistry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import io.github.libsdl4j.api.audio.*;
import io.github.libsdl4j.api.rwops.SDL_RWops;

public final class AudioManager implements AutoCloseable {
	private static final String DEFAULT_MAPPING = "audio_mapping.json";
	private static final int BUFFER_SAMPLES = 2_048;
	private static final int SFX_PLAYER_COUNT = 7;
	private static final long TRACK_GAP_MS = 5_000L;
	private static AudioManager INSTANCE;

	private final Map<String, AudioClip> clips = new HashMap<>();
	private final List<AudioPlayer> sfxPlayers = new ArrayList<>();
	private final Random random = new Random();
	private MusicPlayer musicPlayer;

	private AssetLoader loader;
	private AudioRegistry mapping;

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
		if (mapping != null) {
			throw new IllegalStateException(
				"AudioManager is already initialized"
			);
		}
		loader = AssetLoaderInstance.getInstance();
		mapping = AudioRegistry.load(loader, mappingPath);

		preloadClips();
		createPlayers();
	}

	public void play(String category, String object) {
		playWithFadeIn(category, object, 0f);
	}

	public void shufflePlay(String category) {
		shufflePlayWithFadeIn(category, 0f);
	}

	public void shufflePlayWithFadeIn(String category, float fadeInMs) {
		ensureInitialized();
		List<String> objects = objectsFor(category);
		String object = randomObject(objects);
		AudioClip clip = clipFor(category, object);
		ensureLoopCompatible(category, objects, AudioFormat.from(clip));
		musicPlayer.play(
			clip, () -> clipFor(category, randomObject(objects)), TRACK_GAP_MS
		);
		musicPlayer.fadeIn(fadeInMs, fadeInMs > 0f ? 0f : 1f);
	}

	public void playRandom(String category) {
		ensureInitialized();
		List<String> objects = objectsFor(category);
		play(category, randomObject(objects));
	}

	public void stopMusic() {
		fadeOutMusic(3000f);
	}

	public void playWithFadeIn(String category, String object, float fadeInMs) {
		ensureInitialized();
		if (object == null) {
			return;
		}
		AudioClip clip = clipFor(category, object);

		if (mapping.isMusic(category)) {
			musicPlayer.play(clip);
			musicPlayer.fadeIn(fadeInMs, fadeInMs > 0f ? 0f : 1f);
		} else {
			AudioPlayer player = findIdlePlayer();
			if (player != null) {
				player.play(clip);
			}
		}
	}

	@Override
	public void close() {
		if (musicPlayer != null) {
			musicPlayer.close();
			musicPlayer = null;
		}
		closePlayers();
		clips.clear();
		mapping = null;
		loader = null;
	}

	/** Must be called every frame; drives music fade and streaming refill. */
	public void update(long deltaMs) {
		if (musicPlayer != null) {
			musicPlayer.update(deltaMs);
		}
	}

	/** Fade out the currently playing music over {@code durationMs} ms. */
	public void fadeOutMusic(float durationMs) {
		if (musicPlayer != null) {
			musicPlayer.fadeOut(durationMs);
		}
	}

	private void ensureInitialized() {
		if (mapping == null || loader == null || musicPlayer == null) {
			throw new IllegalStateException("AudioManager is not initialized");
		}
	}

	private AudioClip clipFor(String category, String object) {
		if (!mapping.has(category, object)) {
			throw new IllegalArgumentException(
				"No audio mapping for " + category + "/" + object
			);
		}
		String path = mapping.resolve(category, object);
		AudioClip clip = clips.get(path);
		if (clip == null) {
			throw new IllegalStateException(
				"Audio clip is not loaded: " + path
			);
		}
		return clip;
	}

	private List<String> objectsFor(String category) {
		List<String> objects = mapping.objectsIn(category);
		if (objects.isEmpty()) {
			throw new IllegalArgumentException(
				"No audio objects for category: " + category
			);
		}
		return objects;
	}

	private String randomObject(List<String> objects) {
		return objects.get(random.nextInt(objects.size()));
	}

	private void ensureLoopCompatible(
		String category, List<String> objects, AudioFormat expected
	) {
		for (String object : objects) {
			AudioFormat format = AudioFormat.from(clipFor(category, object));
			if (!format.equals(expected)) {
				throw new IllegalStateException(
					"Shuffled audio category has mixed formats: " + category
				);
			}
		}
	}

	private void preloadClips() {
		for (String path : mapping.allPaths()) {
			clips.computeIfAbsent(path, this::loadClip);
		}
	}

	private void createPlayers() {
		List<AudioFormat> sfxFmts = sfxFormats();
		if (!sfxFmts.isEmpty()) {
			try {
				for (int i = 0; i < SFX_PLAYER_COUNT; i++) {
					sfxPlayers.add(new AudioPlayer(sfxFmts));
				}
			} catch (RuntimeException e) {
				closePlayers();
				throw e;
			}
		}
		musicPlayer = new MusicPlayer();
	}

	private List<AudioFormat> sfxFormats() {
		Set<AudioFormat> formats = new LinkedHashSet<>();
		for (String path : mapping.sfxPaths()) {
			AudioClip clip = clips.get(path);
			if (clip != null) {
				formats.add(AudioFormat.from(clip));
			}
		}
		return new ArrayList<>(formats);
	}

	private AudioPlayer findIdlePlayer() {
		for (AudioPlayer player : sfxPlayers) {
			if (player.isIdle()) {
				return player;
			}
		}
		return null;
	}

	private void closePlayers() {
		for (AudioPlayer player : sfxPlayers) {
			player.close();
		}
		sfxPlayers.clear();
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

	private static final class AudioPlayer implements AutoCloseable {
		private final Map<AudioFormat, SDL_AudioDeviceID>
			devices = new HashMap<>();

		AudioPlayer(List<AudioFormat> formats) {
			for (AudioFormat format : formats) {
				devices.put(format, openDevice(format));
			}
		}

		boolean isIdle() {
			for (SDL_AudioDeviceID device : devices.values()) {
				if (SDL_GetQueuedAudioSize(device) > 0) {
					return false;
				}
			}
			return true;
		}

		void play(AudioClip clip) {
			AudioFormat format = AudioFormat.from(clip);
			SDL_AudioDeviceID device = devices.get(format);
			if (device == null) {
				throw new IllegalStateException(
					"No audio player for clip format: " + format
				);
			}

			SDL_ClearQueuedAudio(device);
			if (SDL_QueueAudio(device, clip.mem(), clip.length()) != 0) {
				throw new IllegalStateException(
					"Unable to queue audio: " + SDL_GetError()
				);
			}
		}

		@Override
		public void close() {
			for (SDL_AudioDeviceID device : devices.values()) {
				if (device != null && device.longValue() != 0) {
					SDL_ClearQueuedAudio(device);
					SDL_CloseAudioDevice(device);
				}
			}
			devices.clear();
		}

		private static SDL_AudioDeviceID openDevice(AudioFormat format) {
			SDL_AudioSpec desired = new SDL_AudioSpec();
			desired.freq = format.frequency();
			desired.format = new SDL_AudioFormat(format.sdlFormat());
			desired.channels = (byte)format.channels();
			desired.samples = BUFFER_SAMPLES;

			SDL_AudioSpec obtained = new SDL_AudioSpec();
			SDL_AudioDeviceID device = SDL_OpenAudioDevice(
				null, 0, desired, obtained, 0
			);
			if (device == null || device.longValue() == 0) {
				throw new IllegalStateException(
					"Unable to open audio device: " + SDL_GetError()
				);
			}

			SDL_PauseAudioDevice(device, 0);
			return device;
		}
	}
}

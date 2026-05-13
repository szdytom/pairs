# Audio System

## Overview

The audio system has two independent tracks:

- **SFX pool** — 7 × `AudioPlayer` instances, fire-and-forget, picked round-robin from idle players.
- **Music player** — 1 × `MusicPlayer`, streaming with per-frame fade-in/out. Always loops. Only one music track plays at a time.

Entry point: `AudioManager` singleton. Callers never touch `AudioPlayer` or `MusicPlayer` directly.

---

## AudioManager API

```java
AudioManager.instance().init();                            // call once at startup
AudioManager.instance().play(category, object);            // play SFX or music instantly
AudioManager.instance().playWithFadeIn(category, object, fadeInMs); // play music with fade-in
AudioManager.instance().loopPlay(category, object);        // loop one object on the music player
AudioManager.instance().loopPlay(category);                // randomly loop objects in a category
AudioManager.instance().shufflePlay(category);             // same random category loop, explicit name
AudioManager.instance().playRandom(category);              // play one random object from a category
AudioManager.instance().fadeOutMusic(durationMs);          // fade out current music
AudioManager.instance().stopMusic();                       // stop current music immediately
AudioManager.instance().update(deltaMs);                   // call every frame
AudioManager.instance().close();                           // call at shutdown
```

`update()` must be called each frame (done in `Main`). It advances volume ramps and refills the SDL audio queue for the music player.

---

## Audio Registry (`audio_mapping.json`)

Categories map object names → file paths. Each category entry:

| Field | Required | Description |
|-------|----------|-------------|
| `file` | yes | External JSON with object→event→filename mappings |
| `objectRoot` | yes | Top-level key in that JSON to read from |
| `event` | yes | Event key within each object entry |
| `basePath` | yes | Prefix prepended to every resolved filename |
| `type` | no | Set to `"music"` to route to `MusicPlayer` instead of SFX pool |

Example — `audio_mapping.json`:
```json
{
  "categories": {
    "eliminate": {
      "file": "sound-eliminate.json",
      "objectRoot": "blocks",
      "event": "break",
      "basePath": "EliminateSound"
    },
    "LevelMusic": {
      "type": "music",
      "file": "sound-music.json",
      "objectRoot": "LevelMusic",
      "event": "play",
      "basePath": "BgMusicSound"
    }
  }
}
```

Resolved path: `basePath/filename` (e.g. `BgMusicSound/minecraft_remix.wav`).

---

## MusicPlayer Internals

Streaming model: each `update()` call refills the SDL queue in `CHUNK_MS = 50 ms` chunks when the queued size drops below `REFILL_AHEAD_MS = 150 ms`. Looping is handled by asking the current loop supplier for the next clip when `playPos` reaches the end of the current clip. The default supplier returns the same clip; shuffle playback supplies a random clip from the selected category.

Volume ramp: `curVolume += volumeDelta * deltaMs` each frame. Fade-out stops and clears the queue when volume reaches 0.

The PCM byte buffer and JNA `Memory` block are preallocated on first use and reused across refill calls to avoid per-frame allocation.

---

## Audio File Requirements

All audio files must be **48 kHz WAV**. Use `ffmpeg` to convert if needed:

```sh
ffmpeg -i input.mp3 -ar 48000 output.wav
```

- SFX (`EliminateSound/`): mono
- Music (`BgMusicSound/`): stereo

---

## Adding Music to a Page

In a `Page` implementation:

```java
@Override
public void onEnter() {
    AudioManager.instance().playWithFadeIn("LevelMusic", "minecraft", 1000f);
}

@Override
public void onExit() {
    AudioManager.instance().fadeOutMusic(500f);
}
```

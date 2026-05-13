# Audio System

## Overview

The audio system has two independent tracks:

- **SFX pool** — 7 × `AudioPlayer` instances, fire-and-forget, picked round-robin from idle players.
- **Music player** — 1 × `MusicPlayer`, streaming with per-frame fade-in/out. Only one music track plays at a time.

Entry point: `AudioManager` singleton. Callers never touch `AudioPlayer` or `MusicPlayer` directly.

---

## AudioManager API

```java
AudioManager.instance().init();                                    // call once at startup
AudioManager.instance().play(category, object);                    // play SFX or music instantly
AudioManager.instance().playWithFadeIn(category, object, fadeInMs); // play music with fade-in
AudioManager.instance().shufflePlay(category);                     // shuffle-loop a category (no fade)
AudioManager.instance().shufflePlayWithFadeIn(category, fadeInMs); // shuffle-loop with fade-in
AudioManager.instance().playRandom(category);                      // play one random object from a category
AudioManager.instance().fadeOutMusic(durationMs);                  // fade out current music
AudioManager.instance().stopMusic();                               // fade out over 3 s
AudioManager.instance().update(deltaMs);                           // call every frame
AudioManager.instance().close();                                   // call at shutdown
```

`update()` must be called each frame (done in `Main`). It advances volume ramps and refills the SDL audio queue for the music player.

### Shuffle playback behaviour

`shufflePlay` / `shufflePlayWithFadeIn` pick a random object from the category to start, then supply a new random object each time the current clip ends. A **5-second silence** is inserted between tracks. A new call while music is already playing (or waiting between tracks) is silently ignored. A new call during a fade-out is queued and starts automatically after the fade completes.

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

**Streaming**: each `update()` call refills the SDL queue in `CHUNK_MS = 50 ms` chunks when the queued size drops below `REFILL_AHEAD_MS = 150 ms`. When `playPos` reaches the end of the current clip, the loop supplier is called for the next clip.

**Volume ramp**: `curVolume += volumeDelta * deltaMs` each frame. Fade-out stops and clears the queue when `curVolume` reaches 0.

**Pause between tracks**: when a clip ends and `pauseMs > 0`, the player enters a `waiting` state for `pauseMs` ms before advancing to the next clip. `shufflePlayWithFadeIn` uses a 5-second pause.

**Pending queue**: calling `play()` during a fade-out queues the new clip (and its `fadeIn` parameters) as _pending_. When the fade-out completes, the pending clip starts automatically. A `play()` call while playing normally (not fading out) is silently ignored; the paired `fadeIn()` is ignored to match.

**PCM buffer**: the byte buffer and JNA `Memory` block are preallocated on first use and reused to avoid per-frame allocation.

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
    AudioManager.instance().shufflePlayWithFadeIn("LevelMusic", 3000f);
}

@Override
public void onExit() {
    AudioManager.instance().fadeOutMusic(3000f);
}
```

The fade-out from `onExit()` will complete before the next page's music begins, thanks to the pending-play queue.

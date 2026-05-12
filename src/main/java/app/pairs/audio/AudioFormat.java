package app.pairs.audio;

final record AudioFormat(int frequency, int channels, int sdlFormat) {
	static AudioFormat from(AudioClip clip) {
		return new AudioFormat(
			clip.frequency(), clip.channels(), clip.sdlFormat()
		);
	}
}

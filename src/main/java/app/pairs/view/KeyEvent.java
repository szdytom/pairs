package app.pairs.view;

/**
 * Keyboard-specific event carrying an SDL keycode.
 */
public class KeyEvent extends Event {
	private final int keycode;

	public KeyEvent(Type type, int keycode) {
		super(type);
		this.keycode = keycode;
	}

	public int keycode() {
		return keycode;
	}
}

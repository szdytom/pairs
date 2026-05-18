package app.pairs.view;

/**
 * Keyboard-specific event carrying an SDL keycode and modifiers.
 */
public class KeyEvent extends Event {
	private final int keycode;
	private final int modifiers;

	public KeyEvent(Type type, int keycode, int modifiers) {
		super(type);
		this.keycode = keycode;
		this.modifiers = modifiers;
	}

	public int keycode() {
		return keycode;
	}

	public int modifiers() {
		return modifiers;
	}
}

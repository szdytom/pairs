package app.pairs.view;

/**
 * Base class for all UI events dispatched through the component tree.
 * Events carry a type and can be consumed to stop propagation.
 */
public class Event {
	public enum Type {
		MOUSE_MOVED,
		MOUSE_PRESSED,
		MOUSE_RELEASED,
		MOUSE_LEAVE,
		KEY_PRESSED,
		KEY_RELEASED,
	}

	protected final Type type;
	private boolean consumed;

	public Event(Type type) {
		this.type = type;
	}

	public Type type() {
		return type;
	}

	public boolean isConsumed() {
		return consumed;
	}

	public void consume() {
		consumed = true;
	}
}

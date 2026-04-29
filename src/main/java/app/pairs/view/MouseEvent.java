package app.pairs.view;

/**
 * Mouse-specific event carrying cursor position (root-relative logical pixels)
 * and button identifier.
 */
public class MouseEvent extends Event {
	private final int x;
	private final int y;
	private final int button;

	public MouseEvent(Type type, int x, int y, int button) {
		super(type);
		this.x = x;
		this.y = y;
		this.button = button;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int button() {
		return button;
	}
}

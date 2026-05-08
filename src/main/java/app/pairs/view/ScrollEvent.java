package app.pairs.view;

public class ScrollEvent extends Event {
	private final int x;
	private final int y;
	private final int delta;

	public ScrollEvent(Type type, int x, int y, int delta) {
		super(type);
		this.x = x;
		this.y = y;
		this.delta = delta;
	}

	public int x() {
		return x;
	}
	public int y() {
		return y;
	}
	public int delta() {
		return delta;
	}
}

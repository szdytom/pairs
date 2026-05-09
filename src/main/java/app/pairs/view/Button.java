package app.pairs.view;

public class Button extends Container {
	private final Runnable onClick;
	private final int[] measuredSize = new int[2];

	public Button(Runnable onClick) {
		this.onClick = onClick;
	}

	@Override
	public int[] measure() {
		int maxW = 0;
		int maxH = 0;
		for (Widget child : children) {
			int[] s = child.measure();
			if (s[0] > maxW) {
				maxW = s[0];
			}
			if (s[1] > maxH) {
				maxH = s[1];
			}
		}
		measuredSize[0] = maxW;
		measuredSize[1] = maxH;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		for (Widget child : children) {
			child.layout(0, 0, w, h);
		}
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
			if (me.type() == Event.Type.MOUSE_PRESSED && me.button() == 1) {
				if (onClick != null) {
					onClick.run();
				}
				return true;
			}
		}
		return super.onEvent(event);
	}
}

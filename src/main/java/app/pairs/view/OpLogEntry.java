package app.pairs.view;

/**
 * A single entry in the operation history log, displayed as a
 * {@code (r1,c1)→(r2,c2)} line.
 */
public class OpLogEntry extends TextComponent {
	private static final int FONT_SIZE = 1;
	private static final int R = 180;
	private static final int G = 180;
	private static final int B = 180;

	public OpLogEntry(int r1, int c1, int r2, int c2) {
		super(
			"(" + r1 + "," + c1 + ")→(" + r2 + "," + c2 + ")", FONT_SIZE, R, G,
			B
		);
	}
}

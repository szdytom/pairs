package app.pairs.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Tests for {@link GridLayout}: measure, grid layout, gap, and padding.
 */
class GridLayoutTest {
	private static class FixedWidget extends Widget {
		private final int w;
		private final int h;

		FixedWidget(int w, int h) {
			this.w = w;
			this.h = h;
		}

		@Override
		public int[] measure() {
			return new int[] {w, h};
		}

		@Override
		public void render(
			SDL_Renderer renderer, int parentX, int parentY, int scale
		) {}

		@Override
		public void layout(int x, int y, int w, int h) {
			this.layoutX = x;
			this.layoutY = y;
			this.layoutW = w;
			this.layoutH = h;
		}
	}

	// ---- measure ----------------------------------------------------------

	@Test
	void measureEmpty() {
		GridLayout g = new GridLayout(3, 0, 0);
		assertArrayEquals(new int[] {0, 0}, g.measure());
	}

	@Test
	void measureSingleChild() {
		GridLayout g = new GridLayout(3, 0, 0);
		g.addChild(new FixedWidget(30, 20));
		assertArrayEquals(new int[] {3 * 30, 20}, g.measure());
	}

	@Test
	void measureMultipleChildrenSingleRow() {
		GridLayout g = new GridLayout(3, 0, 0);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		g.addChild(new FixedWidget(40, 30));
		assertArrayEquals(new int[] {3 * 50, 30}, g.measure());
	}

	@Test
	void measureMultipleChildrenMultipleRows() {
		GridLayout g = new GridLayout(2, 0, 0);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		g.addChild(new FixedWidget(40, 30));
		assertArrayEquals(new int[] {2 * 50, 2 * 30}, g.measure());
	}

	@Test
	void measureIncludesGaps() {
		GridLayout g = new GridLayout(3, 4, 5);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		g.addChild(new FixedWidget(40, 30));
		assertArrayEquals(new int[] {3 * 50 + 2 * 4, 30}, g.measure());
	}

	@Test
	void measureIncludesPadding() {
		GridLayout g = new GridLayout(2, 0, 0, 3);
		g.addChild(new FixedWidget(30, 20));
		assertArrayEquals(new int[] {2 * 30 + 6, 20 + 6}, g.measure());
	}

	@Test
	void measureIncludesGapAndPadding() {
		GridLayout g = new GridLayout(2, 2, 3, 3);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		int expW = 2 * 50 + 1 * 2 + 6;
		int expH = 20 + 6;
		assertArrayEquals(new int[] {expW, expH}, g.measure());
	}

	// ---- layout: natural cell size (w=0, h=0 after measure) ---------------
	// measure() must be called before layout() to populate cached cellSize.

	@Test
	void layoutPositionsChildrenInRowMajorOrder() {
		GridLayout g = new GridLayout(2, 0, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		FixedWidget c = new FixedWidget(40, 30);
		g.addChild(a);
		g.addChild(b);
		g.addChild(c);
		g.measure();
		g.layout(0, 0, 0, 0);

		assertEquals(0, a.layoutX);
		assertEquals(0, a.layoutY);
		assertEquals(50, b.layoutX);
		assertEquals(0, b.layoutY);
		assertEquals(0, c.layoutX);
		assertEquals(30, c.layoutY);
	}

	@Test
	void layoutWithGap() {
		GridLayout g = new GridLayout(2, 4, 5);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		g.measure();
		g.layout(0, 0, 0, 0);

		assertEquals(0, a.layoutX);
		assertEquals(50 + 4, b.layoutX);
	}

	@Test
	void layoutWithPadding() {
		GridLayout g = new GridLayout(2, 0, 0, 3);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		g.measure();
		g.layout(0, 0, 0, 0);

		assertEquals(3, a.layoutX);
		assertEquals(3, a.layoutY);
		assertEquals(3 + 50, b.layoutX);
		assertEquals(3, b.layoutY);
	}

	@Test
	void layoutWithGapAndPadding() {
		GridLayout g = new GridLayout(2, 2, 3, 3);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		g.measure();
		g.layout(0, 0, 0, 0);

		assertEquals(3, a.layoutX);
		assertEquals(3, a.layoutY);
		assertEquals(3 + 50 + 2, b.layoutX);
		assertEquals(3, b.layoutY);
	}

	// ---- layout: computed cell size (fills given space) -------------------

	@Test
	void layoutUsesComputedCellSizeWhenGivenSpace() {
		GridLayout g = new GridLayout(2, 0, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		g.layout(0, 0, 120, 60);

		assertEquals(0, a.layoutX);
		assertEquals(0, a.layoutY);
		assertEquals(60, a.layoutW);
		assertEquals(60, a.layoutH);
		assertEquals(60, b.layoutX);
	}

	@Test
	void layoutWithPaddingUsesComputedCellSize() {
		GridLayout g = new GridLayout(2, 0, 0, 5);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		g.layout(0, 0, 120, 60);

		assertEquals(5, a.layoutX);
		assertEquals(5, a.layoutY);
		assertEquals(55, a.layoutW);
		assertEquals(50, a.layoutH);
		assertEquals(5 + 55, b.layoutX);
	}

	// ---- edge cases -------------------------------------------------------

	@Test
	void layoutEmptyNoOp() {
		GridLayout g = new GridLayout(3, 0, 0);
		g.layout(0, 0, 100, 50);
	}

	@Test
	void singleColumnGrid() {
		GridLayout g = new GridLayout(1, 0, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		assertArrayEquals(new int[] {50, 40}, g.measure());

		g.layout(0, 0, 0, 0);
		assertEquals(0, a.layoutX);
		assertEquals(0, a.layoutY);
		assertEquals(50, a.layoutW);
		assertEquals(20, a.layoutH);
		assertEquals(0, b.layoutX);
		assertEquals(20, b.layoutY);
		assertEquals(50, b.layoutW);
		assertEquals(20, b.layoutH);
	}

	@Test
	void throwsOnInvalidColumns() {
		assertThrows(
			IllegalArgumentException.class, () -> new GridLayout(0, 0, 0)
		);
	}

	@Test
	void throwsOnNegativeGap() {
		assertThrows(
			IllegalArgumentException.class, () -> new GridLayout(3, -1, 0)
		);
		assertThrows(
			IllegalArgumentException.class, () -> new GridLayout(3, 0, -1)
		);
	}

	// ---- auto-column-widths mode ------------------------------------------

	@Test
	void autoColumnMeasureSingleChild() {
		GridLayout g = new GridLayout(3, 0, 0, 0, true);
		g.addChild(new FixedWidget(30, 20));
		// col0=30, col1=0, col2=0
		assertArrayEquals(new int[] {30, 20}, g.measure());
	}

	@Test
	void autoColumnMeasureMultipleChildrenSingleRow() {
		GridLayout g = new GridLayout(3, 0, 0, 0, true);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		g.addChild(new FixedWidget(40, 30));
		// col0=30, col1=50, col2=40  →  total=120, height=30
		assertArrayEquals(new int[] {30 + 50 + 40, 30}, g.measure());
	}

	@Test
	void autoColumnMeasureMultipleRows() {
		GridLayout g = new GridLayout(2, 0, 0, 0, true);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		g.addChild(new FixedWidget(40, 30));
		// row0: col0=30, col1=50;  row1: col0=40
		// col0=max(30,40)=40, col1=50  →  90,  height=2*30=60
		assertArrayEquals(new int[] {90, 60}, g.measure());
	}

	@Test
	void autoColumnMeasureIncludesGaps() {
		GridLayout g = new GridLayout(2, 4, 5, 0, true);
		g.addChild(new FixedWidget(30, 20));
		g.addChild(new FixedWidget(50, 10));
		// col0=30, col1=50, gapX=4  →  30+4+50=84, height=20
		assertArrayEquals(new int[] {84, 20}, g.measure());
	}

	@Test
	void autoColumnMeasureIncludesPadding() {
		GridLayout g = new GridLayout(2, 0, 0, 3, true);
		g.addChild(new FixedWidget(30, 20));
		// col0=30, col1=0, padding=3  →  30+6=36, height=20+6=26
		assertArrayEquals(new int[] {36, 26}, g.measure());
	}

	@Test
	void autoColumnMeasureAllInvisibleReturnsZero() {
		GridLayout g = new GridLayout(2, 0, 0, 0, true);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		a.setVisible(false);
		b.setVisible(false);
		g.addChild(a);
		g.addChild(b);
		assertArrayEquals(new int[] {0, 0}, g.measure());
	}

	@Test
	void autoColumnMeasureCountsOnlyVisibleChildren() {
		GridLayout g = new GridLayout(2, 0, 0, 0, true);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		b.setVisible(false);
		g.addChild(a);
		g.addChild(b);
		// only 'a' visible at col0 → col0=30, col1=0, rows=1
		assertArrayEquals(new int[] {30, 20}, g.measure());
	}

	@Test
	void autoColumnLayoutPositionsByColumnWidth() {
		GridLayout g = new GridLayout(2, 0, 0, 0, true);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		FixedWidget c = new FixedWidget(40, 30);
		g.addChild(a);
		g.addChild(b);
		g.addChild(c);
		g.measure();
		g.layout(0, 0, 0, 0);

		assertEquals(0, a.layoutX); // col0, cw=40
		assertEquals(40, a.layoutW);
		assertEquals(40, b.layoutX); // after col0 width
		assertEquals(50, b.layoutW);
		assertEquals(0, c.layoutX); // col0, row1
		assertEquals(40, c.layoutW);
		assertEquals(30, c.layoutY); // row1
	}

	@Test
	void autoColumnLayoutWithGap() {
		GridLayout g = new GridLayout(2, 4, 0, 0, true);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		g.addChild(a);
		g.addChild(b);
		g.measure();
		g.layout(0, 0, 0, 0);

		assertEquals(0, a.layoutX);
		assertEquals(30, a.layoutW);
		assertEquals(30 + 4, b.layoutX);
		assertEquals(50, b.layoutW);
	}

	@Test
	void autoColumnLayoutSkipsInvisibleChildren() {
		GridLayout g = new GridLayout(2, 0, 0, 0, true);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		FixedWidget c = new FixedWidget(40, 30);
		b.setVisible(false);
		g.addChild(a);
		g.addChild(b);
		g.addChild(c);
		g.measure();
		g.layout(0, 0, 0, 0);

		// a at index 0 (col=0), c at index 1 (col=1)
		assertEquals(0, a.layoutX);
		assertEquals(30, a.layoutW);
		assertEquals(30, c.layoutX); // after col0 width
		assertEquals(40, c.layoutW);
	}

	// ---- visibility -------------------------------------------------------

	@Test
	void measureAllInvisibleReturnsZero() {
		GridLayout g = new GridLayout(2, 0, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		a.setVisible(false);
		b.setVisible(false);
		g.addChild(a);
		g.addChild(b);
		assertArrayEquals(new int[] {0, 0}, g.measure());
	}

	@Test
	void measureCountsOnlyVisibleChildren() {
		GridLayout g = new GridLayout(2, 0, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		b.setVisible(false);
		g.addChild(a);
		g.addChild(b);
		// Only 'a' visible: 1 visible child, 1 row, columns=2
		assertArrayEquals(new int[] {2 * 30, 20}, g.measure());
	}

	@Test
	void layoutSkipsInvisibleChildren() {
		GridLayout g = new GridLayout(2, 0, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		FixedWidget c = new FixedWidget(40, 30);
		b.setVisible(false);
		g.addChild(a);
		g.addChild(b);
		g.addChild(c);
		g.measure();
		g.layout(0, 0, 0, 0);

		// a at index 0 (col=0, row=0), c at index 1 (col=1, row=0)
		// cellW = max of visible widths = max(30, 40) = 40
		assertEquals(0, a.layoutX);
		assertEquals(0, a.layoutY);
		assertEquals(40, c.layoutX);
		assertEquals(0, c.layoutY);
	}
}

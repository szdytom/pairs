package app.pairs.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Tests for {@link FlexLayout}: measure, row/column layout, gap, and padding.
 */
class FlexLayoutTest {
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
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		assertArrayEquals(new int[] {0, 0}, l.measure());
	}

	@Test
	void measureSingleChildRow() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		l.addChild(new FixedWidget(30, 20));
		assertArrayEquals(new int[] {30, 20}, l.measure());
	}

	@Test
	void measureSingleChildColumn() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		l.addChild(new FixedWidget(30, 20));
		assertArrayEquals(new int[] {30, 20}, l.measure());
	}

	@Test
	void measureMultipleChildrenRowSumsWidth() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		l.addChild(new FixedWidget(30, 20));
		l.addChild(new FixedWidget(50, 10));
		assertArrayEquals(new int[] {80, 20}, l.measure());
	}

	@Test
	void measureMultipleChildrenColumnSumsHeight() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		l.addChild(new FixedWidget(30, 20));
		l.addChild(new FixedWidget(50, 10));
		assertArrayEquals(new int[] {50, 30}, l.measure());
	}

	@Test
	void measureIncludesGaps() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 4);
		l.addChild(new FixedWidget(30, 20));
		l.addChild(new FixedWidget(50, 10));
		l.addChild(new FixedWidget(10, 30));
		assertArrayEquals(new int[] {30 + 4 + 50 + 4 + 10, 30}, l.measure());
	}

	@Test
	void measureIncludesPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0, 3);
		l.addChild(new FixedWidget(30, 20));
		assertArrayEquals(new int[] {30 + 6, 20 + 6}, l.measure());
	}

	@Test
	void measureIncludesGapAndPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 2, 3);
		l.addChild(new FixedWidget(10, 10));
		l.addChild(new FixedWidget(20, 20));
		assertArrayEquals(new int[] {10 + 2 + 20 + 6, 20 + 6}, l.measure());
	}

	// ---- layout: row ------------------------------------------------------

	@Test
	void layoutRowPositionsChildrenHorizontally() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(0, a.layoutX);
		assertEquals(0, a.layoutY);
		assertEquals(30, b.layoutX);
		assertEquals(0, b.layoutY);
	}

	@Test
	void layoutRowWithGap() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 4);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(0, a.layoutX);
		assertEquals(30 + 4, b.layoutX);
	}

	@Test
	void layoutRowWithPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0, 3);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(3, a.layoutX);
		assertEquals(3, a.layoutY);
		assertEquals(3 + 30, b.layoutX);
		assertEquals(3, b.layoutY);
	}

	@Test
	void layoutRowChildHeightIsContainerHeightMinusPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0, 2);
		FixedWidget a = new FixedWidget(30, 20);
		l.addChild(a);
		l.layout(0, 0, 100, 50);

		assertEquals(2, a.layoutY);
		assertEquals(50 - 4, a.layoutH);
	}

	@Test
	void layoutRowNoChildWidthExpansion() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		FixedWidget a = new FixedWidget(30, 20);
		l.addChild(a);
		l.layout(0, 0, 200, 100);

		assertEquals(30, a.layoutW);
	}

	// ---- layout: column ---------------------------------------------------

	@Test
	void layoutColumnPositionsChildrenVertically() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(0, a.layoutX);
		assertEquals(0, a.layoutY);
		assertEquals(0, b.layoutX);
		assertEquals(20, b.layoutY);
	}

	@Test
	void layoutColumnWithGap() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 4);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(0, a.layoutY);
		assertEquals(20 + 4, b.layoutY);
	}

	@Test
	void layoutColumnWithPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0, 3);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(3, a.layoutX);
		assertEquals(3, a.layoutY);
		assertEquals(3, b.layoutX);
		assertEquals(3 + 20, b.layoutY);
	}

	@Test
	void layoutColumnChildWidthIsContainerWidthMinusPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0, 2);
		FixedWidget a = new FixedWidget(30, 20);
		l.addChild(a);
		l.layout(0, 0, 100, 50);

		assertEquals(2, a.layoutX);
		assertEquals(100 - 4, a.layoutW);
	}

	@Test
	void layoutColumnNoChildHeightExpansion() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		FixedWidget a = new FixedWidget(30, 20);
		l.addChild(a);
		l.layout(0, 0, 200, 100);

		assertEquals(20, a.layoutH);
	}

	// ---- edge cases -------------------------------------------------------

	@Test
	void layoutEmptyNoOp() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		l.layout(0, 0, 100, 50); // should not throw
	}

	@Test
	void zeroGapAndPadding() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0, 0);
		FixedWidget a = new FixedWidget(10, 10);
		FixedWidget b = new FixedWidget(10, 10);
		l.addChild(a);
		l.addChild(b);
		assertArrayEquals(new int[] {20, 10}, l.measure());
		l.layout(0, 0, 100, 50);
		assertEquals(0, a.layoutX);
		assertEquals(10, b.layoutX);
	}

	@Test
	void measureRowMaxHeightAcrossChildren() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		l.addChild(new FixedWidget(10, 5));
		l.addChild(new FixedWidget(10, 15));
		l.addChild(new FixedWidget(10, 10));
		assertArrayEquals(new int[] {30, 15}, l.measure());
	}

	@Test
	void measureColumnMaxWidthAcrossChildren() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		l.addChild(new FixedWidget(5, 10));
		l.addChild(new FixedWidget(15, 10));
		l.addChild(new FixedWidget(10, 10));
		assertArrayEquals(new int[] {15, 30}, l.measure());
	}
}

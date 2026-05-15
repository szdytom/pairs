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

	// ---- flex-grow: row ---------------------------------------------------

	@Test
	void layoutRowFlexGrowExpandsChildToFillRemaining() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		b.setProp("flex-grow", 1);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);

		assertEquals(30, a.layoutW);
		assertEquals(70, b.layoutW); // 50 + (100-30-50)*1/1
		assertEquals(0, a.layoutX);
		assertEquals(30, b.layoutX);
	}

	@Test
	void layoutRowFlexGrowMultipleChildrenShareRemaining() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		FixedWidget a = new FixedWidget(20, 10);
		FixedWidget b = new FixedWidget(30, 10);
		FixedWidget c = new FixedWidget(10, 10);
		b.setProp("flex-grow", 2);
		c.setProp("flex-grow", 1);
		l.addChild(a);
		l.addChild(b);
		l.addChild(c);
		l.layout(0, 0, 90, 50);
		// remaining = 90 - (20+30+10) = 30
		// b gets 30 + 30*2/3 = 50
		// c gets 10 + 30*1/3 = 20

		assertEquals(20, a.layoutW);
		assertEquals(50, b.layoutW);
		assertEquals(20, c.layoutW);
		assertEquals(0, a.layoutX);
		assertEquals(20, b.layoutX);
		assertEquals(70, c.layoutX);
	}

	@Test
	void layoutRowFlexGrowNoShrinkBelowMeasured() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(80, 10);
		b.setProp("flex-grow", 1);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 80, 50);
		// remaining = 80 - (30+80) = -30, clamped to 0
		// b gets 80 + 0 = 80 (measured size preserved)

		assertEquals(30, a.layoutW);
		assertEquals(80, b.layoutW);
	}

	@Test
	void layoutRowNoGrowKeepsMeasuredSize() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 200, 50);
		// no flex-grow — children keep measured sizes

		assertEquals(30, a.layoutW);
		assertEquals(50, b.layoutW);
	}

	// ---- flex-grow: column -------------------------------------------------

	@Test
	void layoutColumnFlexGrowExpandsChildToFillRemaining() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(30, 30);
		b.setProp("flex-grow", 1);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 70);

		assertEquals(20, a.layoutH);
		assertEquals(50, b.layoutH); // 30 + (70-20-30)*1/1
		assertEquals(0, a.layoutY);
		assertEquals(20, b.layoutY);
	}

	@Test
	void layoutColumnFlexGrowNoShrinkBelowMeasured() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(30, 60);
		b.setProp("flex-grow", 1);
		l.addChild(a);
		l.addChild(b);
		l.layout(0, 0, 100, 50);
		// remaining = 50 - (20+60) = -30, clamped to 0

		assertEquals(20, a.layoutH);
		assertEquals(60, b.layoutH); // measured size preserved
	}

	// ---- visibility -------------------------------------------------------

	@Test
	void measureIgnoresInvisibleChildren() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 4);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		b.setVisible(false);
		l.addChild(a);
		l.addChild(b);
		// Only 'a' visible; no gap since only one visible child
		assertArrayEquals(new int[] {30, 20}, l.measure());
	}

	@Test
	void measureGapCountsOnlyVisibleChildren() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 4);
		FixedWidget a = new FixedWidget(10, 5);
		FixedWidget b = new FixedWidget(20, 5);
		FixedWidget c = new FixedWidget(30, 5);
		b.setVisible(false);
		l.addChild(a);
		l.addChild(b);
		l.addChild(c);
		// visible: a, c — one gap between them
		assertArrayEquals(new int[] {10 + 4 + 30, 5}, l.measure());
	}

	@Test
	void layoutRowSkipsInvisibleChildren() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.ROW, 4);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		FixedWidget c = new FixedWidget(20, 15);
		b.setVisible(false);
		l.addChild(a);
		l.addChild(b);
		l.addChild(c);
		l.layout(0, 0, 200, 50);

		assertEquals(0, a.layoutX);
		assertEquals(30 + 4, c.layoutX); // gap only between a and c
	}

	@Test
	void layoutColumnSkipsInvisibleChildren() {
		FlexLayout l = new FlexLayout(FlexLayout.Direction.COLUMN, 4);
		FixedWidget a = new FixedWidget(30, 20);
		FixedWidget b = new FixedWidget(50, 10);
		FixedWidget c = new FixedWidget(20, 15);
		b.setVisible(false);
		l.addChild(a);
		l.addChild(b);
		l.addChild(c);
		l.layout(0, 0, 100, 200);

		assertEquals(0, a.layoutY);
		assertEquals(20 + 4, c.layoutY); // gap only between a and c
	}
}

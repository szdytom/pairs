package app.pairs.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Tests for {@link AlignLayout}: single-child enforcement, measure,
 * and alignment in layout.
 */
class AlignLayoutTest {
	private static final int W = 100;
	private static final int H = 60;

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

	@Test
	void measureReturnsChildSize() {
		AlignLayout layout = new AlignLayout();
		layout.addChild(new FixedWidget(30, 20));

		int[] size = layout.measure();
		assertArrayEquals(new int[] {30, 20}, size);
	}

	@Test
	void measureReturnsZeroWhenEmpty() {
		AlignLayout layout = new AlignLayout();
		int[] size = layout.measure();
		assertArrayEquals(new int[] {0, 0}, size);
	}

	@Test
	void addChildRejectsSecondChild() {
		AlignLayout layout = new AlignLayout();
		layout.addChild(new FixedWidget(10, 10));
		assertThrows(
			IllegalStateException.class,
			() -> layout.addChild(new FixedWidget(10, 10))
		);
	}

	@Test
	void defaultAlignIsLeftTop() {
		AlignLayout layout = new AlignLayout();
		FixedWidget child = new FixedWidget(30, 20);
		layout.addChild(child);
		layout.layout(0, 0, W, H);

		assertEquals(0, child.layoutX);
		assertEquals(0, child.layoutY);
	}

	@Test
	void centerAlign() {
		AlignLayout layout = new AlignLayout();
		layout.setProp("h-align", AlignLayout.HAlign.CENTER);
		layout.setProp("v-align", AlignLayout.VAlign.CENTER);
		FixedWidget child = new FixedWidget(30, 20);
		layout.addChild(child);
		layout.layout(0, 0, W, H);

		assertEquals((W - 30) / 2, child.layoutX);
		assertEquals((H - 20) / 2, child.layoutY);
	}

	@Test
	void rightBottomAlign() {
		AlignLayout layout = new AlignLayout();
		layout.setProp("h-align", AlignLayout.HAlign.RIGHT);
		layout.setProp("v-align", AlignLayout.VAlign.BOTTOM);
		FixedWidget child = new FixedWidget(30, 20);
		layout.addChild(child);
		layout.layout(0, 0, W, H);

		assertEquals(W - 30, child.layoutX);
		assertEquals(H - 20, child.layoutY);
	}

	@Test
	void noOpWhenEmpty() {
		AlignLayout layout = new AlignLayout();
		layout.layout(0, 0, W, H); // should not throw
	}
}

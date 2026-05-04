package app.pairs.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.libsdl4j.api.render.*;

/**
 * Tests for {@link Container}: auto-traversal of update/render/destroy,
 * child visibility control, and basic event dispatch.
 */
class ContainerTest {
	/** A Widget that records which methods were called and handles events. */
	private static class SpyWidget extends Widget {
		boolean updateCalled;
		boolean renderCalled;
		boolean destroyCalled;
		private final boolean handleEvents;

		SpyWidget() {
			this(false);
		}

		SpyWidget(boolean handleEvents) {
			this.handleEvents = handleEvents;
		}

		@Override
		public void update(long deltaTimeMs) {
			updateCalled = true;
		}

		@Override
		public void render(
			SDL_Renderer renderer, int parentX, int parentY, int scale
		) {
			renderCalled = true;
		}

		@Override
		public boolean onEvent(Event event) {
			return handleEvents;
		}

		@Override
		public void destroy() {
			destroyCalled = true;
		}

		@Override
		public int[] measure() {
			return new int[] {10, 10};
		}
	}

	/** A minimal concrete Container for testing. */
	private static class TestContainer extends Container {
		@Override
		public int[] measure() {
			return new int[] {100, 100};
		}

		@Override
		public void layout(int x, int y, int w, int h) {
			this.layoutX = x;
			this.layoutY = y;
			this.layoutW = w;
			this.layoutH = h;
		}
	}

	@Test
	void updatePropagatesToAllVisibleChildren() {
		TestContainer c = new TestContainer();
		SpyWidget child1 = new SpyWidget();
		SpyWidget child2 = new SpyWidget();
		c.addChild(child1);
		c.addChild(child2);

		c.update(16);

		assertTrue(
			child1.updateCalled, "update() should be called on visible child"
		);
		assertTrue(
			child2.updateCalled, "update() should be called on visible child"
		);
	}

	@Test
	void updateSkipsInvisibleChildren() {
		TestContainer c = new TestContainer();
		SpyWidget visible = new SpyWidget();
		SpyWidget invisible = new SpyWidget();
		invisible.setVisible(false);
		c.addChild(visible);
		c.addChild(invisible);

		c.update(16);

		assertTrue(
			visible.updateCalled, "visible child should receive update()"
		);
		assertFalse(
			invisible.updateCalled,
			"invisible child should not receive update()"
		);
	}

	@Test
	void destroyPropagatesToAllChildren() {
		TestContainer c = new TestContainer();
		SpyWidget child1 = new SpyWidget();
		SpyWidget child2 = new SpyWidget();
		c.addChild(child1);
		c.addChild(child2);

		c.destroy();

		assertTrue(child1.destroyCalled);
		assertTrue(child2.destroyCalled);
		assertTrue(
			c.children.isEmpty(),
			"children list should be cleared after destroy"
		);
	}

	@Test
	void addChildSetsParent() {
		TestContainer c = new TestContainer();
		SpyWidget child = new SpyWidget();
		assertNull(child.parent, "parent should be null before addChild");

		c.addChild(child);
		assertSame(c, child.parent, "parent should be set after addChild");
	}

	@Test
	void removeChildClearsParent() {
		TestContainer c = new TestContainer();
		SpyWidget child = new SpyWidget();
		c.addChild(child);
		c.removeChild(child);

		assertNull(child.parent, "parent should be null after removeChild");
		assertFalse(
			c.children.contains(child),
			"children list should not contain removed child"
		);
	}

	@Test
	void addSameChildTwiceIsNoOp() {
		TestContainer c = new TestContainer();
		SpyWidget child = new SpyWidget();
		c.addChild(child);
		c.addChild(child); // duplicate

		assertEquals(
			1, c.children.size(), "duplicate addChild should be a no-op"
		);
	}

	@Test
	void reparentingRemovesFromOldParent() {
		TestContainer c1 = new TestContainer();
		TestContainer c2 = new TestContainer();
		SpyWidget child = new SpyWidget();
		c1.addChild(child);
		c2.addChild(child);

		assertFalse(
			c1.children.contains(child),
			"old parent should lose the child after reparenting"
		);
		assertTrue(
			c2.children.contains(child), "new parent should contain the child"
		);
		assertSame(
			c2, child.parent, "parent pointer should point to new parent"
		);
	}

	@Test
	void dispatchEventHitsVisibleChildUnderCursor() {
		TestContainer c = new TestContainer();
		c.layout(0, 0, 100, 100);

		// A child at (10, 10, 20, 20) in the container's local space.
		SpyWidget child = new SpyWidget(true);
		child.layout(10, 10, 20, 20);
		c.addChild(child);

		// Event at global (15, 15) — should hit the child.
		Event ev = new MouseEvent(Event.Type.MOUSE_PRESSED, 15, 15, 1);
		boolean handled = c.dispatchEvent(ev, 0, 0);

		assertTrue(handled, "event should be handled by child");
	}

	@Test
	void dispatchSkipsInvisibleChild() {
		TestContainer c = new TestContainer();
		c.layout(0, 0, 100, 100);

		SpyWidget child = new SpyWidget();
		child.layout(10, 10, 20, 20);
		child.setVisible(false);
		c.addChild(child);

		// Event at global (15, 15) — should NOT reach invisible child,
		// and should bubble to container (which returns false).
		Event ev = new MouseEvent(Event.Type.MOUSE_PRESSED, 15, 15, 1);
		boolean handled = c.dispatchEvent(ev, 0, 0);

		assertFalse(handled, "invisible child should be skipped");
	}

	@Test
	void firstHitWinsNoPassThroughToSiblings() {
		TestContainer c = new TestContainer();
		c.layout(0, 0, 100, 100);

		// Two overlapping children: front child does NOT handle events,
		// back child DOES. Click hits both bounds.
		SpyWidget front = new SpyWidget(false);
		front.layout(10, 10, 50, 50);
		SpyWidget back = new SpyWidget(true);
		back.layout(10, 10, 50, 50);
		c.addChild(back);  // added first → back in z-order
		c.addChild(front); // added second → front in z-order

		Event ev = new MouseEvent(Event.Type.MOUSE_PRESSED, 30, 30, 1);
		boolean handled = c.dispatchEvent(ev, 0, 0);

		assertFalse(
			handled,
			"front child was hit but didn't handle — should not reach back "
				+ "child"
		);
	}

	@Test
	void dispatchEventMissReturnsFalse() {
		TestContainer c = new TestContainer();
		c.layout(0, 0, 100, 100);

		SpyWidget child = new SpyWidget();
		child.layout(10, 10, 20, 20);
		c.addChild(child);

		// Click outside all children.
		Event ev = new MouseEvent(Event.Type.MOUSE_PRESSED, 99, 99, 1);
		boolean handled = c.dispatchEvent(ev, 0, 0);

		assertFalse(
			handled, "click outside all children should not be handled"
		);
	}
}

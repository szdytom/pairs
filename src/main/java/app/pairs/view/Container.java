package app.pairs.view;

import java.util.ArrayList;
import java.util.List;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Abstract container that manages a list of child {@link Widget}s.
 *
 * <p>
 * Automatically propagates {@link #update(long)},
 * {@link #render(SDL_Renderer, int, int, int)}, and
 * {@link #destroy()} to all children. Dispatches mouse events with
 * hit-testing and bubbling semantics.
 *
 * <p>
 * Subclasses must provide their own {@link #measure()} and
 * {@link #layout(int, int, int, int)} — this class does not define a
 * default layout strategy.
 */
public abstract class Container extends Widget {
	protected final List<Widget> children = new ArrayList<>();

	public void addChild(Widget child) {
		if (child.parent == this) {
			return; // already a child — no-op
		}
		if (child.parent != null) {
			((Container)child.parent).children.remove(child);
		}
		children.add(child);
		child.parent = this;
	}

	public void removeChild(Widget child) {
		if (children.remove(child)) {
			child.parent = null;
		}
	}

	// ---- auto-traversal ----------------------------------------------------

	@Override
	public void update(long deltaTimeMs) {
		for (Widget child : children) {
			if (child.isVisible()) {
				child.update(deltaTimeMs);
			}
		}
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int myGlobalX = parentX + layoutX;
		int myGlobalY = parentY + layoutY;
		for (Widget child : children) {
			if (child.isVisible()) {
				child.render(renderer, myGlobalX, myGlobalY, scale);
			}
		}
	}

	@Override
	public void destroy() {
		for (Widget child : children) {
			child.destroy();
		}
		children.clear();
	}

	// ---- event dispatch ----------------------------------------------------

	/**
	 * Dispatches an event to this container's subtree.
	 *
	 * <p>
	 * For mouse events, hit-tests children front-to-back (reverse
	 * insertion order). The first visible child whose bounds contain the
	 * cursor receives the event. If the child handles it (returns true),
	 * dispatch stops. If the child does not handle it, the event bubbles
	 * to this container's {@link #onEvent(Event)} immediately — siblings
	 * further back are not tried.
	 *
	 * <p>
	 * If no child is hit, the event also reaches this container's
	 * {@link #onEvent(Event)}.
	 *
	 * <p>
	 * For non-mouse events (e.g. keyboard), dispatches directly to
	 * {@link #onEvent(Event)} without hit-testing.
	 */
	@Override
	public boolean dispatchEvent(
		Event event, int parentGlobalX, int parentGlobalY
	) {
		int myGlobalX = parentGlobalX + layoutX;
		int myGlobalY = parentGlobalY + layoutY;

		if (event instanceof MouseEvent me) {
			// Hit-test children front-to-back (topmost first).
			for (int i = children.size() - 1; i >= 0; i--) {
				Widget child = children.get(i);
				if (!child.isVisible()) {
					continue;
				}
				int childGlobalX = myGlobalX + child.layoutX;
				int childGlobalY = myGlobalY + child.layoutY;
				if (me.x() >= childGlobalX
				    && me.x() < childGlobalX + child.layoutW
				    && me.y() >= childGlobalY
				    && me.y() < childGlobalY + child.layoutH) {
					// First hit — dispatch to this child.
					if (child.dispatchEvent(event, myGlobalX, myGlobalY)
					    || event.isConsumed()) {
						return true;
					}
					// Not handled — bubble to self, skip siblings.
					return onEvent(event) || event.isConsumed();
				}
			}
		}
		return onEvent(event) || event.isConsumed();
	}
}

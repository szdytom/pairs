package app.pairs.view;

import java.util.ArrayList;
import java.util.List;

import io.github.libsdl4j.api.render.*;

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
	private Widget prevHovered;

	public void addChild(Widget child) {
		if (child.parent == this) {
			return; // already a child — no-op
		}
		if (child.parent != null) {
			((Container)child.parent).children.remove(child);
		}
		children.add(child);
		child.parent = this;
		if (this.blackboard != null) {
			child.setBlackboard(this.blackboard);
		}
	}

	public void removeChild(Widget child) {
		if (children.remove(child)) {
			child.parent = null;
		}
	}

	public void removeAllChildren() {
		for (Widget child : children) {
			child.destroy();
		}
		children.clear();
	}

	// ---- auto-traversal ----------------------------------------------------

	@Override
	public void update(long deltaTimeMs) {
		for (Widget child : children) {
			if (child.isVisible()) {
				child.update(deltaTimeMs);
			}
		}
		Blackboard bb = blackboard();
		if (bb != null && bb.mouseX >= 0) {
			recheckHover(bb);
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
		removeAllChildren();
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
	 * For keyboard events, broadcasts to all visible children
	 * front-to-back (topmost first). The first child that handles the
	 * event stops dispatch. If no child handles it, it reaches this
	 * container's {@link #onEvent(Event)}.
	 */
	@Override
	public boolean dispatchEvent(
		Event event, int parentGlobalX, int parentGlobalY
	) {
		int myGlobalX = parentGlobalX + layoutX;
		int myGlobalY = parentGlobalY + layoutY;

		if (event instanceof MouseEvent me) {
			hoverTrack(me, myGlobalX, myGlobalY);
			if (me.type() == Event.Type.MOUSE_LEAVE) {
				for (Widget child : children) {
					if (child.isVisible()) {
						child.dispatchEvent(event, myGlobalX, myGlobalY);
					}
				}
				return onEvent(event) || event.isConsumed();
			}
		}

		// Hit-test events with position data front-to-back (topmost first).
		int ex = Integer.MIN_VALUE, ey = Integer.MIN_VALUE;
		if (event instanceof MouseEvent me) {
			ex = me.x();
			ey = me.y();
		} else if (event instanceof ScrollEvent se) {
			ex = se.x();
			ey = se.y();
		}
		if (ex != Integer.MIN_VALUE) {
			for (int i = children.size() - 1; i >= 0; i--) {
				Widget child = children.get(i);
				if (!child.isVisible()) {
					continue;
				}
				int childGlobalX = myGlobalX + child.layoutX;
				int childGlobalY = myGlobalY + child.layoutY;
				if (ex >= childGlobalX && ex < childGlobalX + child.layoutW
				    && ey >= childGlobalY
				    && ey < childGlobalY + child.layoutH) {
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
		if (event instanceof KeyEvent) {
			for (int i = children.size() - 1; i >= 0; i--) {
				Widget child = children.get(i);
				if (!child.isVisible())
					continue;
				if (child.dispatchEvent(event, myGlobalX, myGlobalY)) {
					return true;
				}
			}
		}
		return onEvent(event) || event.isConsumed();
	}

	// ---- hover tracking ---------------------------------------------------

	private void hoverTrack(MouseEvent me, int myGlobalX, int myGlobalY) {
		if (me.type() == Event.Type.MOUSE_LEAVE) {
			setHoveredChild(null);
			return;
		}
		if (me.type() != Event.Type.MOUSE_MOVED) {
			return;
		}
		setHoveredChild(hitTestChild(me.x(), me.y(), myGlobalX, myGlobalY));
	}

	private void setHoveredChild(Widget child) {
		if (child == prevHovered) {
			return;
		}
		if (prevHovered != null) {
			prevHovered.hovered = false;
			prevHovered.onHoverChanged();
		}
		if (child != null) {
			child.hovered = true;
			child.onHoverChanged();
		}
		prevHovered = child;
	}

	private void recheckHover(Blackboard bb) {
		setHoveredChild(
			hitTestChild(bb.mouseX, bb.mouseY, globalX(), globalY())
		);
	}

	private Widget hitTestChild(int mx, int my, int gx, int gy) {
		for (int i = children.size() - 1; i >= 0; i--) {
			Widget child = children.get(i);
			if (!child.isVisible()) {
				continue;
			}
			int childGlobalX = gx + child.layoutX;
			int childGlobalY = gy + child.layoutY;
			if (mx >= childGlobalX && mx < childGlobalX + child.layoutW
			    && my >= childGlobalY && my < childGlobalY + child.layoutH) {
				return child;
			}
		}
		return null;
	}

	private int globalX() {
		int x = layoutX;
		Widget p = parent;
		while (p != null) {
			x += p.layoutX;
			p = p.parent;
		}
		return x;
	}

	private int globalY() {
		int y = layoutY;
		Widget p = parent;
		while (p != null) {
			y += p.layoutY;
			p = p.parent;
		}
		return y;
	}
}

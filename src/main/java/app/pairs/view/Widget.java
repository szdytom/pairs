package app.pairs.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all UI components in the widget tree.
 * Adds parent-child tracking, dynamic properties, layout coordinate storage,
 * and event dispatch to the {@link ViewComponent} lifecycle.
 */
public abstract class Widget implements ViewComponent {
	Widget parent;
	final Map<String, Object> props = new HashMap<>();
	int layoutX;
	int layoutY;
	int layoutW;
	int layoutH;

	@SuppressWarnings("unchecked")
	public <T> T getProp(String name) {
		return (T)props.get(name);
	}

	public void setProp(String name, Object val) {
		props.put(name, val);
	}

	public void setVisible(boolean v) {
		setProp("visible", v);
	}

	public boolean isVisible() {
		Boolean v = getProp("visible");
		return v != null ? v : true;
	}

	/**
	 * Handles an event dispatched to this widget.
	 * Override to react to mouse, keyboard, or other events.
	 *
	 * @param event the event to handle
	 * @return true if the event was handled (stops propagation)
	 */
	public boolean onEvent(Event event) {
		return false;
	}

	/**
	 * Dispatches an event to this widget's subtree.
	 * The event coordinates are in root-relative logical pixels.
	 *
	 * @param event     the event to dispatch
	 * @param myGlobalX this widget's origin X in root-relative logical pixels
	 * @param myGlobalY this widget's origin Y in root-relative logical pixels
	 * @return true if the event was handled by this subtree
	 */
	public boolean dispatchEvent(Event event, int myGlobalX, int myGlobalY) {
		return onEvent(event) || event.isConsumed();
	}

	// ---- ViewComponent defaults --------------------------------------------

	@Override
	public void update(long deltaTimeMs) {
		// Subclasses may override for per-frame state updates.
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		this.layoutX = x;
		this.layoutY = y;
		this.layoutW = w;
		this.layoutH = h;
	}

	@Override
	public void destroy() {
		// Subclasses may override to release native resources.
	}
}

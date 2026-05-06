package app.pairs.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe context store for sharing state across the widget hierarchy.
 * Widgets access the nearest Blackboard by walking up the parent chain via
 * {@link Widget#blackboard()}.
 *
 * <p>
 * Also maintains a layout-dirty flag. When a widget mutates the tree in a way
 * that affects layout (e.g. adding children after the initial layout pass),
 * it sets {@link #layoutDirty} to true. The root component checks this flag
 * before each render and performs a full measure + layout cycle when set,
 * then clears it.
 */
public class Blackboard {
	private final Map<Class<?>, Object> store = new HashMap<>();
	/** Set to true when the widget tree needs a full measure + layout cycle. */
	public boolean layoutDirty;

	public <T> void put(Class<T> type, T value) {
		store.put(type, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type) {
		T value = (T)store.get(type);
		if (value == null) {
			throw new IllegalStateException(
				"No blackboard entry for " + type.getName()
			);
		}
		return value;
	}
}

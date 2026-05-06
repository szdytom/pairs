package app.pairs.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe context store for sharing state across the widget hierarchy.
 * Widgets access the nearest Blackboard by walking up the parent chain via
 * {@link Widget#blackboard()}.
 */
public class Blackboard {
	private final Map<Class<?>, Object> store = new HashMap<>();

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

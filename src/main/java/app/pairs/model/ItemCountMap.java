package app.pairs.model;

import java.util.EnumMap;
import java.util.Map;

public class ItemCountMap {
	private final Map<ItemType, Integer> counts = new EnumMap<>(ItemType.class);

	public ItemCountMap() {
		for (ItemType type : ItemType.values()) {
			counts.put(type, 0);
		}
	}

	public int get(ItemType type) {
		return counts.get(type);
	}

	public void set(ItemType type, int count) {
		counts.put(type, count);
	}

	public void add(ItemType type, int delta) {
		counts.merge(type, delta, Integer::sum);
	}

	public boolean reduce(ItemType type) {
		int current = get(type);
		if (current <= 0) {
			return false;
		}
		counts.put(type, current - 1);
		return true;
	}

	public void reset() {
		for (ItemType type : ItemType.values()) {
			counts.put(type, 0);
		}
	}
}

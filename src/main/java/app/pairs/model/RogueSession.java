package app.pairs.model;

import java.util.EnumMap;
import java.util.Map;

public class RogueSession {
	public static final long TOTAL_TIME_MS = 10 * 60 * 1_000;

	public long remainingMs = TOTAL_TIME_MS;
	public int spendableScore;
	public int cumulativeSpent;
	public int level = 1;
	public final ItemCountMap items = new ItemCountMap();
	public final Map<ItemType, Integer>
		purchaseCounts = new EnumMap<>(ItemType.class);
	public int timePurchases;
	public long totalTimePurchasedMs;

	public RogueSession() {
		for (ItemType type : ItemType.values()) {
			purchaseCounts.put(type, 0);
		}
	}

	public boolean buyItem(ItemType type, int cost) {
		if (spendableScore < cost) {
			return false;
		}
		spendableScore -= cost;
		cumulativeSpent += cost;
		items.add(type, 1);
		purchaseCounts.merge(type, 1, Integer::sum);
		return true;
	}

	public boolean buyTime(int cost) {
		if (spendableScore < cost) {
			return false;
		}
		spendableScore -= cost;
		cumulativeSpent += cost;
		remainingMs += 30_000;
		totalTimePurchasedMs += 30_000;
		timePurchases++;
		return true;
	}

	public int getTotalEarned() {
		return spendableScore + cumulativeSpent;
	}

	public boolean isTimeUp() {
		return remainingMs <= 0;
	}
}

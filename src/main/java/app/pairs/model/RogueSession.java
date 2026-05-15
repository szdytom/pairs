package app.pairs.model;

import java.util.EnumMap;
import java.util.Map;

public class RogueSession {
	public static final long TOTAL_TIME_MS = 10 * 60 * 1_000;

	public static final int AUTO_SOLVER_BASE_COST = 2_000;
	public static final int TNT_BASE_COST = 3_000;
	public static final int TIME_BASE_COST = 5_000;
	public static final double COST_GROWTH_RATE = 1.5;

	public long remainingMs = TOTAL_TIME_MS;
	public int spendableScore;
	public int cumulativeSpent;
	public int level = 1;
	public final ItemCountMap items = new ItemCountMap();
	public final Map<ItemType, Integer>
		purchaseCounts = new EnumMap<>(ItemType.class);
	public int timePurchases;

	public RogueSession() {
		for (ItemType type : ItemType.values()) {
			purchaseCounts.put(type, 0);
		}
	}

	public int getItemCost(ItemType type) {
		int base = switch (type) {
			case AUTO_SOLVER -> AUTO_SOLVER_BASE_COST;
			case TNT -> TNT_BASE_COST;
		};
		int count = purchaseCounts.get(type);
		return (int)(base * Math.pow(COST_GROWTH_RATE, count));
	}

	public int getTimeCost() {
		return (int)(TIME_BASE_COST
		             * Math.pow(COST_GROWTH_RATE, timePurchases));
	}

	public boolean purchaseItem(ItemType type) {
		int cost = getItemCost(type);
		if (spendableScore < cost) {
			return false;
		}
		spendableScore -= cost;
		cumulativeSpent += cost;
		items.add(type, 1);
		purchaseCounts.merge(type, 1, Integer::sum);
		return true;
	}

	public boolean purchaseTime() {
		int cost = getTimeCost();
		if (spendableScore < cost) {
			return false;
		}
		spendableScore -= cost;
		cumulativeSpent += cost;
		remainingMs += 30_000;
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

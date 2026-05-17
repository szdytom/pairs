package app.pairs.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Serialization record for a rogue-mode save. All enum-keyed maps are stored
 * as {@code Map<String, Integer>} (keyed by {@link ItemType#name()}) so Gson
 * can round-trip them without needing to reconstruct {@code EnumMap}.
 *
 * {@code relatedMapId} is the id of the linked non-rogue save that holds the
 * current level state, or {@code null} when the session is between levels
 * (shop state).  {@code gameItems} captures in-level item counts so they can
 * be restored when resuming a mid-level save.
 */
public record RogueSnapshot(
	long remainingMs, int spendableScore, int cumulativeSpent, int level,
	Map<String, Integer> sessionItems, Map<String, Integer> purchaseCounts,
	int timePurchases, long totalTimePurchasedMs,
	Map<String, Integer> gameItems, Long relatedMapId
) {
	/**
	 * Build a snapshot from the live rogue state.
	 *
	 * @param relatedMapId id of the linked map save, or {@code null} for shop
	 * @param gameStatus   current level status, or {@code null} for shop state
	 */
	public static RogueSnapshot from(
		RogueSession session, Long relatedMapId, GameStatus gameStatus
	) {
		Map<String, Integer> sessionItems = new HashMap<>();
		Map<String, Integer> purchaseCounts = new HashMap<>();
		Map<String, Integer> gameItems = new HashMap<>();
		for (ItemType type : ItemType.values()) {
			sessionItems.put(type.name(), session.items.get(type));
			purchaseCounts.put(
				type.name(), session.purchaseCounts.getOrDefault(type, 0)
			);
			if (gameStatus != null) {
				gameItems.put(type.name(), gameStatus.items.get(type));
			}
		}

		return new RogueSnapshot(
			session.remainingMs, session.spendableScore,
			session.cumulativeSpent, session.level, sessionItems,
			purchaseCounts, session.timePurchases, session.totalTimePurchasedMs,
			gameItems, relatedMapId
		);
	}

	/** Reconstruct a {@link RogueSession} from this snapshot. */
	public RogueSession toSession() {
		RogueSession session = new RogueSession();
		session.remainingMs = remainingMs;
		session.spendableScore = spendableScore;
		session.cumulativeSpent = cumulativeSpent;
		session.level = level;
		session.timePurchases = timePurchases;
		session.totalTimePurchasedMs = totalTimePurchasedMs;
		for (ItemType type : ItemType.values()) {
			Integer si = sessionItems.get(type.name());
			if (si != null) {
				session.items.set(type, si);
			}
			Integer pc = purchaseCounts.get(type.name());
			if (pc != null) {
				session.purchaseCounts.put(type, pc);
			}
		}
		return session;
	}
}

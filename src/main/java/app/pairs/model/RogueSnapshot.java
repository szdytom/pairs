package app.pairs.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Serialization record for a rogue-mode save. All enum-keyed maps are stored
 * as {@code Map<String, Integer>} (keyed by {@link ItemType#name()}) so Gson
 * can round-trip them without needing to reconstruct {@code EnumMap}.
 */
public record RogueSnapshot(
	long remainingMs, int spendableScore, int cumulativeSpent, int level,
	Map<String, Integer> sessionItems, Map<String, Integer> purchaseCounts,
	int timePurchases, long totalTimePurchasedMs,
	Map<String, Integer> gameItems, GameSnapshot game
) {
	/** Build a snapshot from the live rogue state. */
	public static RogueSnapshot from(
		RogueSession session, Tilemap tilemap, OpLogs opLogs,
		GameStatus gameStatus
	) {
		Map<String, Integer> sessionItems = new HashMap<>();
		Map<String, Integer> purchaseCounts = new HashMap<>();
		Map<String, Integer> gameItems = new HashMap<>();
		for (ItemType type : ItemType.values()) {
			sessionItems.put(type.name(), session.items.get(type));
			purchaseCounts.put(
				type.name(), session.purchaseCounts.getOrDefault(type, 0)
			);
			gameItems.put(type.name(), gameStatus.items.get(type));
		}

		int[][] grid = new int[tilemap.getHeight()][tilemap.getWidth()];
		for (int r = 0; r < tilemap.getHeight(); r++) {
			for (int c = 0; c < tilemap.getWidth(); c++) {
				grid[r][c] = tilemap.getTile(r, c);
			}
		}
		GameSnapshot game = new GameSnapshot(
			tilemap.getDifficulty(), gameStatus.score, gameStatus.combo, grid,
			opLogs.snapshots()
		);

		return new RogueSnapshot(
			session.remainingMs, session.spendableScore,
			session.cumulativeSpent, session.level, sessionItems,
			purchaseCounts, session.timePurchases, session.totalTimePurchasedMs,
			gameItems, game
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

package app.pairs.map;

import app.pairs.asset.TileRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Picks {@code count} tile types from a {@link TileRegistry} subject to
 * difficulty-style constraints. Two orthogonal knobs:
 *
 * <ul>
 * <li>{@code includeSlabs} — when {@code false}, members of any slab
 * {@link TileGroup} are excluded.</li>
 * <li>{@link Spread} — controls how the picks relate to non-slab groups:
 *   <ul>
 *   <li>{@link Spread#NO_DUPLICATES} — at most one tile per group.</li>
 *   <li>{@link Spread#FREE} — uniformly random among all eligible tiles.</li>
 *   <li>{@link Spread#PREFER_DUPLICATES} — greedily exhaust whole groups
 *   before drawing ungrouped tiles, maximising same-looking tiles in the
 *   final palette.</li>
 *   </ul>
 * </li>
 * </ul>
 *
 * The result is a 1-based array {@code subset[1..count]} of registry numeric
 * IDs, suitable for {@link SubsetTilemapFactory}.
 */
public final class TileSelectionPolicy {
	public enum Spread { NO_DUPLICATES, FREE, PREFER_DUPLICATES }

	private final boolean includeSlabs;
	private final Spread spread;

	public TileSelectionPolicy(boolean includeSlabs, Spread spread) {
		this.includeSlabs = includeSlabs;
		this.spread = spread;
	}

	public static TileSelectionPolicy easy() {
		return new TileSelectionPolicy(false, Spread.NO_DUPLICATES);
	}

	public static TileSelectionPolicy hard() {
		return new TileSelectionPolicy(false, Spread.FREE);
	}

	public static TileSelectionPolicy extreme() {
		return new TileSelectionPolicy(true, Spread.PREFER_DUPLICATES);
	}

	public boolean includeSlabs() {
		return includeSlabs;
	}
	public Spread spread() {
		return spread;
	}

	/**
	 * Convenience overload that pulls string IDs out of a {@link
	 * TileRegistry}.
	 */
	public int[] selectFor(
		TileRegistry registry, TileGroupRegistry groups, int count, Random rnd
	) {
		int n = registry.getTypeCount();
		List<String> ids = new ArrayList<>(n);
		for (int i = 1; i <= n; i++) {
			ids.add(registry.getStringId(i));
		}
		return select(ids, groups, count, rnd);
	}

	/**
	 * @param stringIdsByNumeric index {@code i} holds the string ID of
	 *                           registry numeric ID {@code i + 1}.
	 * @return array of length {@code count + 1}; {@code subset[0]} is unused,
	 *         {@code subset[1..count]} are registry numeric IDs.
	 */
	public int[] select(
		List<String> stringIdsByNumeric, TileGroupRegistry groups, int count,
		Random rnd
	) {
		List<Candidate> pool = buildPool(stringIdsByNumeric, groups);
		List<Integer> picks = switch (spread) {
			case FREE -> pickFree(pool, count, rnd);
			case NO_DUPLICATES -> pickNoDuplicates(pool, count, rnd);
			case PREFER_DUPLICATES -> pickPreferDuplicates(pool, count, rnd);
		};
		if (picks.size() < count) {
			throw new IllegalStateException(
				"Not enough eligible tiles: requested=" + count + ", available="
				+ picks.size() + " (includeSlabs=" + includeSlabs
				+ ", spread=" + spread + ")"
			);
		}
		int[] out = new int[count + 1];
		for (int i = 0; i < count; i++) {
			out[i + 1] = picks.get(i);
		}
		return out;
	}

	private List<Candidate> buildPool(
		List<String> ids, TileGroupRegistry groups
	) {
		List<Candidate> pool = new ArrayList<>();
		for (int i = 0; i < ids.size(); i++) {
			String s = ids.get(i);
			if (s == null) {
				continue;
			}
			TileGroup g = groups.findGroup(s).orElse(null);
			if (!includeSlabs && g != null && g.slab()) {
				continue;
			}
			// Slab group is never used as a "spread group" — a single slab
			// tile in the palette doesn't visually clash with itself.
			TileGroup spreadGroup = (g != null && !g.slab()) ? g : null;
			pool.add(new Candidate(i + 1, spreadGroup));
		}
		return pool;
	}

	private static List<Integer> pickFree(
		List<Candidate> pool, int count, Random rnd
	) {
		List<Candidate> shuffled = new ArrayList<>(pool);
		Collections.shuffle(shuffled, rnd);
		int n = Math.min(count, shuffled.size());
		List<Integer> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			out.add(shuffled.get(i).numericId);
		}
		return out;
	}

	private static List<Integer> pickNoDuplicates(
		List<Candidate> pool, int count, Random rnd
	) {
		// Bucket candidates by spread group; keep ungrouped as singleton
		// buckets. Then take one tile from each bucket (random member).
		Map<TileGroup, List<Candidate>> buckets = new HashMap<>();
		List<Candidate> ungrouped = new ArrayList<>();
		for (Candidate c : pool) {
			if (c.group == null) {
				ungrouped.add(c);
			} else {
				buckets.computeIfAbsent(c.group, k -> new ArrayList<>()).add(c);
			}
		}
		List<Candidate> reps = new ArrayList<>(ungrouped);
		for (List<Candidate> b : buckets.values()) {
			reps.add(b.get(rnd.nextInt(b.size())));
		}
		Collections.shuffle(reps, rnd);
		int n = Math.min(count, reps.size());
		List<Integer> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			out.add(reps.get(i).numericId);
		}
		return out;
	}

	private static List<Integer> pickPreferDuplicates(
		List<Candidate> pool, int count, Random rnd
	) {
		Map<TileGroup, List<Candidate>> buckets = new HashMap<>();
		List<Candidate> ungrouped = new ArrayList<>();
		for (Candidate c : pool) {
			if (c.group == null) {
				ungrouped.add(c);
			} else {
				buckets.computeIfAbsent(c.group, k -> new ArrayList<>()).add(c);
			}
		}
		List<List<Candidate>> bucketList = new ArrayList<>(buckets.values());
		// Larger groups first => more visual confusion early.
		bucketList.sort((a, b) -> Integer.compare(b.size(), a.size()));
		// Among same-size groups, randomise.
		shuffleEqualSizeRuns(bucketList, rnd);
		Collections.shuffle(ungrouped, rnd);

		List<Integer> out = new ArrayList<>(count);
		for (List<Candidate> bucket : bucketList) {
			List<Candidate> shuffled = new ArrayList<>(bucket);
			Collections.shuffle(shuffled, rnd);
			for (Candidate c : shuffled) {
				if (out.size() >= count) {
					return out;
				}
				out.add(c.numericId);
			}
		}
		for (Candidate c : ungrouped) {
			if (out.size() >= count) {
				break;
			}
			out.add(c.numericId);
		}
		return out;
	}

	private static <T> void shuffleEqualSizeRuns(
		List<List<T>> list, Random rnd
	) {
		int i = 0;
		while (i < list.size()) {
			int j = i + 1;
			while (j < list.size()
			       && list.get(j).size() == list.get(i).size()) {
				j++;
			}
			Collections.shuffle(list.subList(i, j), rnd);
			i = j;
		}
	}

	private static final class Candidate {
		final int numericId;
		final TileGroup
			group; // null = ungrouped (or slab, treated as ungrouped)

		Candidate(int numericId, TileGroup group) {
			this.numericId = numericId;
			this.group = group;
		}
	}
}

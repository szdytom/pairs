package app.pairs.map;

import app.pairs.asset.TileRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Difficuty of the game depends on the tile selction policy in customized
 * tilemap factory.
 *
 * Picks {@code count} tile types from a {@link TileRegistry} subject to
 * difficulty-style constraints. Two orthogonal knobs:
 *
 * <ul>
 * <li>{@code includeSlabs} — when {@code false}, members of any slab
 * group are excluded.</li>
 * <li>{@link Spread} — controls how the picks relate to non-slab groups:
 * <ul>
 * <li>{@link Spread#NO_DUPLICATES} — at most one tile per group.</li>
 * <li>{@link Spread#FREE} — uniformly random among all eligible tiles.</li>
 * <li>{@link Spread#PREFER_DUPLICATES} — greedily exhaust whole groups
 * before drawing ungrouped tiles, maximising same-looking tiles in the
 * final palette.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * The result is a 1-based array {@code subset[1..count]} of registry numeric
 * IDs, suitable for {@link SubsetTilemapFactory}.
 */
public final class TileSelectionPolicy {
	public enum Spread { NO_DUPLICATES, FREE, PREFER_DUPLICATES }

	/**
	 * Hard cap on slab tiles in the final palette when {@link
	 * #includeSlabs} is true. Slabs all look broadly similar, so a few of
	 * them go a long way; pulling in the whole slab group would drown out
	 * the rest of the palette.
	 */
	public static final int MAX_SLABS = 3;

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
		List<Integer> slabPicks = pickSlabs(stringIdsByNumeric, groups, rnd);
		int nonSlabCount = Math.max(0, count - slabPicks.size());
		List<Candidate> pool = buildNonSlabPool(stringIdsByNumeric, groups);
		List<Integer> picks = switch (spread) {
			case FREE -> pickFree(pool, nonSlabCount, rnd);
			case NO_DUPLICATES -> pickNoDuplicates(pool, nonSlabCount, rnd);
			case PREFER_DUPLICATES ->
				pickPreferDuplicates(pool, nonSlabCount, rnd);
		};
		List<Integer> all = new ArrayList<>(picks.size() + slabPicks.size());
		all.addAll(picks);
		all.addAll(slabPicks);
		Collections.shuffle(all, rnd);
		if (all.size() < count) {
			throw new IllegalStateException(
				"Not enough eligible tiles: requested=" + count + ", available="
				+ all.size() + " (includeSlabs=" + includeSlabs
				+ ", spread=" + spread + ")"
			);
		}
		int[] out = new int[count + 1];
		for (int i = 0; i < count; i++) {
			out[i + 1] = all.get(i);
		}
		return out;
	}

	/**
	 * Random sample of up to {@link #MAX_SLABS} slab tiles. Returns empty
	 * when slabs are disabled. Slabs are intentionally NOT routed through
	 * the {@link Spread} buckets — three near-identical slabs are already
	 * confusing enough; flooding the palette with sixteen of them drowns
	 * out everything else.
	 */
	private List<Integer> pickSlabs(
		List<String> ids, TileGroupRegistry groups, Random rnd
	) {
		if (!includeSlabs) {
			return List.of();
		}
		List<Integer> slabs = new ArrayList<>();
		for (int i = 0; i < ids.size(); i++) {
			String s = ids.get(i);
			if (s == null) {
				continue;
			}
			if (groups.isSlab(s)) {
				slabs.add(i + 1);
			}
		}
		Collections.shuffle(slabs, rnd);
		return slabs.subList(0, Math.min(MAX_SLABS, slabs.size()));
	}

	/**
	 * Pool of non-slab candidates, each tagged with its (non-slab) group
	 * so the {@link Spread} strategies can bucket them. Slab tiles are
	 * handled separately by {@link #pickSlabs}.
	 */
	private List<Candidate> buildNonSlabPool(
		List<String> ids, TileGroupRegistry groups
	) {
		List<Candidate> pool = new ArrayList<>();
		for (int i = 0; i < ids.size(); i++) {
			String s = ids.get(i);
			if (s == null) {
				continue;
			}
			if (groups.isSlab(s)) {
				continue;
			}
			List<String> g = groups.findGroup(s).orElse(null);
			pool.add(new Candidate(i + 1, g));
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

	private static void partitionByGroup(
		List<Candidate> pool, Map<List<String>, List<Candidate>> buckets,
		List<Candidate> ungrouped
	) {
		for (Candidate c : pool) {
			if (c.group == null) {
				ungrouped.add(c);
			} else {
				buckets.computeIfAbsent(c.group, k -> new ArrayList<>()).add(c);
			}
		}
	}

	private static List<Integer> pickNoDuplicates(
		List<Candidate> pool, int count, Random rnd
	) {
		// Bucket candidates by spread group; keep ungrouped as singleton
		// buckets. Then take one tile from each bucket (random member).
		Map<List<String>, List<Candidate>> buckets = new HashMap<>();
		List<Candidate> ungrouped = new ArrayList<>();
		partitionByGroup(pool, buckets, ungrouped);
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
		Map<List<String>, List<Candidate>> buckets = new HashMap<>();
		List<Candidate> ungrouped = new ArrayList<>();
		partitionByGroup(pool, buckets, ungrouped);
		List<List<Candidate>> bucketList = new ArrayList<>(buckets.values());
		// Larger groups first => more visual confusion early.
		bucketList.sort((a, b) -> Integer.compare(b.size(), a.size()));
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

	private static final class Candidate {
		final int numericId;
		final List<String>
			group; // null = ungrouped (or slab, treated as ungrouped)

		Candidate(int numericId, List<String> group) {
			this.numericId = numericId;
			this.group = group;
		}
	}
}

package app.pairs.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TileSelectionPolicyTest {
	private static final List<TileGroup> GROUPS = List.of(
		new TileGroup(List.of("a1", "a2", "a3"), false),
		new TileGroup(List.of("b1", "b2"), false),
		new TileGroup(List.of("c1", "c2", "c3", "c4"), false),
		new TileGroup(List.of("s1", "s2", "s3"), true)
	);
	private static final TileGroupRegistry REG = new TileGroupRegistry(GROUPS);

	// Numeric IDs 1..12. 1-3 = a, 4-5 = b, 6-9 = c, 10-12 = slabs.
	private static final List<String> IDS = List.of(
		"a1", "a2", "a3", "b1", "b2", "c1", "c2", "c3", "c4", "s1", "s2", "s3"
	);

	private static Random rng() {
		return new Xoroshiro128PP(new Seed(0xCAFEBABEL, 0xDEADBEEFL));
	}

	@Test
	void easyExcludesSlabsAndForbidsDuplicateGroup() {
		int[] subset = TileSelectionPolicy.easy().select(IDS, REG, 3, rng());
		Set<String> picked = pickedStrings(subset);
		assertEquals(3, picked.size());
		// no slabs
		picked.forEach(s -> assertFalse(s.startsWith("s"), s));
		// at most one per group
		assertAtMostOnePerPrefix(picked);
	}

	@Test
	void hardExcludesSlabsButAllowsDuplicateGroup() {
		// Request 8: only a (3) + b (2) + c (4) = 9 non-slab tiles.
		int[] subset = TileSelectionPolicy.hard().select(IDS, REG, 8, rng());
		Set<String> picked = pickedStrings(subset);
		assertEquals(8, picked.size());
		picked.forEach(s -> assertFalse(s.startsWith("s"), s));
	}

	@Test
	void extremeAllowsSlabsAndPrefersSameGroup() {
		// Request 7: pickSlabs takes up to MAX_SLABS=3 from the slab group,
		// the remaining 4 come from PREFER_DUPLICATES which should drain the
		// largest non-slab group (c, 4 members) in one go.
		int[] subset = TileSelectionPolicy.extreme().select(IDS, REG, 7, rng());
		Set<String> picked = pickedStrings(subset);
		assertEquals(7, picked.size());
		long slabCount = picked.stream().filter(s -> s.startsWith("s")).count();
		assertTrue(
			slabCount <= TileSelectionPolicy.MAX_SLABS,
			"slab count " + slabCount + " > MAX_SLABS"
		);
		long cCount = picked.stream().filter(s -> s.startsWith("c")).count();
		assertEquals(4, cCount, "should drain the whole c group: " + picked);
	}

	@Test
	void easyThrowsWhenInsufficient() {
		// 4 distinct non-slab groups, requesting 5 with NO_DUPLICATES is
		// impossible.
		assertThrows(
			IllegalStateException.class,
			() -> TileSelectionPolicy.easy().select(IDS, REG, 5, rng())
		);
	}

	@Test
	void customMatchesPrebuiltVariants() {
		TileSelectionPolicy easy = new TileSelectionPolicy(
			false, TileSelectionPolicy.Spread.NO_DUPLICATES
		);
		assertFalse(easy.includeSlabs());
		assertEquals(TileSelectionPolicy.Spread.NO_DUPLICATES, easy.spread());
	}

	@Test
	void emptyGroupsBehavesAsFreePool() {
		TileGroupRegistry empty = new TileGroupRegistry(List.of());
		List<String> ids = List.of("x", "y", "z");
		int[] subset = TileSelectionPolicy.easy().select(ids, empty, 3, rng());
		assertEquals(3, pickedStrings(subset, ids).size());
	}

	private static Set<String> pickedStrings(int[] subset) {
		return pickedStrings(subset, IDS);
	}

	private static Set<String> pickedStrings(int[] subset, List<String> ids) {
		Set<String> out = new HashSet<>();
		for (int i = 1; i < subset.length; i++) {
			out.add(ids.get(subset[i] - 1));
		}
		return out;
	}

	private static void assertAtMostOnePerPrefix(Set<String> picked) {
		Set<Character> seen = new HashSet<>();
		List<Character> prefixes = new ArrayList<>();
		for (String s : picked) {
			prefixes.add(s.charAt(0));
		}
		for (Character c : prefixes) {
			assertTrue(seen.add(c), "duplicate group prefix " + c);
		}
	}
}

package app.pairs.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catalog of tile groups used by {@link TileSelectionPolicy} to enforce
 * difficulty-specific constraints (e.g. "no slabs", "at most one per group",
 * "prefer same-group"). Instances are loaded from
 * {@code assets/tile-groups.json} via {@code TileGroupsOperation} and looked
 * up through {@code AssetManager} under id {@code tile-groups/default}.
 */
public final class TileGroupRegistry {
	private final Map<String, List<String>> byMember;
	private final Set<String> slabMembers;

	public TileGroupRegistry(
		List<List<String>> nonSlabGroups, List<List<String>> slabGroups
	) {
		Map<String, List<String>> map = new HashMap<>();
		Set<String> slabs = new HashSet<>();
		for (List<String> g : nonSlabGroups) {
			for (String m : g) {
				map.put(m, g);
			}
		}
		for (List<String> g : slabGroups) {
			for (String m : g) {
				map.put(m, g);
				slabs.add(m);
			}
		}
		this.byMember = Map.copyOf(map);
		this.slabMembers = Set.copyOf(slabs);
	}

	/** Group members containing the given string ID, if any. */
	public Optional<List<String>> findGroup(String stringId) {
		return Optional.ofNullable(byMember.get(stringId));
	}

	/** True iff the tile belongs to a slab group. */
	public boolean isSlab(String stringId) {
		return slabMembers.contains(stringId);
	}
}

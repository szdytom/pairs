package app.pairs.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalog of {@link TileGroup}s used by {@link TileSelectionPolicy} to enforce
 * difficulty-specific constraints (e.g. "no slabs", "at most one per group",
 * "prefer same-group"). Instances are loaded from
 * {@code assets/tile-groups.json} via {@code TileGroupsOperation} and looked
 * up through {@code AssetManager} under id {@code tile-groups/default}.
 */
public final class TileGroupRegistry {
	private final List<TileGroup> groups;
	private final Map<String, TileGroup> byMember;

	public TileGroupRegistry(List<TileGroup> groups) {
		this.groups = List.copyOf(groups);
		Map<String, TileGroup> map = new HashMap<>();
		for (TileGroup g : this.groups) {
			for (String m : g.members()) {
				map.put(m, g);
			}
		}
		this.byMember = Map.copyOf(map);
	}

	public List<TileGroup> groups() {
		return groups;
	}

	/** Group containing the given string ID, if any. */
	public Optional<TileGroup> findGroup(String stringId) {
		return Optional.ofNullable(byMember.get(stringId));
	}

	/** True iff the tile belongs to the slab group. */
	public boolean isSlab(String stringId) {
		TileGroup g = byMember.get(stringId);
		return g != null && g.slab();
	}
}

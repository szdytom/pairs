package app.pairs.map;

import java.util.List;

/**
 * A group of visually similar tiles. The {@code slab} flag identifies the
 * special "slab" group: easy/hard difficulties exclude slab tiles entirely,
 * while extreme mode allows them.
 */
public record TileGroup(List<String> members, boolean slab) {
	public TileGroup {
		members = List.copyOf(members);
	}

	public boolean contains(String stringId) {
		return members.contains(stringId);
	}
}

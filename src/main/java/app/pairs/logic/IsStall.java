package app.pairs.logic;

import app.pairs.model.Tilemap;

import java.util.List;

public class IsStall {
	public static boolean isStall(Tilemap map) {
		for (var entry : map.tilesByIdMap().entrySet()) {
			List<Tilemap.TilePos> positions = entry.getValue();
			for (int i = 0; i < positions.size(); i++) {
				for (int j = i + 1; j < positions.size(); j++) {
					if (TileTransition.transition(
							map, positions.get(i).row(), positions.get(i).col(),
							positions.get(j).row(), positions.get(j).col()
						)) {
						return false;
					}
				}
			}
		}
		return true;
	}
}

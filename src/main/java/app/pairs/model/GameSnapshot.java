package app.pairs.model;

import app.pairs.map.TilemapFactorySnapshot;

import java.util.List;

public record GameSnapshot(
	TilemapSnapshot tilemap, TilemapFactorySnapshot factory,
	GameStatusSnapshot status, List<OperationSnapshot> operations
) {
	public Tilemap.Difficulty difficulty() {
		return tilemap.difficulty();
	}
}

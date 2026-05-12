package app.pairs.model;

import java.util.List;

public record GameSnapshot(
	Tilemap.Difficulty difficulty, int score, int combo, int[][] tilemap,
	List<OperationSnapshot> operations
) {}

package app.pairs.model;

import java.util.List;

public record GameSnapshot(
	GameType difficulty, int score, int combo, int[][] tilemap,
	List<OperationSnapshot> operations
) {}

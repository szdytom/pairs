package app.pairs.model;

import java.util.List;

public record OperationSnapshot(
	int row1, int col1, int row2, int col2, int tileId, int time,
	int deltaScore, List<Integer> path, int comboBefore
) {}

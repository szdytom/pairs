package app.pairs.solver;

import java.util.List;

/**
 * Result of a solver run.
 *
 * <p>{@code moves} is the best (lowest remainingPairs) elimination sequence
 * found within the time budget. {@code remainingPairs} is the number of
 * unsolvable pairs left after applying all moves; 0 means a complete clear.
 */
public record SolverResult(List<Move> moves, int remainingPairs) {
	public boolean isComplete() {
		return remainingPairs == 0;
	}
}

package app.pairs.solver;

import app.pairs.logic.TileTransition;
import app.pairs.model.Tilemap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 *
 * Solves (or partially solves) a {@link Tilemap} by enumerating elimination
 * sequences. Goal: minimize the number of tiles left after all valid moves
 * are exhausted; reaching 0 means a complete clear.
 *
 *
 * <p>
 * Strategy: DFS with Zobrist hashing + transposition table, forced-move
 * propagation for size-2 types, most-constrained-first branching, and an
 * optional anytime time budget.
 *
 * <p>
 * The input map is not mutated: the solver works on an internal copy.
 */
public final class Solver {
	private final int height;
	private final int width;
	private final int[][] grid;
	private final Tilemap view;
	private final long[] zKey;
	private final Map<Long, Integer> tt = new HashMap<>(1 << 14);

	private long hash;
	private int remainingTiles;

	private long deadlineNanos;
	private boolean deadlineHit;

	private int bestRemainingTiles;
	private final ArrayDeque<Move> currentPath = new ArrayDeque<>();
	private List<Move> bestPath = List.of();

	private Solver(Tilemap source) {
		this.height = source.getHeight();
		this.width = source.getWidth();
		this.grid = new int[height][width];
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				grid[r][c] = source.getTile(r, c);
			}
		}
		this.view = new Tilemap(grid);
		this.zKey = ZKey.Generate(height, width);
		long h = 0;
		int count = 0;
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				if (grid[r][c] > 0) {
					h ^= zKey[r * width + c];
					count++;
				}
			}
		}
		this.hash = h;
		this.remainingTiles = count;
	}

	/** Solve with no time limit (runs until exact optimum is proven). */
	public static SolverResult solve(Tilemap map) {
		return solve(map, 0);
	}

	/**
	 * Solve with an optional time budget. {@code timeoutMillis <= 0} means no
	 * limit. On timeout the best result found so far is returned (anytime).
	 */
	public static SolverResult solve(Tilemap map, long timeoutMillis) {
		Solver s = new Solver(map);
		s.deadlineNanos = timeoutMillis > 0
			? System.nanoTime() + timeoutMillis * 1_000_000L
			: Long.MAX_VALUE;
		s.bestRemainingTiles = s.remainingTiles;
		s.dfs();
		return new SolverResult(s.bestPath, s.bestRemainingTiles / 2);
	}

	private int dfs() {
		if (remainingTiles == 0) {
			snapshotBest();
			return 0;
		}
		if (System.nanoTime() > deadlineNanos) {
			deadlineHit = true;
			return remainingTiles;
		}
		int forcedDepth = applyForcedMoves();
		try {
			if (remainingTiles == 0) {
				snapshotBest();
				return 0;
			}
			Integer cached = tt.get(hash);
			if (cached != null) {
				return cached;
			}
			List<Move> moves = generateMoves();
			if (moves.isEmpty()) {
				snapshotBest();
				return remainingTiles;
			}

			int localBest = remainingTiles;
			for (Move m : moves) {
				int val = grid[m.r1()][m.c1()];
				apply(m);
				int sub = dfs();
				undo(m, val);
				if (sub < localBest) {
					localBest = sub;
				}
				if (localBest == 0 || deadlineHit) {
					break;
				}
			}
			if (!deadlineHit) {
				tt.put(hash, localBest);
			}
			return localBest;
		} finally {
			unwindForcedMoves(forcedDepth);
		}
	}

	private int applyForcedMoves() {
		int depth = 0;
		while (true) {
			Move f = findForcedMove();
			if (f == null) {
				return depth;
			}
			int val = grid[f.r1()][f.c1()];
			apply(f);
			forcedStack.push(new int[] {f.r1(), f.c1(), f.r2(), f.c2(), val});
			depth++;
		}
	}

	private final ArrayDeque<int[]> forcedStack = new ArrayDeque<>();

	private void unwindForcedMoves(int depth) {
		for (int i = 0; i < depth; i++) {
			int[] f = forcedStack.pop();
			Move m = new Move(f[0], f[1], f[2], f[3]);
			undo(m, f[4]);
		}
	}

	private Move findForcedMove() {
		Map<Integer, int[]> firstSeen = new HashMap<>();
		Map<Integer, int[]> secondSeen = new HashMap<>();
		Set<Integer> overTwo = new HashSet<>();
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int v = grid[r][c];
				if (v <= 0 || overTwo.contains(v)) {
					continue;
				}
				if (!firstSeen.containsKey(v)) {
					firstSeen.put(v, new int[] {r, c});
				} else if (!secondSeen.containsKey(v)) {
					secondSeen.put(v, new int[] {r, c});
				} else {
					overTwo.add(v);
					firstSeen.remove(v);
					secondSeen.remove(v);
				}
			}
		}
		for (var e : secondSeen.entrySet()) {
			int[] a = firstSeen.get(e.getKey());
			int[] b = e.getValue();
			if (TileTransition.transition(view, a[0], a[1], b[0], b[1])) {
				return new Move(a[0], a[1], b[0], b[1]);
			}
		}
		return null;
	}

	private List<Move> generateMoves() {
		Map<Integer, List<int[]>> byType = new HashMap<>();
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int v = grid[r][c];
				if (v > 0) {
					byType.computeIfAbsent(v, k -> new ArrayList<>())
						.add(new int[] {r, c});
				}
			}
		}
		List<List<int[]>> groups = new ArrayList<>(byType.values());
		groups.sort(Comparator.comparingInt(List::size));

		List<Move> moves = new ArrayList<>();
		for (List<int[]> g : groups) {
			for (int i = 0; i < g.size(); i++) {
				int[] a = g.get(i);
				for (int j = i + 1; j < g.size(); j++) {
					int[] b = g.get(j);
					if (TileTransition.transition(
							view, a[0], a[1], b[0], b[1]
						)) {
						moves.add(new Move(a[0], a[1], b[0], b[1]));
					}
				}
			}
		}
		return moves;
	}

	private void apply(Move m) {
		grid[m.r1()][m.c1()] = 0;
		grid[m.r2()][m.c2()] = 0;
		hash ^= zKey[m.r1() * width + m.c1()] ^ zKey[m.r2() * width + m.c2()];
		remainingTiles -= 2;
		currentPath.push(m);
	}

	private void undo(Move m, int val) {
		currentPath.pop();
		grid[m.r1()][m.c1()] = val;
		grid[m.r2()][m.c2()] = val;
		hash ^= zKey[m.r1() * width + m.c1()] ^ zKey[m.r2() * width + m.c2()];
		remainingTiles += 2;
	}

	private void snapshotBest() {
		if (remainingTiles < bestRemainingTiles) {
			bestRemainingTiles = remainingTiles;
			List<Move> snap = new ArrayList<>(currentPath.size());
			Iterator<Move> it = currentPath.descendingIterator();
			while (it.hasNext()) {
				snap.add(it.next());
			}
			bestPath = snap;
		}
	}
}

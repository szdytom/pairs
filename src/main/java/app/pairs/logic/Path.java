package app.pairs.logic;

import app.pairs.model.Tilemap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * Lianliankan path search.
 *
 * <p>
 * Finds a legal route from one tile to another that
 * <ul>
 * <li>moves only horizontally or vertically,</li>
 * <li>passes only through empty cells (or the outside ring), and</li>
 * <li>uses the minimum possible number of turns.</li>
 * </ul>
 *
 * <p>
 * The returned path is a flat list of corner coordinates
 * <code>[row0, col0, row1, col1, …, rowN, colN]</code>: the start, every
 * turning point, and the end. Coordinates use the project's
 * {@code (row, col)} convention. The implicit "outside ring" of the board
 * (rows {@code -1} / {@code height} and columns {@code -1} / {@code width})
 * is treated as always passable, matching {@link TileTransition}.
 */
public final class Path {
	private static final int[] DR = {-1, 1, 0, 0};
	private static final int[] DC = {0, 0, -1, 1};

	private Path() {}

	/**
	 * Finds a min-turns path between two tiles.
	 *
	 * @return a flat list {@code [r,c,r,c,…]} of corner points, or an empty
	 *         list if no legal path exists. If start equals target, returns
	 *         {@code [r, c]}.
	 */
	public static ArrayList<Integer> path(
		Tilemap map, int startRow, int startCol, int targetRow, int targetCol
	) {
		if (startRow == targetRow && startCol == targetCol) {
			ArrayList<Integer> single = new ArrayList<>(2);
			single.add(startRow);
			single.add(startCol);
			return single;
		}

		// Expanded coordinate space: rows -1..height, cols -1..width.
		// Index in arrays = (r+1, c+1).
		final int H = map.getHeight();
		final int W = map.getWidth();
		final int eh = H + 2;
		final int ew = W + 2;

		// Min number of straight segments used to reach each cell.
		// `segments - 1` == number of turns.
		int[][] segs = new int[eh][ew];
		// Encoded parent: (r+1) * ew + (c+1), or -1 if none.
		int[][] parent = new int[eh][ew];
		for (int i = 0; i < eh; i++) {
			for (int j = 0; j < ew; j++) {
				segs[i][j] = Integer.MAX_VALUE;
				parent[i][j] = -1;
			}
		}

		segs[startRow + 1][startCol + 1] = 0;
		Deque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[] {startRow, startCol});

		while (!queue.isEmpty()) {
			int[] cur = queue.poll();
			int r = cur[0];
			int c = cur[1];
			if (r == targetRow && c == targetCol) {
				break;
			}
			int curSeg = segs[r + 1][c + 1];
			int nextSeg = curSeg + 1;
			int parentEnc = (r + 1) * ew + (c + 1);

			// Walk a straight ray in each direction until we leave the
			// passable region. Each cell along the ray is reachable in
			// `nextSeg` segments, with `(r,c)` as its parent (corner).
			for (int d = 0; d < 4; d++) {
				int nr = r + DR[d];
				int nc = c + DC[d];
				while (isPassable(map, nr, nc, targetRow, targetCol)) {
					if (nextSeg < segs[nr + 1][nc + 1]) {
						segs[nr + 1][nc + 1] = nextSeg;
						parent[nr + 1][nc + 1] = parentEnc;
						queue.add(new int[] {nr, nc});
					}
					// Don't walk past the target.
					if (nr == targetRow && nc == targetCol) {
						break;
					}
					nr += DR[d];
					nc += DC[d];
				}
			}
		}

		if (segs[targetRow + 1][targetCol + 1] == Integer.MAX_VALUE) {
			return new ArrayList<>();
		}

		// Walk parent pointers back to start, then reverse.
		ArrayList<Integer> reversed = new ArrayList<>();
		int rr = targetRow;
		int cc = targetCol;
		while (true) {
			reversed.add(rr);
			reversed.add(cc);
			int p = parent[rr + 1][cc + 1];
			if (p < 0) {
				break;
			}
			rr = p / ew - 1;
			cc = p % ew - 1;
		}

		ArrayList<Integer> result = new ArrayList<>(reversed.size());
		for (int i = reversed.size() - 2; i >= 0; i -= 2) {
			result.add(reversed.get(i));
			result.add(reversed.get(i + 1));
		}
		return result;
	}

	/**
	 * `(r, c)` is passable as an intermediate cell iff it's on the outside
	 * ring or contains no tile. The target cell is always considered
	 * passable so the BFS can reach it.
	 */
	private static boolean isPassable(
		Tilemap map, int r, int c, int targetRow, int targetCol
	) {
		int H = map.getHeight();
		int W = map.getWidth();
		if (r < -1 || r > H || c < -1 || c > W) {
			return false;
		}
		if (r == -1 || r == H || c == -1 || c == W) {
			return true;
		}
		if (r == targetRow && c == targetCol) {
			return true;
		}
		return map.getTile(r, c) <= 0;
	}
}

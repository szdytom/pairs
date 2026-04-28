package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class PathTest {
	private static Tilemap mapOf(int[][] grid) {
		// Defensive copy: keep tests independent from any mutation.
		int[][] copy = new int[grid.length][];
		for (int i = 0; i < grid.length; i++) {
			copy[i] = grid[i].clone();
		}
		return new Tilemap(copy);
	}

	private static int turns(ArrayList<Integer> path) {
		// path = [r,c,r,c,...]; corners count = path.size() / 2.
		// turns = corners - 2 (start and end are not turns), clamped at 0.
		int corners = path.size() / 2;
		return Math.max(0, corners - 2);
	}

	@Test
	void straightLineHasZeroTurns() {
		// Same row, only empties between.
		int[][] grid = {{1, 0, 0, 1}};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 0, 3);
		assertThat(p).containsExactly(0, 0, 0, 3);
		assertThat(turns(p)).isZero();
	}

	@Test
	void oneTurnLPath() {
		// Tile at (0,0) and (2,2). Empty L route via (0,2) or (2,0).
		int[][] grid = {
			{1, 0, 0},
			{0, 0, 0},
			{0, 0, 1},
		};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 2, 2);
		assertThat(p.get(0)).isEqualTo(0);
		assertThat(p.get(1)).isEqualTo(0);
		assertThat(p.get(p.size() - 2)).isEqualTo(2);
		assertThat(p.get(p.size() - 1)).isEqualTo(2);
		assertThat(turns(p)).isEqualTo(1);
	}

	@Test
	void twoTurnZPath() {
		// 3×4 board:
		// 1 . 9 2
		// . 9 9 .
		// 9 9 . .
		// (1) at (0,0), (2) at (0,3); a wall blocks the direct row.
		// Routes must use rows 0 → outside ring or columns; min turns is 2.
		int[][] grid = {
			{1, 0, 9, 2},
			{0, 9, 9, 0},
			{9, 9, 0, 0},
		};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 0, 3);
		assertThat(p).isNotEmpty();
		assertThat(p.get(0)).isEqualTo(0);
		assertThat(p.get(1)).isEqualTo(0);
		assertThat(p.get(p.size() - 2)).isEqualTo(0);
		assertThat(p.get(p.size() - 1)).isEqualTo(3);
		assertThat(turns(p)).isEqualTo(2);
	}

	@Test
	void routeViaOutsideRing() {
		// Both tiles on the top row, but the row between them is filled.
		// The only legal route exits through row -1 (outside ring).
		int[][] grid = {
			{1, 9, 9, 9, 2},
			{9, 9, 9, 9, 9},
		};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 0, 4);
		assertThat(p).isNotEmpty();
		assertThat(turns(p)).isEqualTo(2);
	}

	@Test
	void noPathReturnsEmpty() {
		// Target completely walled in by other tiles, no way around.
		int[][] grid = {
			{1, 0, 9, 9, 9},
			{0, 0, 9, 2, 9},
			{0, 0, 9, 9, 9},
		};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 1, 3);
		assertThat(p).isEmpty();
	}

	@Test
	void prefersFewerTurnsWhenChoiceExists() {
		// Two tiles on same row with a clear straight path; ensure BFS
		// picks 0 turns over a 2-turn detour.
		int[][] grid = {
			{1, 0, 0, 0, 2},
			{0, 0, 0, 0, 0},
		};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 0, 4);
		assertThat(turns(p)).isZero();
		assertThat(p).containsExactly(0, 0, 0, 4);
	}

	@Test
	void startEqualsTargetReturnsSinglePoint() {
		int[][] grid = {{1, 0}, {0, 0}};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 0, 0);
		assertThat(p).containsExactly(0, 0);
	}

	@Test
	void cornersAreActualTurningPoints() {
		// Force a single L-turn through the bottom-right empty corner.
		int[][] grid = {
			{1, 9, 0},
			{0, 9, 0},
			{0, 0, 2},
		};
		ArrayList<Integer> p = Path.path(mapOf(grid), 0, 0, 2, 2);
		assertThat(p).isNotEmpty();
		// Each consecutive pair must share a row or a column (axis-aligned).
		for (int i = 0; i + 3 < p.size(); i += 2) {
			int r1 = p.get(i);
			int c1 = p.get(i + 1);
			int r2 = p.get(i + 2);
			int c2 = p.get(i + 3);
			assertThat(r1 == r2 || c1 == c2).isTrue();
		}
	}
}

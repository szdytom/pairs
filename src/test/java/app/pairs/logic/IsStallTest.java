package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;

import org.junit.jupiter.api.Test;

class IsStallTest {
	@Test
	void emptyBoardIsStall() {
		// No tiles at all — nothing to eliminate.
		Tilemap tm = new Tilemap(new int[][] {{0, 0}, {0, 0}});
		assertThat(IsStall.isStall(tm)).isTrue();
	}

	@Test
	void noMatchingPairsIsStall() {
		// Every tile has a unique id — no same-id pair can ever be formed.
		Tilemap tm = new Tilemap(new int[][] {{1, 2, 3}, {4, 5, 6}});
		assertThat(IsStall.isStall(tm)).isTrue();
	}

	@Test
	void sameTileBlockedOnAllSidesIsStall() {
		// Two 1s are locked inside unique-id walls with no reachable clear
		// row or column between them.
		//
		// 2 3 4 5 6
		// 7 1 8 1 9
		// 10 11 12 13 14
		//
		// The only shared row (row 1) is blocked by 8.
		// The horizontal clear stretch of each 1 is a single cell, so they
		// share no reachable column either.
		// All wall tile ids are distinct so no wall pair can eliminate.
		int[][] map = {
			{2, 3, 4, 5, 6},
			{7, 1, 8, 1, 9},
			{10, 11, 12, 13, 14},
		};
		assertThat(IsStall.isStall(new Tilemap(map))).isTrue();
	}

	@Test
	void adjacentPairNotStall() {
		// Directly adjacent 1s always have a clear path.
		Tilemap tm = new Tilemap(new int[][] {{0, 1, 1, 0}});
		assertThat(IsStall.isStall(tm)).isFalse();
	}

	@Test
	void sameRowWithGapNotStall() {
		// 1s separated by an empty cell; they can route through an outside
		// row (above or below the single-row grid).
		Tilemap tm = new Tilemap(new int[][] {{1, 0, 1}});
		assertThat(IsStall.isStall(tm)).isFalse();
	}

	@Test
	void lShapePathNotStall() {
		// 1s in opposite corners of a clear 3x3 board — one-turn L path.
		int[][] map = {
			{1, 0, 0},
			{0, 0, 0},
			{0, 0, 1},
		};
		assertThat(IsStall.isStall(new Tilemap(map))).isFalse();
	}

	@Test
	void uShapeOutsidePathNotStall() {
		// 1s blocked in their shared row/column by a centre tile, but they
		// can meet via a two-turn U-path going outside the grid boundary.
		int[][] map = {
			{0, 1, 0},
			{0, 5, 0},
			{0, 1, 0},
		};
		assertThat(IsStall.isStall(new Tilemap(map))).isFalse();
	}

	@Test
	void oneOpenPairAmongOthersNotStall() {
		// 1s are adjacent (always eliminable); 2s are each unique (blocked).
		// A single open pair is enough to make the board not stalled.
		int[][] map = {
			{2, 3, 4},
			{0, 1, 1},
		};
		assertThat(IsStall.isStall(new Tilemap(map))).isFalse();
	}
}

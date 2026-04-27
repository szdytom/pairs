package app.pairs.logic;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;

import org.junit.jupiter.api.Test;

class TileTransitionTest {
	@Test
	void sameRowDirectClear() {
		int[][] map = {
			{0, 0, 0, 0, 0},
			{1, 0, 0, 0, 1},
			{0, 0, 0, 0, 0},
		};
		assertThat(TileTransition.transition(new Tilemap(map), 1, 0, 1, 4))
			.isTrue();
	}

	@Test
	void sameRowBlockedButFreeAdjacentRow() {
		int[][] map = {
			{0, 0, 0},
			{1, 5, 1},
			{0, 0, 0},
		};
		assertThat(TileTransition.transition(new Tilemap(map), 1, 0, 1, 2))
			.isTrue();
	}

	@Test
	void sameColumnDirectClear() {
		int[][] map = {
			{0, 1, 0},
			{0, 0, 0},
			{0, 1, 0},
		};
		assertThat(TileTransition.transition(new Tilemap(map), 0, 1, 2, 1))
			.isTrue();
	}

	@Test
	void sameColumnBlockedButFreeAdjacentColumn() {
		int[][] map = {
			{0, 1, 0},
			{0, 9, 0},
			{0, 1, 0},
		};
		// Direct column is blocked by (1,1)=9, but detour via column 0 or 2
		// connects within 2 turns
		assertThat(TileTransition.transition(new Tilemap(map), 0, 1, 2, 1))
			.isTrue();
	}

	@Test
	void adjacentSameRow() {
		int[][] map = {
			{1, 1, 0},
		};
		assertThat(TileTransition.transition(new Tilemap(map), 0, 0, 0, 1))
			.isTrue();
	}

	@Test
	void adjacentSameColumn() {
		int[][] map = {
			{1},
			{1},
			{0},
		};
		assertThat(TileTransition.transition(new Tilemap(map), 0, 0, 1, 0))
			.isTrue();
	}

	@Test
	void lShapeOneTurn() {
		int[][] map = {
			{1, 0, 0},
			{0, 0, 0},
			{0, 0, 1},
		};
		assertThat(TileTransition.transition(new Tilemap(map), 0, 0, 2, 2))
			.isTrue();
	}

	@Test
	void uShapeOutsideMeeting() {
		int[][] map = {
			{0, 0, 0, 0, 0}, {0, 1, 0, 0, 0}, {0, 0, 5, 0, 0},
			{0, 0, 0, 1, 0}, {0, 0, 0, 0, 0},
		};
		// (1,1) and (3,3): center (2,2) is blocked by 5, but can route via
		// row=0/4 or col=0/4
		assertThat(TileTransition.transition(new Tilemap(map), 1, 1, 3, 3))
			.isTrue();
	}

	@Test
	void noPathDiagonalSurrounded() {
		int[][] map = {
			{1, 9, 0, 0},
			{9, 0, 0, 0},
			{0, 0, 0, 9},
			{0, 0, 9, 1},
		};
		// Both 1s are immediately surrounded by 9s on all reachable sides, no
		// path exists
		assertThat(TileTransition.transition(new Tilemap(map), 0, 0, 3, 3))
			.isFalse();
	}

	@Test
	void noPathFullyEnclosed() {
		int[][] map = {
			{9, 9, 9, 9, 9},
			{9, 1, 9, 1, 9},
			{9, 9, 9, 9, 9},
		};
		// Both 1s are enclosed by internal 9 walls with no passable route
		// between them
		assertThat(TileTransition.transition(new Tilemap(map), 1, 1, 1, 3))
			.isFalse();
	}
}

package app.pairs.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IsometricMapperTest {
	private static final int TILE_WIDTH = 16;
	private static final int TILE_HEIGHT = 16;
	private static final int ORIGIN_X = 512;
	private static final int ORIGIN_Y = 80;
	private static final int SCALE = 6;

	private final IsometricMapper mapper = new IsometricMapper(
		TILE_WIDTH, TILE_HEIGHT, ORIGIN_X, ORIGIN_Y
	);

	@Test
	void screenToGridRoundTripsCenter() {
		int[][] points = {{0, 0}, {5, 5}, {0, 9}, {9, 0}, {3, 7}};
		for (int[] p : points) {
			int row = p[0], col = p[1];
			IsometricMapper.IsometricCoordinate screen = mapper.gridToScreen(
				row, col, SCALE
			);
			int[] grid = mapper.screenToGrid(screen.x, screen.y, SCALE);
			assertThat(grid[0]).isEqualTo(row);
			assertThat(grid[1]).isEqualTo(col);
		}
	}

	@Test
	void containsReturnsTrueForCenter() {
		int[][] points = {{0, 0}, {5, 5}, {9, 9}};
		for (int[] p : points) {
			int row = p[0], col = p[1];
			IsometricMapper.IsometricCoordinate screen = mapper.gridToScreen(
				row, col, SCALE
			);
			assertThat(mapper.contains(screen.x, screen.y, row, col, SCALE))
				.isTrue();
		}
	}

	@Test
	void containsReturnsFalseForFarPoint() {
		assertThat(mapper.contains(0, 0, 5, 5, SCALE)).isFalse();
	}

	@Test
	void screenToGridWorksWithNegativeFractionalRegion() {
		// Points on the upper-left side of the grid where col < row
		// produce negative colMinusRow, exercising the rounding fix.
		IsometricMapper.IsometricCoordinate screen = mapper.gridToScreen(
			7, 2, SCALE
		);
		// The diamond footprint extends halfStep in each direction.
		// Test the top vertex where dy is negative relative to center.
		int topY = screen.y - TILE_WIDTH * SCALE / 4;
		int[] grid = mapper.screenToGrid(screen.x, topY, SCALE);
		assertThat(grid[0]).isEqualTo(7);
		assertThat(grid[1]).isEqualTo(2);
	}

	@Test
	void screenToGridRoundTripWithDifferentScale() {
		int scale = 3;
		int row = 4, col = 6;
		IsometricMapper.IsometricCoordinate screen = mapper.gridToScreen(
			row, col, scale
		);
		int[] grid = mapper.screenToGrid(screen.x, screen.y, scale);
		assertThat(grid[0]).isEqualTo(row);
		assertThat(grid[1]).isEqualTo(col);
	}
}

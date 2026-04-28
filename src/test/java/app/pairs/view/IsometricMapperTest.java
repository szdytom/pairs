package app.pairs.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IsometricMapperTest {
	private static final int TILE_WIDTH = 16;
	private static final int TILE_HEIGHT = 16;
	private static final int ORIGIN_X = 512;
	private static final int ORIGIN_Y = 80;

	private final IsometricMapper mapper = new IsometricMapper(
		TILE_WIDTH, TILE_HEIGHT, ORIGIN_X, ORIGIN_Y
	);

	@Test
	void logicalToGridRoundTripsCenter() {
		int[][] points = {{0, 0}, {5, 5}, {0, 9}, {9, 0}, {3, 7}};
		for (int[] p : points) {
			int row = p[0], col = p[1];
			IsometricMapper.IsometricCoordinate logical = mapper.gridToLogical(
				row, col
			);
			int[] grid = mapper.logicalToGrid(logical.x, logical.y);
			assertThat(grid[0]).isEqualTo(row);
			assertThat(grid[1]).isEqualTo(col);
		}
	}

	@Test
	void containsReturnsTrueForCenter() {
		int[][] points = {{0, 0}, {5, 5}, {9, 9}};
		for (int[] p : points) {
			int row = p[0], col = p[1];
			IsometricMapper.IsometricCoordinate logical = mapper.gridToLogical(
				row, col
			);
			assertThat(mapper.contains(logical.x, logical.y, row, col))
				.isTrue();
		}
	}

	@Test
	void containsReturnsFalseForFarPoint() {
		assertThat(mapper.contains(0, 0, 5, 5)).isFalse();
	}

	@Test
	void containsDiamondExtendsToTopVertex() {
		int row = 5, col = 5;
		IsometricMapper.IsometricCoordinate center = mapper.gridToLogical(
			row, col
		);
		int halfH = TILE_HEIGHT / 2;
		assertThat(mapper.contains(center.x, center.y - halfH, row, col))
			.isTrue();
		assertThat(mapper.contains(center.x, center.y - halfH - 1, row, col))
			.isFalse();
	}

	@Test
	void logicalToGridWorksWithNegativeFractionalRegion() {
		IsometricMapper.IsometricCoordinate logical = mapper.gridToLogical(
			7, 2
		);
		int topY = logical.y - TILE_WIDTH / 4;
		int[] grid = mapper.logicalToGrid(logical.x, topY);
		assertThat(grid[0]).isEqualTo(7);
		assertThat(grid[1]).isEqualTo(2);
	}
}

package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TilemapGeneratorCoreTest {
	@Test
	void rejectsZeroTypes() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				()
					-> new CustomizedTilemapFactory()
						   .setWidth(4)
						   .setHeight(4)
						   .setTypes(0)
						   .generate()
			)
			.withMessageContaining("types must be >= 1");
	}

	@Test
	void rejectsNegativeTypes() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				()
					-> new CustomizedTilemapFactory()
						   .setWidth(4)
						   .setHeight(4)
						   .setTypes(-3)
						   .generate()
			)
			.withMessageContaining("types must be >= 1");
	}

	@Test
	void rejectsOddFillableTileCount() {
		// 3x3 map = 9 fillable tiles (odd)
		assertThatIllegalArgumentException()
			.isThrownBy(
				()
					-> new CustomizedTilemapFactory()
						   .setWidth(3)
						   .setHeight(3)
						   .setTypes(1)
						   .generate()
			)
			.withMessageContaining("Fillable tile count must be even");
	}

	@Test
	void rejectsInsufficientTilesForTypes() {
		// 2x2 map = 4 tiles = 2 pairs, but requesting 3 types
		assertThatIllegalArgumentException()
			.isThrownBy(
				()
					-> new CustomizedTilemapFactory()
						   .setWidth(2)
						   .setHeight(2)
						   .setTypes(3)
						   .generate()
			)
			.withMessageContaining("Not enough fillable tiles");
	}
}

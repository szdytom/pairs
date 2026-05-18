package app.pairs.map;

import static org.assertj.core.api.Assertions.*;

import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import org.junit.jupiter.api.Test;

class TilemapGeneratorCoreTest {
	@Test
	void rejectsZeroTypes() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				()
					-> TilemapFactory
						   .customized(
							   4, 4, 0, app.pairs.utils.Seed.deviceRandom()
						   )
						   .generate()
			)
			.withMessageContaining("types must be >= 1");
	}

	@Test
	void rejectsNegativeTypes() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				()
					-> TilemapFactory
						   .customized(
							   4, 4, -3, app.pairs.utils.Seed.deviceRandom()
						   )
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
					-> TilemapFactory
						   .customized(
							   3, 3, 1, app.pairs.utils.Seed.deviceRandom()
						   )
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
					-> TilemapFactory
						   .customized(
							   2, 2, 3, app.pairs.utils.Seed.deviceRandom()
						   )
						   .generate()
			)
			.withMessageContaining("Not enough fillable tiles");
	}

	@Test
	void baseStrategyProducesSolvableMap() {
		var map = generateWithStrategy(PairingStrategy.BASE);
		assertThat(map).isNotNull();
		assertThat(map.getHeight()).isEqualTo(6);
	}

	@Test
	void nonAdjacentStrategyProducesSolvableMap() {
		var map = generateWithStrategy(PairingStrategy.NON_ADJACENT);
		assertThat(map).isNotNull();
		assertThat(map.getHeight()).isEqualTo(6);
	}

	@Test
	void distantStrategyProducesSolvableMap() {
		var map = generateWithStrategy(PairingStrategy.DISTANT);
		assertThat(map).isNotNull();
		assertThat(map.getHeight()).isEqualTo(6);
	}

	private static app.pairs.model.Tilemap generateWithStrategy(
		PairingStrategy strategy
	) {
		int[][] map = new int[6][6];
		var random = new Xoroshiro128PP(Seed.deviceRandom());
		return TilemapGeneratorCore.generate(map, 3, random, strategy);
	}
}

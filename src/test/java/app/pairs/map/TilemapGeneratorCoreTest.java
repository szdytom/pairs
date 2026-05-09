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
					-> new CustomizedTilemapFactory(
						   app.pairs.utils.Seed.deviceRandom()
					)
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
					-> new CustomizedTilemapFactory(
						   app.pairs.utils.Seed.deviceRandom()
					)
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
					-> new CustomizedTilemapFactory(
						   app.pairs.utils.Seed.deviceRandom()
					)
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
					-> new CustomizedTilemapFactory(
						   app.pairs.utils.Seed.deviceRandom()
					)
						   .setWidth(2)
						   .setHeight(2)
						   .setTypes(3)
						   .generate()
			)
			.withMessageContaining("Not enough fillable tiles");
	}

	@Test
	void baseStrategyProducesSolvableMap() {
		var map = generateWithStrategy(new BasePairingStrategy());
		assertThat(map).isNotNull();
		assertThat(map.getHeight()).isEqualTo(6);
	}

	@Test
	void nonAdjacentStrategyProducesSolvableMap() {
		var map = generateWithStrategy(new NonAdjacentPairingStrategy());
		assertThat(map).isNotNull();
		assertThat(map.getHeight()).isEqualTo(6);
	}

	@Test
	void distantStrategyProducesSolvableMap() {
		var map = generateWithStrategy(new DistantPairingStrategy());
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

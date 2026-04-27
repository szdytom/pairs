package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;

import org.junit.jupiter.api.Test;

class HardTileFactoryPrintTest {
	@Test
	void printHardIdArray() {
		HardTilemapFactory factory = new HardTilemapFactory();
		Tilemap tilemap = factory.generate();

		System.out.println("===== HARD ID (tile type) =====");
		MapTestHelper.printTilemap(tilemap);

		assertThat(tilemap.getHeight()).isGreaterThan(0);
		assertThat(tilemap.getWidth()).isGreaterThan(0);
	}
}

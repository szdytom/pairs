package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import app.pairs.model.Tilemap;

class EasyTileFactoryPrintTest {
	@Test
	void printEasyIdArray() {
		EasyTilemapFactory factory = new EasyTilemapFactory();
		Tilemap tilemap = factory.generate();

		System.out.println("===== EASY ID (tile type) =====");
		MapTestHelper.printTilemap(tilemap);

		assertThat(tilemap.getHeight()).isGreaterThan(0);
		assertThat(tilemap.getWidth()).isGreaterThan(0);
	}
}

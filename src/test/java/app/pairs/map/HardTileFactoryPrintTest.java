package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;

import org.junit.jupiter.api.Test;

class HardTileFactoryPrintTest {
	@Test
	void printHardIdArray() {
		TilemapPreset preset = new TilemapPreset(12, 12, 20, null);
		Tilemap tilemap = CustomizedTilemapFactory
							  .fromPreset(preset, Seed.deviceRandom())
							  .generate();

		System.out.println("===== HARD ID (tile type) =====");
		MapTestHelper.printTilemap(tilemap);

		assertThat(tilemap.getHeight()).isEqualTo(12);
		assertThat(tilemap.getWidth()).isEqualTo(12);
	}
}

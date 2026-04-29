package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class EasyTileFactoryPrintTest {
	@Test
	void printEasyIdArray() {
		int[][] initial = new int[9][9];
		for (int[] row : initial) {
			Arrays.fill(row, -1);
		}
		for (int i = 0; i <= 3; i++) {
			for (int j = 0; j <= 3; j++) {
				initial[i][j] = 0;
			}
		}
		for (int i = 5; i <= 8; i++) {
			for (int j = 5; j <= 8; j++) {
				initial[i][j] = 0;
			}
		}
		TilemapPreset preset = new TilemapPreset(9, 9, 6, initial);
		Tilemap tilemap = new PresetTilemapFactory(preset).generate();

		System.out.println("===== EASY ID (tile type) =====");
		MapTestHelper.printTilemap(tilemap);

		assertThat(tilemap.getHeight()).isEqualTo(9);
		assertThat(tilemap.getWidth()).isEqualTo(9);
	}
}

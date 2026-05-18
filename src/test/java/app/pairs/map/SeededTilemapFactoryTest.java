package app.pairs.map;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import org.junit.jupiter.api.Test;

class SeededTilemapFactoryTest {
	@Test
	void sameSeedProducesSameMap() {
		Seed seed = Seed.fromString("hello-pairs");
		Tilemap a = TilemapFactory.customized(8, 8, 4, seed).generate();
		Tilemap b = TilemapFactory.customized(8, 8, 4, seed).generate();
		assertThat(flatten(a)).containsExactly(flatten(b));
	}

	@Test
	void differentSeedsProduceDifferentMaps() {
		Tilemap a = TilemapFactory.customized(8, 8, 4, new Seed(1L, 2L))
						.generate();
		Tilemap b = TilemapFactory.customized(8, 8, 4, new Seed(3L, 4L))
						.generate();
		assertThat(flatten(a)).isNotEqualTo(flatten(b));
	}

	@Test
	void xoroshiroJumpProducesIndependentStream() {
		Xoroshiro128PP base = new Xoroshiro128PP(new Seed(42L, 99L));
		Xoroshiro128PP jumped = base.jumpShort();
		// Base unchanged, jumped is a fresh independent generator.
		assertThat(base.nextLong()).isNotEqualTo(jumped.nextLong());
	}

	private static int[] flatten(Tilemap m) {
		int h = m.getHeight();
		int w = m.getWidth();
		int[] out = new int[h * w];
		int k = 0;
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				out[k++] = m.getTile(r, c);
			}
		}
		return out;
	}
}

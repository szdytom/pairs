package app.pairs.solver;
import java.util.Random;
public class ZKey {
	private static final long ZOBRIST_SEED = 0xC0FFEEL;
	public static long[] Generate(int height, int width) {
		long[] zKey = new long[height * width];
		Random rng = new Random(ZOBRIST_SEED);
		for (int i = 0; i < zKey.length; i++) {
			zKey[i] = rng.nextLong();
		}
		return zKey;
	}
}

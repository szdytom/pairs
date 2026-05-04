package app.pairs.utils;

import java.security.SecureRandom;

/**
 * 128-bit seed used by {@link Xoroshiro128PP}.
 * <p>
 * Both halves must not be simultaneously zero; the constructor patches
 * {@code s1 = 1} in that degenerate case.
 */
public final class Seed {
	private final long s0;
	private final long s1;

	public Seed(long s0, long s1) {
		if (s0 == 0L && s1 == 0L) {
			s1 = 1L;
		}
		this.s0 = s0;
		this.s1 = s1;
	}

	public long s0() {
		return s0;
	}

	public long s1() {
		return s1;
	}

	/**
	 * Derive a seed deterministically from a string using a simple polynomial
	 * hash — no crypto needed for a game seed.
	 */
	public static Seed fromString(String str) {
		long s0 = 0;
		for (int i = 0; i < str.length(); i++) {
			s0 = s0 * 31 + str.charAt(i);
		}
		return new Seed(s0, ~s0);
	}

	/** Draw a random seed from the OS entropy source. */
	public static Seed deviceRandom() {
		SecureRandom sr = new SecureRandom();
		return new Seed(sr.nextLong(), sr.nextLong());
	}

	@Override
	public String toString() {
		return String.format("Seed(%016x, %016x)", s0, s1);
	}
}

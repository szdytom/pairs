package app.pairs.utils;

import java.util.Random;

/**
 * Xoroshiro128++ PRNG, ported from the reference C implementation by
 * Blackman &amp; Vigna (<a
 * href="https://prng.di.unimi.it/xoroshiro128plusplus.c">prng.di.unimi.it</a>).
 * <p>
 * Extends {@link Random} so it works as a drop-in replacement anywhere a
 * {@code Random} is accepted (e.g. {@code Collections.shuffle}). Not
 * thread-safe.
 */
public final class Xoroshiro128PP extends Random {
	private static final long serialVersionUID = 1L;

	private long s0;
	private long s1;
	private boolean initialized;

	public Xoroshiro128PP(Seed seed) {
		super(0L); // triggers setSeed(0) before our fields are set; ignored.
		this.s0 = seed.s0();
		this.s1 = seed.s1();
		this.initialized = true;
	}

	public Xoroshiro128PP() {
		this(Seed.deviceRandom());
	}

	private Xoroshiro128PP(long s0, long s1) {
		super(0L);
		this.s0 = s0;
		this.s1 = s1;
		this.initialized = true;
	}

	@Override
	public long nextLong() {
		long a = s0;
		long b = s1;
		long result = Long.rotateLeft(a + b, 17) + a;
		b ^= a;
		s0 = Long.rotateLeft(a, 49) ^ b ^ (b << 21);
		s1 = Long.rotateLeft(b, 28);
		return result;
	}

	@Override
	protected int next(int bits) {
		return (int)(nextLong() >>> (64 - bits));
	}

	/**
	 * Re-seeds the generator. Calls from the {@link Random} super-constructor
	 * (before our state is initialized) are silently ignored. Subsequent
	 * calls derive a fresh 128-bit state from the given long via SplitMix64.
	 */
	@Override
	public void setSeed(long seed) {
		if (!initialized) {
			return;
		}
		long a = splitmix64(seed);
		long b = splitmix64(seed + 0x9E3779B97F4A7C15L);
		if (a == 0L && b == 0L) {
			b = 1L;
		}
		this.s0 = a;
		this.s1 = b;
	}

	private static long splitmix64(long x) {
		long z = x + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** Returns a copy of the current state. */
	public Xoroshiro128PP copy() {
		return new Xoroshiro128PP(s0, s1);
	}

	/**
	 * Returns a new generator advanced by 2^64 steps, useful for
	 * non-overlapping parallel streams. The receiver's state is unchanged.
	 */
	public Xoroshiro128PP jumpShort() {
		return jumpWith(JUMP_SHORT);
	}

	/**
	 * Returns a new generator advanced by 2^96 steps. The receiver's state
	 * is unchanged.
	 */
	public Xoroshiro128PP jumpLong() {
		return jumpWith(JUMP_LONG);
	}

	private static final long[] JUMP_SHORT = {
		0x2BD7A6A6E99C2DDCL, 0x0992CCAF6A6FCA05L
	};

	private static final long[] JUMP_LONG = {
		0x360FD5F2CF8D5D99L, 0x9C6E6877736C46E3L
	};

	private Xoroshiro128PP jumpWith(long[] poly) {
		long origS0 = s0;
		long origS1 = s1;
		long acc0 = 0L;
		long acc1 = 0L;
		for (long word : poly) {
			for (int b = 0; b < 64; b++) {
				if ((word & (1L << b)) != 0L) {
					acc0 ^= s0;
					acc1 ^= s1;
				}
				nextLong();
			}
		}
		// Restore original state, return jumped state as a fresh generator.
		this.s0 = origS0;
		this.s1 = origS1;
		return new Xoroshiro128PP(acc0, acc1);
	}

	private static final class Holder {
		static final Xoroshiro128PP INSTANCE = new Xoroshiro128PP();
	}

	/** Process-wide shared instance. Not thread-safe. */
	public static Xoroshiro128PP globalInstance() {
		return Holder.INSTANCE;
	}
}

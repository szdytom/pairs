package app.pairs.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 128-bit seed used by {@link Xoroshiro128PP}.
 * <p>
 * Both halves must not be simultaneously zero; the constructor enforces this
 * by patching {@code s1 = 1} in that degenerate case.
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

	/** Derive a seed deterministically from a string via SHA-256. */
	public static Seed fromString(String str) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
		byte[] digest = md.digest(
			str.getBytes(java.nio.charset.StandardCharsets.UTF_8)
		);
		ByteBuffer buf = ByteBuffer.wrap(digest).order(ByteOrder.LITTLE_ENDIAN);
		return new Seed(buf.getLong(), buf.getLong());
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

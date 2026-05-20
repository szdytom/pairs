package app.pairs.save;

import app.pairs.model.GameType;

import java.security.*;
import java.security.spec.*;
import java.sql.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Users {
	private static final int ITERATIONS = 100_000;
	private static final int KEY_LENGTH = 256;
	private static final int SALT_BYTES = 16;

	private final Connection connection;

	Users(Connection connection) {
		this.connection = connection;
	}

	public boolean containsUser(String username) {
		try (
			PreparedStatement ps = connection.prepareStatement(
				"SELECT 1 FROM users WHERE name = ?"
			)
		) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to query user", e);
		}
	}

	public void changePassword(String username, String newPassword) {
		if (newPassword == null) {
			throw new IllegalArgumentException("password must not be null");
		}
		try (
			PreparedStatement ps = connection.prepareStatement(
				"UPDATE users SET password_hash = ? WHERE name = ?"
			)
		) {
			ps.setString(1, hashPassword(newPassword));
			ps.setString(2, username);
			int updated = ps.executeUpdate();
			if (updated != 1) {
				throw new IllegalStateException(
					"failed to update password for user: " + username
				);
			}
		} catch (SQLException | GeneralSecurityException e) {
			throw new IllegalStateException("failed to change password", e);
		}
	}

	// Returns true if the password matches the stored hash.
	public boolean checkPassword(String username, String password) {
		if (password == null) {
			return false;
		}
		try (
			PreparedStatement ps = connection.prepareStatement(
				"SELECT password_hash FROM users WHERE name = ?"
			)
		) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return false;
				}
				return verifyPassword(password, rs.getString("password_hash"));
			}
		} catch (SQLException | GeneralSecurityException e) {
			throw new IllegalStateException("failed to check password", e);
		}
	}

	public void createUser(String username, String password) {
		if (password == null) {
			throw new IllegalArgumentException("password must not be null");
		}
		try (
			PreparedStatement ps = connection.prepareStatement(
				"INSERT INTO users (name, password_hash) VALUES (?, ?)"
			)
		) {
			ps.setString(1, username);
			ps.setString(2, hashPassword(password));
			ps.executeUpdate();
		} catch (SQLException | GeneralSecurityException e) {
			throw new IllegalStateException("failed to create user", e);
		}
	}

	public void addScore(String username, GameType difficulty, long score) {
		try (
			PreparedStatement ps = connection.prepareStatement(
				"INSERT INTO scores (user_id, difficulty, score, played_at)"
				+ " VALUES ((SELECT id FROM users WHERE name = ?), ?, ?, ?)"
				+ " ON CONFLICT(user_id, difficulty) DO UPDATE SET"
				+ " score = excluded.score, played_at = excluded.played_at"
				+ " WHERE excluded.score > scores.score"
			)
		) {
			ps.setString(1, username);
			ps.setString(2, difficulty.name());
			ps.setLong(3, score);
			ps.setLong(4, System.currentTimeMillis());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to add score", e);
		}
	}

	private static String hashPassword(String password)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] salt = new byte[SALT_BYTES];
		new SecureRandom().nextBytes(salt);
		byte[] hash = pbkdf2(password, salt);
		Base64.Encoder enc = Base64.getEncoder();
		return enc.encodeToString(salt) + ":" + enc.encodeToString(hash);
	}

	private static boolean verifyPassword(String password, String stored)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		String[] parts = stored.split(":", 2);
		if (parts.length != 2) {
			return false;
		}
		Base64.Decoder dec = Base64.getDecoder();
		byte[] salt = dec.decode(parts[0]);
		byte[] expected = dec.decode(parts[1]);
		byte[] actual = pbkdf2(password, salt);
		return MessageDigest.isEqual(actual, expected);
	}

	private static byte[] pbkdf2(String password, byte[] salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeySpec spec = new PBEKeySpec(
			password.toCharArray(), salt, ITERATIONS, KEY_LENGTH
		);
		return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
			.generateSecret(spec)
			.getEncoded();
	}
}

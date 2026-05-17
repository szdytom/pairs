package app.pairs.save;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {
	private static Database instance;

	public static Database instance() {
		if (instance == null) {
			instance = new Database();
		}
		return instance;
	}

	private final Connection connection;

	private Database() {
		this(SavePath.get());
	}

	Database(String dbPath) {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
			init();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to open database", e);
		}
	}

	public Save saves() {
		return new Save(connection);
	}

	public Save saves(String username) {
		return new Save(connection, userId(username));
	}

	public RogueSave rogueSave(String username) {
		return new RogueSave(connection, userId(username));
	}

	public Users users() {
		return new Users(connection);
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to close database", e);
		}
	}

	private long userId(String username) {
		try (
			PreparedStatement statement = connection.prepareStatement(
				"SELECT id FROM users WHERE name = ?"
			)
		) {
			statement.setString(1, username);
			try (ResultSet rows = statement.executeQuery()) {
				if (!rows.next()) {
					throw new IllegalStateException(
						"user not found: " + username
					);
				}
				return rows.getLong(1);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to query user id", e);
		}
	}

	private void init() throws SQLException {
		try (Statement st = connection.createStatement()) {
			st.executeUpdate(
				"CREATE TABLE IF NOT EXISTS users ("
				+ "id            INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name          TEXT NOT NULL UNIQUE, "
				+ "password_hash TEXT NOT NULL)"
			);
			st.executeUpdate(
				"CREATE TABLE IF NOT EXISTS saves ("
				+ "id         INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "user_id    INTEGER REFERENCES users(id), "
				+ "updated_at INTEGER NOT NULL, "
				+ "type       TEXT NOT NULL, "
				+ "json_data  TEXT NOT NULL)"
			);
			st.executeUpdate(
				"CREATE TABLE IF NOT EXISTS scores ("
				+ "id         INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "user_id    INTEGER NOT NULL REFERENCES users(id), "
				+ "difficulty TEXT NOT NULL, "
				+ "score      INTEGER NOT NULL, "
				+ "played_at  INTEGER NOT NULL)"
			);
			st.executeUpdate(
				"CREATE INDEX IF NOT EXISTS idx_scores_rank "
				+ "ON scores(difficulty, score DESC, user_id)"
			);
			st.executeUpdate(
				"CREATE INDEX IF NOT EXISTS idx_saves_user "
				+ "ON saves(user_id, updated_at DESC)"
			);
			// Enforce one rogue save per user without a separate table.
			st.executeUpdate(
				"CREATE UNIQUE INDEX IF NOT EXISTS idx_rogue_save "
				+ "ON saves(user_id) WHERE type = 'ROGUE'"
			);
		}
		// Idempotent migration: add soft-delete column if not already present.
		try (Statement ms = connection.createStatement()) {
			ms.executeUpdate(
				"ALTER TABLE saves ADD COLUMN is_deleted INTEGER NOT NULL "
				+ "DEFAULT 0"
			);
		} catch (SQLException ignored) {
			// Column already exists — safe to ignore.
		}
		// Idempotent migration: add is_rogue column if not already present.
		try (Statement ms = connection.createStatement()) {
			ms.executeUpdate(
				"ALTER TABLE saves ADD COLUMN is_rogue INTEGER NOT NULL "
				+ "DEFAULT 0"
			);
		} catch (SQLException ignored) {
			// Column already exists — safe to ignore.
		}
	}
}

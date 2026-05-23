package app.pairs.save;

import app.pairs.model.GameType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database implements AutoCloseable {
	private static Database instance;
	private static boolean wasCorrupted = false;

	public static Database instance() {
		if (instance == null) {
			instance = new Database();
		}
		return instance;
	}

	public static boolean wasCorrupted() {
		return wasCorrupted;
	}

	private final Connection connection;

	private Database() {
		this(SavePath.get(), true);
	}

	Database(String dbPath) {
		this(dbPath, false);
	}

	private Database(String dbPath, boolean recoverCorrupted) {
		Connection conn;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
			init(conn);
		} catch (SQLException e) {
			if (!recoverCorrupted) {
				throw new IllegalStateException("failed to open database", e);
			}
			renameCorrupted(dbPath);
			wasCorrupted = true;
			conn = openRecovered(dbPath);
		}
		connection = conn;
	}

	private static Connection openRecovered(String dbPath) {
		try {
			Connection conn = DriverManager.getConnection(
				"jdbc:sqlite:" + dbPath
			);
			init(conn);
			return conn;
		} catch (SQLException e) {
			throw new IllegalStateException(
				"failed to open database after recovery", e
			);
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

	public List<LeaderboardEntry> listLeaderboard() {
		List<LeaderboardEntry> entries = new ArrayList<>();
		try (
			PreparedStatement ps = connection.prepareStatement(
				"SELECT u.name, s.difficulty, s.score, s.played_at"
				+ " FROM scores s JOIN users u ON s.user_id = u.id"
				+ " ORDER BY s.score DESC, s.played_at ASC"
			)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					entries.add(new LeaderboardEntry(
						rs.getString(1), GameType.valueOf(rs.getString(2)),
						rs.getLong(3), rs.getLong(4)
					));
				}
			}
			return entries;
		} catch (SQLException e) {
			throw new IllegalStateException("failed to list leaderboard", e);
		}
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to close database", e);
		}
	}

	public void runInTransaction(Runnable action) {
		try {
			connection.setAutoCommit(false);
			action.run();
			connection.commit();
			connection.setAutoCommit(true);
		} catch (RuntimeException e) {
			silentRollback();
			throw e;
		} catch (SQLException e) {
			silentRollback();
			throw new IllegalStateException("transaction failed", e);
		}
	}

	private void silentRollback() {
		try {
			connection.rollback();
			connection.setAutoCommit(true);
		} catch (SQLException ignore) {}
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

	private static void renameCorrupted(String dbPath) {
		Path src = Paths.get(dbPath);
		if (!Files.exists(src)) {
			return;
		}
		Path dir = src.getParent();
		for (int i = 0;; i++) {
			Path dst = dir.resolve("BrokenDb_" + i);
			if (!Files.exists(dst)) {
				try {
					Files.move(src, dst);
				} catch (IOException ex) {
					// Rename failed — delete to allow creating a fresh DB.
					try {
						Files.delete(src);
					} catch (IOException ignored) {}
				}
				return;
			}
		}
	}

	private static void init(Connection connection) throws SQLException {
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
				+ "map_data   TEXT, "
				+ "rogue_data TEXT)"
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
				"CREATE UNIQUE INDEX IF NOT EXISTS idx_scores_user_difficulty "
				+ "ON scores(user_id, difficulty)"
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
				+ "ON saves(user_id) WHERE rogue_data IS NOT NULL"
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
		// Idempotent migration: add map_data column if not already present.
		try (Statement ms = connection.createStatement()) {
			ms.executeUpdate("ALTER TABLE saves ADD COLUMN map_data TEXT");
		} catch (SQLException ignored) {}
		// Idempotent migration: add rogue_data column if not already present.
		try (Statement ms = connection.createStatement()) {
			ms.executeUpdate("ALTER TABLE saves ADD COLUMN rogue_data TEXT");
		} catch (SQLException ignored) {}
		validateSavesSchema(connection);
	}

	private static void validateSavesSchema(Connection connection)
		throws SQLException {
		boolean hasMapData = false;
		boolean hasRogueData = false;
		boolean hasJsonData = false;
		boolean hasIsDeleted = false;
		try (Statement st = connection.createStatement();
		     ResultSet rs = st.executeQuery("PRAGMA table_info(saves)")) {
			while (rs.next()) {
				String name = rs.getString("name");
				hasMapData |= "map_data".equals(name);
				hasRogueData |= "rogue_data".equals(name);
				hasJsonData |= "json_data".equals(name);
				hasIsDeleted |= "is_deleted".equals(name);
			}
		}
		if (hasJsonData || !hasMapData || !hasRogueData || !hasIsDeleted) {
			throw new SQLException("incompatible saves table schema");
		}
	}
}

package app.pairs.save;

import java.sql.Connection;
import java.sql.DriverManager;
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

	// users() and scores() are not yet exposed.
	// Tables exist in the schema for future use.

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to close database", e);
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
		}
	}
}

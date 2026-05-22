package app.pairs.save;

import app.pairs.model.GameSnapshot;
import app.pairs.model.GameType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class Save {
	private final Connection connection;
	private final Long userId;
	private final Gson gson = new Gson();

	Save(Connection connection) {
		this(connection, null);
	}

	Save(Connection connection, Long userId) {
		this.connection = connection;
		this.userId = userId;
	}

	public long save(GameSnapshot snapshot) {
		String json = gson.toJson(snapshot);
		long now = System.currentTimeMillis();
		try (
			PreparedStatement statement = connection.prepareStatement(
				insertSql(), Statement.RETURN_GENERATED_KEYS
			)
		) {
			int index = 1;
			if (userId != null) {
				statement.setLong(index++, userId);
			}
			statement.setLong(index++, now);
			statement.setString(index++, snapshot.type().name());
			statement.setString(index++, json);
			statement.executeUpdate();
			try (ResultSet keys = statement.getGeneratedKeys()) {
				if (!keys.next()) {
					throw new IllegalStateException(
						"save id was not generated"
					);
				}
				return keys.getLong(1);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to write save", e);
		}
	}

	public List<SaveEntry> list() {
		List<SaveEntry> entries = new ArrayList<>();
		try (
			PreparedStatement statement = connection.prepareStatement(listSql())
		) {
			if (userId != null) {
				statement.setLong(1, userId);
			}
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					entries.add(new SaveEntry(
						rows.getLong(1), rows.getLong(2),
						GameType.valueOf(rows.getString(3))
					));
				}
				return entries;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to list saves", e);
		}
	}

	public GameSnapshot load(long id) {
		try (
			PreparedStatement statement = connection.prepareStatement(loadSql())
		) {
			statement.setLong(1, id);
			if (userId != null) {
				statement.setLong(2, userId);
			}
			try (ResultSet rows = statement.executeQuery()) {
				if (!rows.next()) {
					throw new IllegalStateException("save not found: " + id);
				}
				return gson.fromJson(rows.getString(1), GameSnapshot.class);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to load save", e);
		}
	}

	public void softDelete(long id) {
		try (
			PreparedStatement statement = connection.prepareStatement(
				softDeleteSql()
			)
		) {
			statement.setLong(1, id);
			if (userId != null) {
				statement.setLong(2, userId);
			}
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to soft-delete save", e);
		}
	}

	public void update(long id, GameSnapshot snapshot) {
		String json = gson.toJson(snapshot);
		long now = System.currentTimeMillis();
		try (
			PreparedStatement statement = connection.prepareStatement(
				updateSql()
			)
		) {
			statement.setString(1, json);
			statement.setLong(2, now);
			statement.setLong(3, id);
			if (userId != null) {
				statement.setLong(4, userId);
			}
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to update save", e);
		}
	}

	private String insertSql() {
		return "INSERT INTO saves (user_id, updated_at, type, map_data) "
			+ (userId == null ? "VALUES (NULL, ?, ?, ?)"
		                      : "VALUES (?, ?, ?, ?)");
	}

	private String listSql() {
		return "SELECT id, updated_at, type FROM saves"
			+ (userId == null ? " WHERE is_deleted = 0"
		               + " AND (map_data IS NOT NULL OR rogue_data IS NOT NULL)"
		                      : " WHERE user_id = ? AND is_deleted = 0"
		               + " AND (map_data IS NOT NULL OR rogue_data IS NOT NULL)")
			+ " ORDER BY updated_at DESC";
	}

	private String loadSql() {
		return "SELECT map_data FROM saves WHERE id = ? AND is_deleted = 0"
			+ (userId == null ? "" : " AND user_id = ?");
	}

	private String softDeleteSql() {
		return "UPDATE saves SET is_deleted = 1 WHERE id = ?"
			+ (userId == null ? "" : " AND user_id = ?");
	}

	private String updateSql() {
		return "UPDATE saves SET map_data = ?, updated_at = ? WHERE id = ?"
			+ (userId == null ? "" : " AND user_id = ?");
	}

	public Map<GameType, Long> listBestScores() {
		if (userId == null) {
			throw new IllegalStateException("listBestScores requires a user");
		}
		Map<GameType, Long> result = new EnumMap<>(GameType.class);
		try (
			PreparedStatement ps = connection.prepareStatement(
				"SELECT difficulty, score FROM scores WHERE user_id = ?"
			)
		) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					result.put(
						GameType.valueOf(rs.getString(1)), rs.getLong(2)
					);
				}
			}
			return result;
		} catch (SQLException e) {
			throw new IllegalStateException("failed to list best scores", e);
		}
	}
}

package app.pairs.save;

import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.OpLogs;
import app.pairs.model.Tilemap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class Save {
	private final Connection connection;
	private final Gson gson = new Gson();

	Save(Connection connection) {
		this.connection = connection;
	}

	public long save(Tilemap tilemap, OpLogs opLogs, GameStatus gameStatus) {
		GameSnapshot snapshot = new GameSnapshot(
			tilemap.getDifficulty(), gameStatus.score, gameStatus.combo,
			copy(tilemap), opLogs.snapshots()
		);
		String json = gson.toJson(snapshot);
		long now = System.currentTimeMillis();
		try (
			PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO saves (user_id, updated_at, type, json_data) "
					+ "VALUES (NULL, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
			)
		) {
			statement.setLong(1, now);
			statement.setString(2, tilemap.getDifficulty().name());
			statement.setString(3, json);
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
		try (PreparedStatement statement = connection.prepareStatement(
				 "SELECT id, updated_at, type FROM saves ORDER BY "
				 + "updated_at DESC"
			 );
		     ResultSet rows = statement.executeQuery()) {
			while (rows.next()) {
				entries.add(new SaveEntry(
					rows.getLong(1), rows.getLong(2),
					Tilemap.Difficulty.valueOf(rows.getString(3))
				));
			}
			return entries;
		} catch (SQLException e) {
			throw new IllegalStateException("failed to list saves", e);
		}
	}

	public GameSnapshot load(long id) {
		try (
			PreparedStatement statement = connection.prepareStatement(
				"SELECT json_data FROM saves WHERE id = ?"
			)
		) {
			statement.setLong(1, id);
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

	public void delete(long id) {
		try (
			PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM saves WHERE id = ?"
			)
		) {
			statement.setLong(1, id);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to delete save", e);
		}
	}

	private int[][] copy(Tilemap tilemap) {
		int[][] result = new int[tilemap.getHeight()][tilemap.getWidth()];
		for (int row = 0; row < tilemap.getHeight(); row++) {
			for (int col = 0; col < tilemap.getWidth(); col++) {
				result[row][col] = tilemap.getTile(row, col);
			}
		}
		return result;
	}
}
// autosave can be added
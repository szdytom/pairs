package app.pairs.save;

import app.pairs.model.GameStatus;
import app.pairs.model.OpLogs;
import app.pairs.model.RogueSession;
import app.pairs.model.RogueSnapshot;
import app.pairs.model.Tilemap;
import app.pairs.model.TilemapType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.google.gson.Gson;

// One rogue save slot per user — enforced by a partial unique index on
// saves(user_id) WHERE type='ROGUE'.
public class RogueSave {
	public static final TilemapType TYPE = TilemapType.ROGUE;

	private final Connection connection;
	private final long userId;
	private final Gson gson = new Gson();

	RogueSave(Connection connection, long userId) {
		this.connection = connection;
		this.userId = userId;
	}

	public void save(
		RogueSession session, Tilemap tilemap, OpLogs opLogs,
		GameStatus gameStatus
	) {
		RogueSnapshot snapshot = RogueSnapshot.from(
			session, tilemap, opLogs, gameStatus
		);
		String json = gson.toJson(snapshot);
		long now = System.currentTimeMillis();
		try (
			PreparedStatement st = connection.prepareStatement(
				"INSERT OR REPLACE INTO saves "
				+ "(user_id, updated_at, type, json_data) VALUES (?, ?, ?, ?)"
			)
		) {
			st.setLong(1, userId);
			st.setLong(2, now);
			st.setString(3, TYPE.name());
			st.setString(4, json);
			st.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to write rogue save", e);
		}
	}

	public Optional<RogueSnapshot> load() {
		try (
			PreparedStatement st = connection.prepareStatement(
				"SELECT json_data FROM saves WHERE user_id = ? AND type = ?"
			)
		) {
			st.setLong(1, userId);
			st.setString(2, TYPE.name());
			try (ResultSet rows = st.executeQuery()) {
				if (!rows.next()) {
					return Optional.empty();
				}
				return Optional.of(
					gson.fromJson(rows.getString(1), RogueSnapshot.class)
				);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to load rogue save", e);
		}
	}

	public void delete() {
		try (
			PreparedStatement st = connection.prepareStatement(
				"DELETE FROM saves WHERE user_id = ? AND type = ?"
			)
		) {
			st.setLong(1, userId);
			st.setString(2, TYPE.name());
			st.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to delete rogue save", e);
		}
	}
}

package app.pairs.save;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameType;
import app.pairs.model.RogueSession;
import app.pairs.model.RogueSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.google.gson.Gson;

// One rogue save slot per user — enforced by a partial unique index on
// saves(user_id) WHERE rogue_data IS NOT NULL.
public class RogueSave {
	public static final GameType TYPE = GameType.ROGUE;

	private final Connection connection;
	private final long userId;
	private final Gson gson = new Gson();

	RogueSave(Connection connection, long userId) {
		this.connection = connection;
		this.userId = userId;
	}

	public void save(RogueSession session) {
		save(session, null);
	}

	public void save(RogueSession session, GameState gameState) {
		String rogueJson = gson.toJson(RogueSnapshot.from(session));
		String mapJson = gameState != null
			? gson.toJson(gameState.toSnapshot())
			: null;
		long now = System.currentTimeMillis();
		try (
			PreparedStatement st = connection.prepareStatement(
				"INSERT OR REPLACE INTO saves "
				+ "(user_id, updated_at, type, map_data, rogue_data, "
				+ "is_deleted) VALUES (?, ?, ?, ?, ?, 0)"
			)
		) {
			st.setLong(1, userId);
			st.setLong(2, now);
			st.setString(3, TYPE.name());
			if (mapJson != null) {
				st.setString(4, mapJson);
			} else {
				st.setNull(4, java.sql.Types.NULL);
			}
			st.setString(5, rogueJson);
			st.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("failed to write rogue save", e);
		}
	}

	public Optional<RogueLoadData> load() {
		try (
			PreparedStatement st = connection.prepareStatement(
				"SELECT rogue_data, map_data FROM saves"
				+ " WHERE user_id = ? AND rogue_data IS NOT NULL"
				+ " AND is_deleted = 0"
			)
		) {
			st.setLong(1, userId);
			try (ResultSet rows = st.executeQuery()) {
				if (!rows.next()) {
					return Optional.empty();
				}
				RogueSnapshot rogueSnap
					= gson.fromJson(rows.getString(1), RogueSnapshot.class);
				RogueSession session = rogueSnap.toSession();
				String mapJson = rows.getString(2);
				GameState gameState = null;
				if (mapJson != null) {
					GameSnapshot
						mapSnap = gson.fromJson(mapJson, GameSnapshot.class);
					gameState = GameState.fromSnapshot(mapSnap);
				}
				return Optional.of(new RogueLoadData(session, gameState));
			}
		} catch (SQLException e) {
			throw new IllegalStateException("failed to load rogue save", e);
		}
	}

	public void softDelete() {
		try (
			PreparedStatement st = connection.prepareStatement(
				"UPDATE saves SET is_deleted = 1"
				+ " WHERE user_id = ? AND rogue_data IS NOT NULL"
			)
		) {
			st.setLong(1, userId);
			st.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException(
				"failed to soft-delete rogue save", e
			);
		}
	}
}

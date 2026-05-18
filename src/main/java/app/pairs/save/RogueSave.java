package app.pairs.save;

import app.pairs.model.GameStatus;
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
// saves(user_id) WHERE type='ROGUE'.
public class RogueSave {
	public static final GameType TYPE = GameType.ROGUE;

	private final Connection connection;
	private final long userId;
	private final Gson gson = new Gson();

	RogueSave(Connection connection, long userId) {
		this.connection = connection;
		this.userId = userId;
	}

	/**
	 * @param relatedMapId id of the linked map save, or {@code null} for shop
	 * @param gameStatus   current level status, or {@code null} for shop state
	 */
	public void save(
		RogueSession session, Long relatedMapId, GameStatus gameStatus
	) {
		RogueSnapshot snapshot = RogueSnapshot.from(
			session, relatedMapId, gameStatus
		);
		String json = gson.toJson(snapshot);
		long now = System.currentTimeMillis();
		try (
			PreparedStatement st = connection.prepareStatement(
				"INSERT OR REPLACE INTO saves "
				+ "(user_id, updated_at, type, json_data, is_deleted) VALUES "
				+ "(?, ?, ?, ?, 0)"
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
				"SELECT json_data FROM saves"
				+ " WHERE user_id = ? AND type = ? AND is_deleted = 0"
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

	public void softDelete() {
		try (
			PreparedStatement st = connection.prepareStatement(
				"UPDATE saves SET is_deleted = 1 WHERE user_id = ? AND type = ?"
			)
		) {
			st.setLong(1, userId);
			st.setString(2, TYPE.name());
			st.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException(
				"failed to soft-delete rogue save", e
			);
		}
	}
}

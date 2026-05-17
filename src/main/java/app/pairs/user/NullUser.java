package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.OpLogs;
import app.pairs.model.RogueSession;
import app.pairs.model.RogueSnapshot;
import app.pairs.model.Tilemap;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Optional;

// Null Object: represents guest mode (not logged in). save() is a no-op.
public class NullUser implements User {
	@Override
	public String getUsername() {
		return "Guest";
	}

	@Override
	public boolean isAuthorized() {
		return false;
	}

	@Override
	public void save(GameState st) {}

	@Override
	public long saveGame(
		Tilemap tilemap, OpLogs opLogs, GameStatus gameStatus
	) {
		return -1;
	}

	@Override
	public List<SaveEntry> listSaves() {
		return List.of();
	}

	@Override
	public Optional<GameSnapshot> loadSave(long id) {
		return Optional.empty();
	}

	@Override
	public void deleteSave(long id) {}

	@Override
	public void saveRogue(
		RogueSession session, Tilemap tilemap, OpLogs opLogs,
		GameStatus gameStatus
	) {}

	@Override
	public Optional<RogueSnapshot> loadRogue() {
		return Optional.empty();
	}

	@Override
	public void deleteRogue() {}
}

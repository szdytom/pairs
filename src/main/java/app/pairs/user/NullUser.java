package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameType;
import app.pairs.model.RogueSession;
import app.pairs.save.RogueLoadData;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Map;
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
	public long saveGame(GameState state) {
		return -1;
	}

	@Override
	public void updateSave(long id, GameState state) {}

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
	public void saveRogue(RogueSession session) {}

	@Override
	public void saveRogue(RogueSession session, GameState gameState) {}

	@Override
	public Optional<RogueLoadData> loadRogue() {
		return Optional.empty();
	}

	@Override
	public void deleteRogue() {}

	@Override
	public Map<GameType, Long> listBestScores() {
		return Map.of();
	}
}

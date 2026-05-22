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

public interface User {
	boolean isAuthorized();
	String getUsername();
	void save(GameState st);
	long saveGame(GameState state);
	void updateSave(long id, GameState state);
	List<SaveEntry> listSaves();
	Optional<GameSnapshot> loadSave(long id);
	void deleteSave(long id);
	void saveRogue(RogueSession session);
	void saveRogue(RogueSession session, GameState gameState);
	Optional<RogueLoadData> loadRogue();
	void deleteRogue();
	Map<GameType, Long> listBestScores();
}

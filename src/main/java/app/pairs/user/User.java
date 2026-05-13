package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.OpLogs;
import app.pairs.model.Tilemap;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Optional;

public interface User {
	String getUsername();
	void save(GameState st);
	long saveGame(Tilemap tilemap, OpLogs opLogs, GameStatus gameStatus);
	List<SaveEntry> listSaves();
	Optional<GameSnapshot> loadSave(long id);
	void deleteSave(long id);
}

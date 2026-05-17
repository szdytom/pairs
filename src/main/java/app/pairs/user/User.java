package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Optional;

public interface User {
	boolean isAuthorized();
	String getUsername();
	void save(GameState st);
	long saveGame(GameState state);
	List<SaveEntry> listSaves();
	Optional<GameSnapshot> loadSave(long id);
	void deleteSave(long id);
}

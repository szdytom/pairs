package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.GameType;
import app.pairs.model.RogueSession;
import app.pairs.model.RogueSnapshot;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface User {
	boolean isAuthorized();
	String getUsername();
	void save(GameState st);
	long saveGame(GameState state);
	/** Saves a rogue-linked map (hidden from the normal save list). */
	long saveRogueLinkedGame(GameState state);
	void updateSave(long id, GameState state);
	List<SaveEntry> listSaves();
	Optional<GameSnapshot> loadSave(long id);
	/** Soft-delete and record the save's score into the rankings. */
	void deleteSave(long id);
	/**
	 * Soft-delete without recording score (used for rogue linked-map cleanup).
	 */
	void discardSave(long id);
	void saveRogue(
		RogueSession session, Long relatedMapId, GameStatus gameStatus
	);
	Optional<RogueSnapshot> loadRogue();
	/** Soft-delete and record the rogue session score into the rankings. */
	void deleteRogue();
	Map<GameType, Long> listBestScores();
}

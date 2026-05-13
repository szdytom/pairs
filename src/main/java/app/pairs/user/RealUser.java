package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.OpLogs;
import app.pairs.model.Tilemap;
import app.pairs.save.Database;
import app.pairs.save.Save;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Optional;

public class RealUser implements User {
	private final String username;

	RealUser(String username) {
		this.username = username;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void save(GameState st) {
		Database.instance().users().addScore(
			username, st.getTilemap().getDifficulty(), st.gameStatus.score
		);
	}

	@Override
	public long saveGame(
		Tilemap tilemap, OpLogs opLogs, GameStatus gameStatus
	) {
		return saves().save(tilemap, opLogs, gameStatus);
	}

	@Override
	public List<SaveEntry> listSaves() {
		return saves().list();
	}

	@Override
	public Optional<GameSnapshot> loadSave(long id) {
		return Optional.of(saves().load(id));
	}

	@Override
	public void deleteSave(long id) {
		saves().delete(id);
	}

	private Save saves() {
		return Database.instance().saves(username);
	}
}

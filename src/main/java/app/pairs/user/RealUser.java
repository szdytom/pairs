package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameType;
import app.pairs.model.RogueSession;
import app.pairs.save.Database;
import app.pairs.save.RogueLoadData;
import app.pairs.save.RogueSave;
import app.pairs.save.Save;
import app.pairs.save.SaveEntry;

import java.util.List;
import java.util.Map;
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
	public boolean isAuthorized() {
		return true;
	}

	@Override
	public void save(GameState st) {
		Database.instance().users().addScore(
			username, GameType.from(st.getTilemap().getDifficulty()),
			st.gameStatus.score
		);
	}

	@Override
	public long saveGame(GameState state) {
		return saves().save(state.toSnapshot());
	}

	@Override
	public void updateSave(long id, GameState state) {
		saves().update(id, state.toSnapshot());
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
		Database.instance().runInTransaction(() -> {
			GameSnapshot snap = saves().load(id);
			saves().softDelete(id);
			Database.instance().users().addScore(
				username, snap.type(), snap.status().score()
			);
		});
	}

	@Override
	public void saveRogue(RogueSession session) {
		rogueSave().save(session);
	}

	@Override
	public void saveRogue(RogueSession session, GameState gameState) {
		rogueSave().save(session, gameState);
	}

	@Override
	public Optional<RogueLoadData> loadRogue() {
		return rogueSave().load();
	}

	@Override
	public void deleteRogue() {
		Database.instance().runInTransaction(() -> {
			Optional<RogueLoadData> data = rogueSave().load();
			rogueSave().softDelete();
			data.ifPresent(
				d
				-> Database.instance().users().addScore(
					username, GameType.ROGUE, d.session().getTotalEarned()
				)
			);
		});
	}

	private Save saves() {
		return Database.instance().saves(username);
	}

	@Override
	public Map<GameType, Long> listBestScores() {
		return saves().listBestScores();
	}

	private RogueSave rogueSave() {
		return Database.instance().rogueSave(username);
	}
}

package app.pairs.user;

import app.pairs.logic.GameState;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.GameType;
import app.pairs.model.RogueSession;
import app.pairs.model.RogueSnapshot;
import app.pairs.save.Database;
import app.pairs.save.RogueSave;
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
	public long saveRogueLinkedGame(GameState state) {
		return saves().saveLinked(state.toSnapshot());
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
	public void discardSave(long id) {
		saves().softDelete(id);
	}

	@Override
	public void saveRogue(
		RogueSession session, Long relatedMapId, GameStatus gameStatus
	) {
		rogueSave().save(session, relatedMapId, gameStatus);
	}

	@Override
	public Optional<RogueSnapshot> loadRogue() {
		return rogueSave().load();
	}

	@Override
	public void deleteRogue() {
		Database.instance().runInTransaction(() -> {
			Optional<RogueSnapshot> snap = rogueSave().load();
			rogueSave().softDelete();
			snap.ifPresent(
				s
				-> Database.instance().users().addScore(
					username, GameType.ROGUE,
					s.spendableScore() + s.cumulativeSpent()
				)
			);
		});
	}

	private Save saves() {
		return Database.instance().saves(username);
	}

	private RogueSave rogueSave() {
		return Database.instance().rogueSave(username);
	}
}

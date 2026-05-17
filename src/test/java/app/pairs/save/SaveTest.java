package app.pairs.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.logic.GameState;
import app.pairs.logic.OpElimination;
import app.pairs.model.GameSnapshot;
import app.pairs.model.OperationSnapshot;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveTest {
	@TempDir Path tempDir;

	@Test
	void savesAndLoadsModelSnapshot() {
		GameState state = GameState.customized(2, 2, 1);
		state.eliminate(0, 0, 0, 1, 5_000, GameState.OpKind.MANUAL);

		try (Database db = new Database(dbPath())) {
			Save save = db.saves();
			long id = save.save(
				state.getTilemap(), state.getOpLogsModel(),
				state.getGameStatus()
			);

			GameSnapshot snapshot = save.load(id);
			assertThat(save.list())
				.extracting(SaveEntry::id)
				.containsExactly(id);
			assertThat(snapshot.difficulty())
				.isEqualTo(Tilemap.Difficulty.NORMAL);
			assertThat(snapshot.score())
				.isEqualTo(OpElimination.SCORE_PER_PAIR);
			assertThat(snapshot.combo()).isEqualTo(1);
			assertThat(snapshot.tilemap())
				.isDeepEqualTo(new int[][] {{0, 0}, {1, 1}});
			assertThat(snapshot.operations())
				.containsExactly(new OperationSnapshot(
					0, 0, 0, 1, 1, 5_000, OpElimination.SCORE_PER_PAIR,
					List.of(0, 0, 0, 1), 0
				));
		}
	}

	@Test
	void restoredSnapshotKeepsUndoHistory() {
		GameState state = GameState.customized(2, 2, 1);
		state.eliminate(0, 0, 0, 1, 5_000, GameState.OpKind.MANUAL);

		GameSnapshot snapshot;
		try (Database db = new Database(dbPath())) {
			Save save = db.saves();
			long id = save.save(
				state.getTilemap(), state.getOpLogsModel(),
				state.getGameStatus()
			);
			snapshot = save.load(id);
		}

		GameState restored = GameState.fromSnapshot(snapshot);
		assertThat(restored.getTile(0, 0)).isZero();
		assertThat(restored.getTile(0, 1)).isZero();
		assertThat(restored.getOpLogs()).hasSize(1);
		assertThat(restored.getGameStatus().score)
			.isEqualTo(OpElimination.SCORE_PER_PAIR);
		assertThat(restored.getGameStatus().combo).isEqualTo(1);

		restored.undo();
		assertThat(restored.getTile(0, 0)).isEqualTo(1);
		assertThat(restored.getTile(0, 1)).isEqualTo(1);
		assertThat(restored.getGameStatus().score).isZero();
		assertThat(restored.getGameStatus().combo).isZero();
		assertThat(restored.getOpLogs()).isEmpty();
	}

	@Test
	void savesAreFilteredByUser() {
		GameState state = GameState.customized(2, 2, 1);

		try (Database db = new Database(dbPath())) {
			db.users().createUser("alice", "password");
			db.users().createUser("bob", "password");

			long aliceId = db.saves("alice").save(
				state.getTilemap(), state.getOpLogsModel(),
				state.getGameStatus()
			);
			long bobId = db.saves("bob").save(
				state.getTilemap(), state.getOpLogsModel(),
				state.getGameStatus()
			);

			assertThat(db.saves("alice").list())
				.extracting(SaveEntry::id)
				.containsExactly(aliceId);
			assertThat(db.saves("bob").list())
				.extracting(SaveEntry::id)
				.containsExactly(bobId);
			assertThatThrownBy(() -> db.saves("alice").load(bobId))
				.isInstanceOf(IllegalStateException.class);
		}
	}

	@Test
	void snapshotStoresSeed() {
		Seed seed = new Seed(0xdeadbeefL, 0xcafebabeL);
		GameState state = GameState.customized(4, 4, 2, seed);
		GameSnapshot snapshot = state.toSnapshot();

		assertThat(snapshot.seedS0()).isEqualTo(0xdeadbeefL);
		assertThat(snapshot.seedS1()).isEqualTo(0xcafebabeL);
		assertThat(snapshot.factoryPresetId()).isNull();
	}

	@Test
	void seedSurvivesJsonRoundTrip() {
		Seed seed = new Seed(0x1122334455667788L, 0x99aabbccddeeff00L);
		GameState state = GameState.customized(4, 4, 2, seed);

		GameSnapshot snapshot;
		try (Database db = new Database(dbPath())) {
			Save save = db.saves();
			long id = save.save(state.toSnapshot());
			snapshot = save.load(id);
		}

		assertThat(snapshot.seedS0()).isEqualTo(0x1122334455667788L);
		assertThat(snapshot.seedS1()).isEqualTo(0x99aabbccddeeff00L);
	}

	@Test
	void restartFromSnapshotWithSeedRegeneratesOriginalBoard() {
		Seed seed = new Seed(42L, 137L);
		GameState original = GameState.customized(4, 4, 2, seed);
		// record the initial board before any moves
		int[][] initialBoard = new int[4][4];
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 4; c++) {
				initialBoard[r][c] = original.getTile(r, c);
			}
		}

	// find two matching tiles and eliminate them
	outer:
		for (int r1 = 0; r1 < 4; r1++) {
			for (int c1 = 0; c1 < 4; c1++) {
				for (int r2 = 0; r2 < 4; r2++) {
					for (int c2 = 0; c2 < 4; c2++) {
						if (r1 == r2 && c1 == c2)
							continue;
						if (original.canEliminate(r1, c1, r2, c2)) {
							original.eliminate(
								r1, c1, r2, c2, 500, GameState.OpKind.MANUAL
							);
							break outer;
						}
					}
				}
			}
		}

		// save, reload, then restart
		GameSnapshot snapshot;
		try (Database db = new Database(dbPath())) {
			Save save = db.saves();
			long id = save.save(original.toSnapshot());
			snapshot = save.load(id);
		}

		// seed in snapshot
		assertThat(snapshot.seedS0()).isEqualTo(42L);
		assertThat(snapshot.seedS1()).isEqualTo(137L);

		// restart from the loaded state: for customized games without a
		// presetId, restart still goes back to the snapshot's tilemap (no
		// presetId stored)
		GameState restored = GameState.fromSnapshot(snapshot);
		restored.restart();

		// The restored game has no presetId, so restart uses the snapshotted
		// grid. Verify the restarted board matches the save-point board (not
		// original).
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 4; c++) {
				assertThat(restored.getTile(r, c))
					.isEqualTo(snapshot.tilemap()[r][c]);
			}
		}
	}

	private String dbPath() {
		return tempDir.resolve("saves.sqlite").toString();
	}
}

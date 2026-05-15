package app.pairs.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.pairs.logic.GameState;
import app.pairs.logic.OpElimination;
import app.pairs.model.GameSnapshot;
import app.pairs.model.OperationSnapshot;
import app.pairs.model.Tilemap;

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

	private String dbPath() {
		return tempDir.resolve("saves.sqlite").toString();
	}
}

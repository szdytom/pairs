package app.pairs.save;

import static org.assertj.core.api.Assertions.assertThat;

import app.pairs.model.GameType;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseTest {
	@TempDir Path tempDir;

	@Test
	void tablesAreCreatedOnOpen() {
		// Smoke test: opening a new database should not throw.
		try (Database db = new Database(dbPath())) {
			assertThat(db.saves().list()).isEmpty();
		}
	}

	@Test
	void addScoreKeepsBestScorePerUserAndDifficulty() {
		try (Database db = new Database(dbPath())) {
			db.users().createUser("alice", "password");
			db.users().addScore("alice", GameType.NORMAL, 100);
			db.users().addScore("alice", GameType.NORMAL, 90);
			db.users().addScore("alice", GameType.NORMAL, 120);

			assertThat(db.saves().listLeaderBoard())
				.singleElement()
				.satisfies(entry -> {
					assertThat(entry.username()).isEqualTo("alice");
					assertThat(entry.difficulty()).isEqualTo(GameType.NORMAL);
					assertThat(entry.score()).isEqualTo(120);
				});
		}
	}

	// The following tests are commented out until the user/score system
	// is wired into the frontend:

	// @Test void registerAndLogin() { ... }
	// @Test void loginFailsOnWrongPassword() { ... }
	// @Test void loginFailsOnUnknownUser() { ... }
	// @Test void savesAreFilteredByUser() { ... }
	// @Test void leaderboardRanksUsersByBestScore() { ... }
	// @Test void leaderboardIsFilteredByDifficulty() { ... }

	private String dbPath() {
		return tempDir.resolve("test.db").toString();
	}
}

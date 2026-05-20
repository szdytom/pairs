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

			assertThat(db.listLeaderboard())
				.singleElement()
				.satisfies(entry -> {
					assertThat(entry.username()).isEqualTo("alice");
					assertThat(entry.difficulty()).isEqualTo(GameType.NORMAL);
					assertThat(entry.score()).isEqualTo(120);
				});
		}
	}

	@Test
	void leaderboardRanksUsersByBestScore() {
		try (Database db = new Database(dbPath())) {
			db.users().createUser("alice", "pass1");
			db.users().createUser("bob", "pass2");
			db.users().addScore("alice", GameType.NORMAL, 100);
			db.users().addScore("bob", GameType.NORMAL, 200);
			db.users().addScore("alice", GameType.NORMAL, 150);

			assertThat(db.listLeaderboard())
				.hasSize(2)
				.satisfiesExactly(
					first
					-> {
						assertThat(first.username()).isEqualTo("bob");
						assertThat(first.score()).isEqualTo(200);
					},
					second -> {
						assertThat(second.username()).isEqualTo("alice");
						assertThat(second.score()).isEqualTo(150);
					}
				);
		}
	}

	@Test
	void leaderboardIsFilteredByDifficulty() {
		try (Database db = new Database(dbPath())) {
			db.users().createUser("alice", "password");
			db.users().addScore("alice", GameType.NORMAL, 50);
			db.users().addScore("alice", GameType.HARD, 80);

			assertThat(db.listLeaderboard())
				.hasSize(2)
				.anySatisfy(e -> {
					assertThat(e.difficulty()).isEqualTo(GameType.NORMAL);
					assertThat(e.score()).isEqualTo(50);
				})
				.anySatisfy(e -> {
					assertThat(e.difficulty()).isEqualTo(GameType.HARD);
					assertThat(e.score()).isEqualTo(80);
				});
		}
	}

	// The following tests are commented out until the user/score system
	// is wired into the frontend:

	// @Test void registerAndLogin() { ... }
	// @Test void loginFailsOnWrongPassword() { ... }
	// @Test void loginFailsOnUnknownUser() { ... }
	// @Test void savesAreFilteredByUser() { ... }

	private String dbPath() {
		return tempDir.resolve("test.db").toString();
	}
}

package app.pairs.save;

import static org.assertj.core.api.Assertions.assertThat;

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

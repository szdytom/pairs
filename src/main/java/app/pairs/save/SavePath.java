package app.pairs.save;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SavePath {
	private static final String APP_NAME = "pairs";
	private static final String DB_FILENAME = "save.db";

	public static String get() {
		Path dir = dataDir();
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new IllegalStateException(
				"failed to create save directory", e
			);
		}
		return dir.resolve(DB_FILENAME).toString();
	}

	private static Path dataDir() {
		String home = System.getProperty("user.home");
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("mac")) {
			return Paths.get(home, "Library", "Application Support", APP_NAME);
		}
		if (os.contains("win")) {
			String appData = System.getenv("APPDATA");
			if (appData == null) {
				appData = home;
			}
			return Paths.get(appData, APP_NAME);
		}
		// Linux / BSD / others: XDG Base Directory
		String xdgData = System.getenv("XDG_DATA_HOME");
		if (xdgData != null && !xdgData.isEmpty()) {
			return Paths.get(xdgData, APP_NAME);
		}
		return Paths.get(home, ".local", "share", APP_NAME);
	}
}

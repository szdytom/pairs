package app.pairs.asset;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class ClspAssetLoader implements AssetLoader {
	private final ClassLoader classLoader;

	public ClspAssetLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public InputStream load(String path) throws Exception {
		InputStream stream = classLoader.getResourceAsStream(path);
		if (stream == null) {
			throw new FileNotFoundException(
				"Asset not found in classpath: " + path
			);
		}
		return stream;
	}
}

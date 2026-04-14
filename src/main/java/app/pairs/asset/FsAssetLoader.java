package app.pairs.asset;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FsAssetLoader implements AssetLoader {
	private final String basePath;

	public FsAssetLoader(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public InputStream load(String path) throws Exception {
		Path fullPath = Paths.get(basePath, path).normalize();
		if (!Files.exists(fullPath)) {
			throw new FileNotFoundException("Asset not found: " + fullPath);
		}
		return Files.newInputStream(fullPath);
	}
}

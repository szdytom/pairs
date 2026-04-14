package app.pairs.asset;

import java.security.CodeSource;

public class AssetLoaderInstance {
	public static AssetLoader getInstance() {
		return instance;
	}

	private static AssetLoader instance = detect();

	private static AssetLoader detect() {
		String mode = System.getProperty("app.pairs.assetLoader", "auto");

		if (mode.equalsIgnoreCase("clsp")) {
			return new ClspAssetLoader(
				AssetLoaderInstance.class.getClassLoader()
			);
		} else if (mode.equalsIgnoreCase("fs")) {
			return new FsAssetLoader("assets");
		}

		// Auto-detect based on environment
		CodeSource codeSource = AssetLoaderInstance.class.getProtectionDomain()
									.getCodeSource();
		if (codeSource != null) {
			String location = codeSource.getLocation().toString();
			if (location.endsWith(".jar") || location.startsWith("jar:")) {
				return new ClspAssetLoader(
					AssetLoaderInstance.class.getClassLoader()
				);
			}
		}

		return new FsAssetLoader("assets");
	}
}

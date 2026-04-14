package app.pairs.asset;

import java.io.InputStream;

public interface AssetLoader {
	InputStream load(String path) throws Exception;
}

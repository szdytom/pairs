package app.pairs.asset;

import io.github.libsdl4j.api.render.*;

public class IconManager {
	private static final IconManager INSTANCE = new IconManager();

	private TileRegistry registry;

	private IconManager() {}

	public static IconManager instance() {
		return INSTANCE;
	}

	public SDL_Texture getTexture(String id) {
		if (registry == null) {
			registry = AssetManager.instance().get("icons/typed");
			registry.createTextures(AssetManager.instance().renderer());
		}
		int numId = registry.getNumericId(id);
		return numId != -1 ? registry.getTexture(numId) : null;
	}
}

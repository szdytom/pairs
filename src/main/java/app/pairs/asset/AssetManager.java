package app.pairs.asset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.libsdl4j.api.render.SDL_Renderer;

public class AssetManager {
	private static AssetManager INSTANCE;

	private final AssetOperation.Registry registry;
	private final Map<String, Object> assets = new HashMap<>();
	private SDL_Renderer renderer;
	private AssetLoader loader;

	public static AssetManager instance() {
		if (INSTANCE == null) {
			INSTANCE = new AssetManager();
		}
		return INSTANCE;
	}

	private AssetManager() {
		this.registry = new RegistryImpl();
		registerDefaultOperations();
	}

	public void init(SDL_Renderer renderer) {
		this.renderer = renderer;
		this.loader = AssetLoaderInstance.getInstance();
	}

	public SDL_Renderer renderer() {
		return renderer;
	}

	private void registerDefaultOperations() {
		registry.register("image", ImageOperation::new);
		registry.register("create-texture", CreateTextureOperation::new);
		registry.register("crop-tiles", CropTilesOperation::new);
		registry.register("tile-type-mapping", TileTypeMappingOperation::new);
		registry.register("mapping", MappingOperation::new);
		registry.register("bitmap-font", BitmapFontOperation::new);
		registry.register("tilemap-preset", TilemapPresetOperation::new);
	}

	public void loadManifest(String path) throws Exception {
		System.out.println("[AssetManager] Loading manifest: " + path);
		InputStream is = loader.load(path);
		byte[] jsonBytes = is.readAllBytes();
		is.close();
		String json = new String(jsonBytes, StandardCharsets.UTF_8);

		Gson gson = new Gson();
		JsonObject root = gson.fromJson(json, JsonObject.class);
		int format = root.get("format").getAsInt();
		JsonArray sequence = root.getAsJsonArray("sequence");

		System.out.println("[AssetManager] Format version: " + format);
		System.out.println(
			"[AssetManager] Loading " + sequence.size() + " assets..."
		);

		for (JsonElement elem : sequence) {
			JsonObject item = elem.getAsJsonObject();
			String type = item.get("type").getAsString();
			String id = item.get("id").getAsString();
			String description = item.has("description")
				? item.get("description").getAsString()
				: "";

			System.out.println("\n[AssetManager] " + description);

			OperationFactory factory = registry.get(type);
			if (factory == null) {
				throw new RuntimeException("Unknown operation type: " + type);
			}

			AssetOperation op = factory.create();
			configureOperation(op, item);
			op.process(new ContextImpl(id));
		}

		System.out.println(
			"\n[AssetManager] Loading complete. " + assets.size()
			+ " assets loaded."
		);
	}

	private void configureOperation(AssetOperation op, JsonObject item) {
		op.configure(item);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String id) {
		Object asset = assets.get(id);
		if (asset == null) {
			throw new RuntimeException("Asset not found: " + id);
		}
		return (T)asset;
	}

	public boolean has(String id) {
		return assets.containsKey(id);
	}

	public void dispose() {
		for (Object asset : assets.values()) {
			if (asset instanceof AutoCloseable ac) {
				try {
					ac.close();
				} catch (Exception e) {
					System.err.println(
						"[AssetManager] Error disposing asset: "
						+ e.getMessage()
					);
				}
			}
		}
		assets.clear();
	}

	private class ContextImpl implements AssetOperation.Context {
		private final String id;

		ContextImpl(String id) {
			this.id = id;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public AssetLoader loader() {
			return loader;
		}

		@Override
		public AssetOperation.Registry registry() {
			return registry;
		}

		@Override
		public void put(String id, Object asset) {
			assets.put(id, asset);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getInput(String ref) throws Exception {
			Object asset = assets.get(ref);
			if (asset == null) {
				throw new RuntimeException(
					"Referenced asset not found: " + ref
				);
			}
			return (T)asset;
		}
	}

	private static class RegistryImpl implements AssetOperation.Registry {
		private final Map<String, OperationFactory> factories = new HashMap<>();

		@Override
		public void register(String type, OperationFactory factory) {
			factories.put(type, factory);
		}

		@Override
		public OperationFactory get(String type) {
			return factories.get(type);
		}
	}
}

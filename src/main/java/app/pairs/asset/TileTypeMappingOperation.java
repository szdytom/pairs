package app.pairs.asset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.libsdl4j.api.surface.*;

/**
 * Builds a flat {@link TileRegistry} from a {@link CropTilesOperation.CropData}
 * and a 2D string-type mapping.  Filters out null entries — only valid tile
 * surfaces with non-null string IDs are registered.
 */
public class TileTypeMappingOperation implements AssetOperation {
	private String id;
	private String input;
	private String[][] mapping;
	private String mappingId;

	@Override
	public String type() {
		return "tile-type-mapping";
	}

	@Override
	public void configure(JsonObject item) {
		id(item.get("id").getAsString());
		input(item.get("input").getAsString());

		if (item.has("mapping-id")) {
			mappingId(item.get("mapping-id").getAsString());
		} else {
			JsonArray mappingArray = item.getAsJsonArray("mapping");
			int rows = mappingArray.size();
			String[][] mapping = new String[rows][];
			for (int r = 0; r < rows; r++) {
				JsonArray rowArray = mappingArray.get(r).getAsJsonArray();
				int cols = rowArray.size();
				mapping[r] = new String[cols];
				for (int c = 0; c < cols; c++) {
					JsonElement elem = rowArray.get(c);
					mapping[r][c] = elem.isJsonNull()
						? null
						: elem.getAsString();
				}
			}
			mapping(mapping);
		}
	}

	@Override
	public void process(Context ctx) throws Exception {
		String[][] resolved = mapping != null
			? mapping
			: ctx.getInput(mappingId);

		CropTilesOperation.CropData crop = ctx.getInput(input);
		int cols = crop.columns();

		List<SDL_Surface> validSurfaces = new ArrayList<>();
		List<String> validIds = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		for (int r = 0; r < resolved.length; r++) {
			for (int c = 0; c < resolved[r].length; c++) {
				String sid = resolved[r][c];
				if (sid == null || !seen.add(sid))
					continue;
				SDL_Surface surface = crop.surfaces()[r * cols + c];
				if (surface != null) {
					validSurfaces.add(surface);
					validIds.add(sid);
				}
			}
		}

		TileRegistry registry = new TileRegistry(
			crop.tileWidth(), crop.tileHeight(),
			validSurfaces.toArray(new SDL_Surface[0]),
			validIds.toArray(new String[0])
		);

		System.out.println(
			"  Created TileRegistry with " + registry.getTypeCount()
			+ " tiles: " + id
		);
		ctx.put(id, registry);
	}

	public TileTypeMappingOperation id(String id) {
		this.id = id;
		return this;
	}

	public TileTypeMappingOperation input(String input) {
		this.input = input;
		return this;
	}

	public TileTypeMappingOperation mapping(String[][] mapping) {
		this.mapping = mapping;
		return this;
	}

	public TileTypeMappingOperation mappingId(String mappingId) {
		this.mappingId = mappingId;
		return this;
	}
}

package app.pairs.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Assigns a type mapping to a TileRegistry.
 */
public class TileTypeMappingOperation implements AssetOperation {
    private String id;
    private String input;
    private int[] mapping;

    @Override
    public String type() {
        return "tile-type-mapping";
    }

    @Override
    public void configure(JsonObject item) {
        id(item.get("id").getAsString());
        input(item.get("input").getAsString());
        JsonArray mappingArray = item.getAsJsonArray("mapping");
        int[] mapping = new int[mappingArray.size()];
        for (int i = 0; i < mappingArray.size(); i++) {
            mapping[i] = mappingArray.get(i).getAsInt();
        }
        mapping(mapping);
    }

    @Override
    public void process(Context ctx) throws Exception {
        TileRegistry registry = ctx.getInput(input);
        registry.setTypeMapping(mapping);
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

    public TileTypeMappingOperation mapping(int[] mapping) {
        this.mapping = mapping;
        return this;
    }
}
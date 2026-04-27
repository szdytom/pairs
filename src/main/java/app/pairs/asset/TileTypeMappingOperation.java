package app.pairs.asset;

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
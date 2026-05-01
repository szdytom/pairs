package app.pairs.logic;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.map.CustomizedTilemapFactory;
import app.pairs.map.SubsetTilemapFactory;
import app.pairs.map.TileGroupRegistry;
import app.pairs.map.TileSelectionPolicy;
import app.pairs.map.TilemapFactory;
import app.pairs.map.TilemapPreset;
import app.pairs.model.OpLogs;
import app.pairs.model.Tilemap;
import app.pairs.utils.Seed;
import app.pairs.utils.Xoroshiro128PP;

import java.util.ArrayList;
import java.util.List;

/**
 * Single facade exposed to the frontend. Bundles a {@link Tilemap}, an
 * {@link OpLogs} history stack, transition checking and elimination semantics
 * behind one entry point. Callers should treat {@link Operation} entries
 * returned by {@link #getOpLogs()} as opaque history records and only act on
 * them via {@link #undo()}; invoking their methods directly is undefined.
 *
 * <p>
 * Every map-creating factory has two forms: a no-arg form that draws a
 * fresh {@link Seed} from {@link Seed#deviceRandom()}, and a {@code (...,
 * Seed)} form for replay. The seed used is always retrievable via {@link
 * #getSeed()}.
 */
public final class GameState {
	private static final String TILE_REGISTRY = "tiles/typed";
	private static final String TILE_GROUPS = "tile-groups/default";

	private final Tilemap tilemap;
	private final OpLogs opLogs;
	private final Seed seed;

	private GameState(Tilemap tilemap, Seed seed) {
		this.tilemap = tilemap;
		this.opLogs = new OpLogs();
		this.seed = seed;
	}

	/** Seed used for both palette selection and layout generation. */
	public Seed getSeed() {
		return seed;
	}

	// ---- difficulty factories --------------------------------------------

	public static GameState easy() {
		return easy(Seed.deviceRandom());
	}

	public static GameState easy(Seed seed) {
		return fromPreset("tilemap/easy", TileSelectionPolicy.easy(), seed);
	}

	public static GameState hard() {
		return hard(Seed.deviceRandom());
	}

	public static GameState hard(Seed seed) {
		return fromPreset("tilemap/hard", TileSelectionPolicy.hard(), seed);
	}

	public static GameState extreme() {
		return extreme(Seed.deviceRandom());
	}

	public static GameState extreme(Seed seed) {
		return fromPreset(
			"tilemap/extreme", TileSelectionPolicy.extreme(), seed
		);
	}

	/** Custom dimensions and tile-type count, no group constraints. */
	public static GameState customized(int width, int height, int types) {
		return customized(width, height, types, Seed.deviceRandom());
	}

	public static GameState customized(
		int width, int height, int types, Seed seed
	) {
		CustomizedTilemapFactory factory = new CustomizedTilemapFactory(seed)
											   .setWidth(width)
											   .setHeight(height)
											   .setTypes(types);
		return build(factory, seed);
	}

	/**
	 * Custom mode with tile-similarity controls. Honours the slab-inclusion
	 * flag and the {@link TileSelectionPolicy.Spread} strategy when picking
	 * the tile palette from the global registry.
	 */
	public static GameState customized(
		int width, int height, int types, boolean includeSlabs,
		TileSelectionPolicy.Spread spread
	) {
		return customized(
			width, height, types, includeSlabs, spread, Seed.deviceRandom()
		);
	}

	public static GameState customized(
		int width, int height, int types, boolean includeSlabs,
		TileSelectionPolicy.Spread spread, Seed seed
	) {
		TileSelectionPolicy policy = new TileSelectionPolicy(
			includeSlabs, spread
		);
		int[] subset = pickSubset(policy, types, seed);
		TilemapFactory inner = new CustomizedTilemapFactory(seed)
								   .setWidth(width)
								   .setHeight(height)
								   .setTypes(types);
		return build(new SubsetTilemapFactory(inner, subset), seed);
	}

	private static GameState fromPreset(
		String presetId, TileSelectionPolicy policy, Seed seed
	) {
		TilemapPreset preset = AssetManager.instance().get(presetId);
		int[] subset = pickSubset(policy, preset.types(), seed);
		TilemapFactory inner = CustomizedTilemapFactory.fromPreset(
			preset, seed
		);
		return build(new SubsetTilemapFactory(inner, subset), seed);
	}

	private static int[] pickSubset(
		TileSelectionPolicy policy, int count, Seed seed
	) {
		TileRegistry registry = AssetManager.instance().get(TILE_REGISTRY);
		TileGroupRegistry groups = AssetManager.instance().get(TILE_GROUPS);
		return policy.selectFor(
			registry, groups, count, new Xoroshiro128PP(seed)
		);
	}

	private static GameState build(TilemapFactory factory, Seed seed) {
		return new GameState(factory.generate(), seed);
	}

	// ---- read-only map accessors -----------------------------------------

	public int getWidth() {
		return tilemap.getWidth();
	}

	public int getHeight() {
		return tilemap.getHeight();
	}

	public int getTile(int row, int col) {
		return tilemap.getTile(row, col);
	}

	// ---- core gameplay API -----------------------------------------------

	/**
	 * Check whether the two tiles can be eliminated (same id and a clear path
	 * exists between them). Performs no state mutation. Returns {@code false}
	 * for any selection that touches an empty cell or the same cell twice.
	 * Out-of-range coordinates throw {@link IllegalStateException} since the
	 * frontend is expected to only pass valid grid positions.
	 */
	public boolean canEliminate(int row1, int col1, int row2, int col2) {
		requireInBounds(row1, col1);
		requireInBounds(row2, col2);
		if (row1 == row2 && col1 == col2) {
			return false;
		}
		if (tilemap.getTile(row1, col1) <= 0
		    || tilemap.getTile(row2, col2) <= 0) {
			return false;
		}
		return TileTransition.transition(tilemap, row1, col1, row2, col2);
	}

	private void requireInBounds(int row, int col) {
		if (row < 0 || row >= tilemap.getHeight() || col < 0
		    || col >= tilemap.getWidth()) {
			throw new IllegalStateException(
				"coordinate out of range: (" + row + "," + col + ") on "
				+ tilemap.getHeight() + "x" + tilemap.getWidth() + " map"
			);
		}
	}

	/**
	 * Eliminate the pair. The move must be legal (see
	 * {@link #canEliminate}); on success the operation is applied and pushed
	 * onto the history stack. Throws {@link IllegalStateException} if the
	 * move is illegal — callers should gate on {@link #canEliminate} first.
	 */
	public void operate(int row1, int col1, int row2, int col2) {
		if (!canEliminate(row1, col1, row2, col2)) {
			throw new IllegalStateException(
				"illegal elimination: (" + row1 + "," + col1 + ") -> (" + row2
				+ "," + col2 + ")"
			);
		}
		new OpElimination(tilemap, row1, col1, row2, col2, opLogs::push)
			.operate();
	}

	/**
	 * Undo the most recent recorded operation. Throws
	 * {@link IllegalStateException} if the history is empty.
	 */
	public void undo() {
		if (!Operation.undoFrom(opLogs::pop)) {
			throw new IllegalStateException("no operation to undo");
		}
	}

	/** Returns the operation history in chronological order (oldest first). */
	public List<Operation> getOpLogs() {
		return opLogs.history();
	}

	/** Check whether the game is in a stalled state (no more valid moves). */
	public boolean isStall() {
		return IsStall.isStall(tilemap);
	}

	public ArrayList<Integer> path(
		int startRow, int startCol, int targetRow, int targetCol
	) {
		requireInBounds(startRow, startCol);
		requireInBounds(targetRow, targetCol);
		return Path.path(tilemap, startRow, startCol, targetRow, targetCol);
	}

	/** Check whether all tiles have been eliminated. */
	public boolean isCleared() {
		for (int r = 0; r < tilemap.getHeight(); r++) {
			for (int c = 0; c < tilemap.getWidth(); c++) {
				if (tilemap.getTile(r, c) > 0) {
					return false;
				}
			}
		}
		return true;
	}
}

package app.pairs.logic;

import app.pairs.map.CustomizedTilemapFactory;
import app.pairs.map.EasyTilemapFactory;
import app.pairs.map.HardTilemapFactory;
import app.pairs.map.TilemapFactory;
import app.pairs.model.OpLogs;
import app.pairs.model.Tilemap;

import java.util.List;

/**
 * Single facade exposed to the frontend. Bundles a {@link Tilemap}, an
 * {@link OpLogs} history stack, transition checking and elimination semantics
 * so callers do not need to import any of the internal types.
 */
public final class GameState {
	private final Tilemap tilemap;
	private final OpLogs opLogs;

	private GameState(Tilemap tilemap) {
		this.tilemap = tilemap;
		this.opLogs = new OpLogs();
	}

	/** Create a game state for the easy mode preset. */
	public static GameState easy() {
		return fromFactory(new EasyTilemapFactory());
	}

	/** Create a game state for the hard mode preset. */
	public static GameState hard() {
		return fromFactory(new HardTilemapFactory());
	}

	/** Create a game state with custom dimensions and tile-type count. */
	public static GameState customized(int width, int height, int types) {
		CustomizedTilemapFactory factory = new CustomizedTilemapFactory()
											   .setWidth(width)
											   .setHeight(height)
											   .setTypes(types);
		return fromFactory(factory);
	}

	private static GameState fromFactory(TilemapFactory factory) {
		return new GameState(factory.generate());
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
	 * exists between them). Performs no state mutation.
	 */
	public boolean canEliminate(int row1, int col1, int row2, int col2) {
		return TileTransition.transition(tilemap, row1, col1, row2, col2);
	}

	/**
	 * Attempt to eliminate the pair. Returns {@code true} when the move is
	 * legal; the operation is then applied and pushed onto the history stack.
	 * Returns {@code false} (and changes nothing) when the move is illegal.
	 */
	public boolean operate(int row1, int col1, int row2, int col2) {
		if (!canEliminate(row1, col1, row2, col2)) {
			return false;
		}
		new OpElimination(tilemap, row1, col1, row2, col2, opLogs::push)
			.operate();
		return true;
	}

	/**
	 * Undo the most recent recorded operation.
	 *
	 * @return {@code true} if an operation was undone, {@code false} if the
	 *         history was empty
	 */
	public boolean undo() {
		return Operation.undoFrom(opLogs::pop);
	}

	/** Returns the operation history in chronological order (oldest first). */
	public List<Operation> getOpLogs() {
		return opLogs.history();
	}
}

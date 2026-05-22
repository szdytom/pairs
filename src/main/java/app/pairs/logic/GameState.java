package app.pairs.logic;

import app.pairs.asset.AssetManager;
import app.pairs.asset.TileRegistry;
import app.pairs.map.PairingStrategy;
import app.pairs.map.TileSelectionPolicy;
import app.pairs.map.TilemapFactory;
import app.pairs.model.GameSnapshot;
import app.pairs.model.GameStatus;
import app.pairs.model.OpLogs;
import app.pairs.model.OperationSnapshot;
import app.pairs.model.Tilemap;
import app.pairs.solver.Solver;
import app.pairs.solver.SolverResult;
import app.pairs.utils.Seed;

import java.util.ArrayList;
import java.util.List;

/**
 * Single facade exposed to the frontend. Bundles a {@link Tilemap}, an
 * {@link OpLogs} history stack, transition checking and elimination semantics
 * behind one entry point. Callers should treat the entries returned by
 * {@link #getOpLogs()} as opaque history records and only act on them via
 * {@link #undo()}; invoking their methods directly is undefined.
 *
 * <p>
 * The canonical way to build a {@code GameState} is to pass a
 * {@link TilemapFactory} (e.g. {@code new GameState(TilemapFactory.fromPreset(
 * "tilemap/hard"))}). The static {@code easy()/hard()/
 * extreme()/customized(...)} helpers are thin shorthands for the common
 * cases and ultimately funnel through the same constructor. Every helper
 * has a no-arg form that draws a fresh {@link Seed} from
 * {@link Seed#deviceRandom()} and a {@code (..., Seed)} form for replay.
 */
public final class GameState {
	private static final String TILE_REGISTRY = "tiles/typed";

	private final TilemapFactory factory;
	private Tilemap tilemap;
	private final OpLogs opLogs;
	public final GameStatus
		gameStatus; // change score by directly mutating this object, not by
	                // pushing operations
	private int undoBarrier;
	private int remainingTiles;
	private Boolean stallResult;

	public enum OpKind { MANUAL, AUTO }

	public GameState(TilemapFactory factory) {
		this(factory, factory.generate(), new OpLogs(), new GameStatus(), 0);
	}

	private GameState(
		TilemapFactory factory, Tilemap tilemap, OpLogs opLogs,
		GameStatus gameStatus, int undoBarrier
	) {
		this.factory = factory;
		this.tilemap = tilemap;
		this.opLogs = opLogs;
		this.gameStatus = gameStatus;
		this.undoBarrier = undoBarrier;
		this.remainingTiles = countTiles();
		this.stallResult = null;
	}

	/**
	 * Regenerate the map from the same factory (same seed) and reset history.
	 */
	public void restart() {
		this.tilemap = factory.generate();
		this.opLogs.clear();
		this.gameStatus.reset();
		this.undoBarrier = 0;
		this.remainingTiles = countTiles();
		this.stallResult = null;
	}

	/** Create from a preset asset ID with a fresh random seed. */
	public static GameState fromPreset(String presetId) {
		return fromPreset(presetId, Seed.deviceRandom());
	}

	/**
	 * Create from a preset asset ID with a specific seed for deterministic
	 * replay.
	 */
	public static GameState fromPreset(String presetId, Seed seed) {
		TilemapFactory factory = TilemapFactory.fromPreset(presetId, seed);
		return new GameState(factory);
	}

	/** Custom dimensions and tile-type count, no group constraints. */
	public static GameState customized(int width, int height, int types) {
		return customized(width, height, types, Seed.deviceRandom());
	}

	public static GameState customized(
		int width, int height, int types, Seed seed
	) {
		return new GameState(
			TilemapFactory.customized(width, height, types, seed)
		);
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
		return new GameState(TilemapFactory.customized(
			width, height, types, includeSlabs, spread, PairingStrategy.BASE,
			seed, Tilemap.Difficulty.NORMAL
		));
	}

	public static GameState fromSnapshot(GameSnapshot snapshot) {
		Tilemap tilemap = Tilemap.fromSnapshot(snapshot.tilemap());
		OpLogs opLogs = new OpLogs();
		GameStatus gameStatus = GameStatus.fromSnapshot(snapshot.status());
		TilemapFactory factory = snapshot.factory() == null
			? TilemapFactory.fixed(snapshot.tilemap())
			: TilemapFactory.fromSnapshot(snapshot.factory());

		GameState state = new GameState(
			factory, tilemap, opLogs, gameStatus, snapshot.undoBarrier()
		);
		for (OperationSnapshot op : snapshot.operations()) {
			opLogs.push(OpElimination.restored(op));
		}
		return state;
	}

	/**
	 * Snapshot the current game state with component-owned state fragments.
	 */
	public GameSnapshot toSnapshot() {
		return new GameSnapshot(
			tilemap.toSnapshot(), factory.toSnapshot(), gameStatus.toSnapshot(),
			opLogs.snapshots(), undoBarrier
		);
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

	public Tilemap getTilemap() {
		return tilemap;
	}

	public OpLogs getOpLogsModel() {
		return opLogs;
	}

	public GameStatus getGameStatus() {
		return gameStatus;
	}

	public String getTileString(int row, int col) {
		int id = tilemap.getTile(row, col);
		if (id <= 0) {
			return null;
		}
		TileRegistry reg = AssetManager.instance().get(TILE_REGISTRY);
		return reg.getStringId(id);
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
		int t1 = tilemap.getTile(row1, col1);
		int t2 = tilemap.getTile(row2, col2);
		if (t1 <= 0 || t2 <= 0) {
			return false;
		}
		if (t1 != t2) {
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
	public void eliminate(
		int row1, int col1, int row2, int col2, int time, OpKind kind
	) {
		if (!canEliminate(row1, col1, row2, col2)) {
			throw new IllegalStateException(
				"illegal elimination: (" + row1 + "," + col1 + ") -> (" + row2
				+ "," + col2 + ")"
			);
		}
		int tileId = tilemap.getTile(row1, col1);
		var path = Path.path(tilemap, row1, col1, row2, col2);
		int deltaScore;
		int oldCombo;
		if (kind == OpKind.AUTO) {
			deltaScore = 0;
			oldCombo = gameStatus.combo;
		} else {
			deltaScore = switch (tilemap.getDifficulty()) {
				case EASY -> OpElimination.SCORE_PER_PAIR;
				case HARD -> OpElimination.SCORE_PER_PAIR * 2;
				case EXTREME -> OpElimination.SCORE_PER_PAIR * 3;
				default -> OpElimination.SCORE_PER_PAIR;
			};
			deltaScore *= Math.max(1, 5 - time / 1_000);
			oldCombo = gameStatus.combo;
			if (time < 1_000) {
				gameStatus.combo++;
			} else {
				gameStatus.combo = 1;
			}
			deltaScore *= gameStatus.combo * (gameStatus.combo + 1) / 2;
		}
		tilemap.setTile(row1, col1, 0);
		tilemap.setTile(row2, col2, 0);
		remainingTiles -= 2;
		stallResult = null;
		gameStatus.changeScore(deltaScore);
		opLogs.push(new OpElimination(
			tileId, row1, col1, row2, col2, time, path, deltaScore, oldCombo
		));
	}

	/**
	 * Undo the most recent recorded operation. Throws
	 * {@link IllegalStateException} if the history is empty.
	 */
	public void undo() {
		if (opLogs.size() <= undoBarrier) {
			throw new IllegalStateException("cannot undo past barrier");
		}
		OpElimination op = opLogs.pop();
		tilemap.setTile(op.getRow1(), op.getCol1(), op.getTileId());
		tilemap.setTile(op.getRow2(), op.getCol2(), op.getTileId());
		remainingTiles += 2;
		stallResult = null;
		gameStatus.changeScore(-op.getDeltaScore());
		gameStatus.combo = op.getComboBefore();
	}

	public void undoTo(int id) {
		while (opLogs.size() > Math.max(id, undoBarrier)) {
			undo();
		}
	}

	private void setUndoBarrier() {
		undoBarrier = opLogs.size();
	}

	/** Returns the operation history in chronological order (oldest first). */
	public List<OpElimination> getOpLogs() {
		return opLogs.history();
	}

	/**
	 * Returns {@code true} if an undo operation is available (not past
	 * barrier).
	 */
	public boolean canUndo() {
		return opLogs.size() > undoBarrier;
	}

	/** Returns the number of eliminated pairs (cheap, no allocation). */
	public int getOpLogCount() {
		return opLogs.size();
	}

	/** Returns the number of remaining pairs on the board. */
	public int remainingPairs() {
		return remainingTiles / 2;
	}

	private int countTiles() {
		int count = 0;
		for (int r = 0; r < tilemap.getHeight(); r++) {
			for (int c = 0; c < tilemap.getWidth(); c++) {
				if (tilemap.getTile(r, c) > 0) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Collect all positions of a given tile type and set the undo barrier.
	 * Does NOT mutate the tilemap — the caller is responsible for animating
	 * and then calling {@link #clearTile} per position.
	 */
	public List<Tilemap.TilePos> eliminateType(int tileId) {
		List<Tilemap.TilePos> positions = new ArrayList<>();
		for (int r = 0; r < tilemap.getHeight(); r++) {
			for (int c = 0; c < tilemap.getWidth(); c++) {
				if (tilemap.getTile(r, c) == tileId) {
					positions.add(new Tilemap.TilePos(r, c));
				}
			}
		}
		if (positions.isEmpty()) {
			throw new IllegalStateException(
				"no tiles of type " + tileId + " to eliminate"
			);
		}
		setUndoBarrier();
		return positions;
	}

	/** Clear a single tile cell (used by TNT animation). */
	public void clearTile(int row, int col) {
		tilemap.setTile(row, col, 0);
		remainingTiles--;
		stallResult = null;
	}

	/** Score per tile for TNT: minimum base score / 2 (no time bonus). */
	public int tntScorePerTile() {
		int base = switch (tilemap.getDifficulty()) {
			case EASY -> OpElimination.SCORE_PER_PAIR;
			case HARD -> OpElimination.SCORE_PER_PAIR * 2;
			case EXTREME -> OpElimination.SCORE_PER_PAIR * 3;
			default -> OpElimination.SCORE_PER_PAIR;
		};
		return base / 2;
	}

	/** Check whether the game is in a stalled state (no more valid moves). */
	public boolean isStall() {
		if (stallResult == null) {
			stallResult = IsStall.isStall(tilemap);
		}
		return stallResult;
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
		return remainingTiles == 0;
	}

	public SolverResult solve() {
		return solve(0); // 0 means no timeout
	}

	public SolverResult solve(long timeoutMs) {
		return Solver.solve(tilemap, timeoutMs);
	}
}

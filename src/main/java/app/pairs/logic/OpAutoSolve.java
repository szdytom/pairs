package app.pairs.logic;
import app.pairs.model.GameStatus;
import app.pairs.model.Tilemap;
import app.pairs.solver.Move;
import app.pairs.solver.Solver;
import app.pairs.solver.SolverResult;

import java.util.function.Consumer;

public class OpAutoSolve {
	private final Tilemap tilemap;
	private final Consumer<Operation> pushFn;
	private final SolverResult opEls;
	private boolean executed;
	private final GameStatus gameStatus;
	public OpAutoSolve(
		GameStatus gameStatus, Tilemap tilemap, Consumer<Operation> pushFn
	) {
		this.gameStatus = gameStatus;
		this.tilemap = tilemap;
		this.pushFn = pushFn;
		opEls = Solver.solve(tilemap);
	}
	public void operate() {
		if (executed) {
			throw new IllegalStateException(
				"OpAutoSolve already executed; replaying history entries is"
				+ " not allowed"
			);
		}
		for (Move v : opEls.moves()) {
			int r1 = v.r1();
			int c1 = v.c1();
			int r2 = v.r2();
			int c2 = v.c2();
			OpElimination op = new OpElimination(
				gameStatus, tilemap, r1, c1, r2, c2, 0, pushFn, true
			);
			op.operate();
		}
		executed = true;
	}
}

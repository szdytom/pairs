package app.pairs.logic;

import java.util.function.Supplier;

/**
 * Represents a reversible game action. Each implementation owns the data it
 * needs to {@link #operate()} and to {@link #undo()} that operation later.
 */
public interface Operation {
	/** Apply this operation to the game state. */
	void operate();

	/** Revert the effects of this operation, restoring the previous state. */
	void undo();

	/**
	 * Pop the most recent operation from the supplied source and undo it.
	 *
	 * @param popFn a supplier that returns the last operation, or {@code null}
	 *              if the log is empty (e.g. a stack's poll method)
	 * @return true if an operation was undone, false if the log was empty
	 */
	static boolean undoFrom(Supplier<Operation> popFn) {
		Operation op = popFn.get();
		if (op == null) {
			return false;
		}
		op.undo();
		return true;
	}
}

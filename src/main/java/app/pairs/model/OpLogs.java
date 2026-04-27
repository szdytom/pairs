package app.pairs.model;

import app.pairs.logic.Operation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A pure stack data structure that holds {@link Operation}s.
 *
 * <p>
 * Pair {@link #push(Operation) push} with {@link Operation#operate()} to record
 * an operation, and use
 * {@link Operation#undoFrom(java.util.function.Supplier)
 * Operation.undoFrom(log::pop)} to undo the most recent one.
 */
public class OpLogs {
    private final Deque<Operation> stack = new ArrayDeque<>();

    /** Push an operation onto the log (does NOT execute it). */
    public void push(Operation op) {
        stack.push(op);
    }

    /**
     * Remove and return the most recent operation.
     *
     * @return the operation, or {@code null} if the log is empty
     */
    public Operation pop() {
        return stack.poll();
    }

    /** Peek at the most recent operation without removing it. */
    public Operation peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        stack.clear();
    }

    /** Returns all operations in chronological order (oldest first). */
    public List<Operation> history() {
        List<Operation> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return list;
    }
}

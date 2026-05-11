package app.pairs.model;

import app.pairs.logic.OpElimination;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class OpLogs {
	private final Deque<OpElimination> stack = new ArrayDeque<>();

	public void push(OpElimination op) {
		stack.push(op);
	}

	public OpElimination pop() {
		return stack.poll();
	}

	public OpElimination peek() {
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

	public List<OpElimination> history() {
		List<OpElimination> list = new ArrayList<>(stack);
		Collections.reverse(list);
		return list;
	}
}

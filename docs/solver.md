# Solver

`Solver.solve(map)` finds the elimination sequence that clears as many pairs
as possible. `Solver.solve(map, timeoutMillis)` returns the best result found
within the budget (anytime).

## Algorithm

DFS with backtracking over elimination sequences. At each node:

1. **Forced moves first** — if a tile type has exactly two instances and they
   are connectable, that move is forced; apply it immediately without
   branching. Repeat until no forced moves remain.
2. **Transposition table** — board states are hashed with Zobrist hashing
   (each cell XORed with a fixed random key). If the current hash is already
   in the table, return the cached result instead of re-searching.
3. **Most-constrained-first ordering** — generate moves grouped by tile type,
   sorted by group size ascending. Smaller groups (fewer options) are tried
   first, failing fast on tight constraints.

State is kept incrementally: `apply`/`undo` XOR two Zobrist keys and adjust
`remainingTiles` in O(1); no full board scan during DFS.

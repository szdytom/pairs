package app.pairs.solver;

/** A single elimination action: pair two same-typed tiles. */
public record Move(int r1, int c1, int r2, int c2) {}

package app.pairs.model;

import java.util.Map;

public record
	GameStatusSnapshot(int score, int combo, Map<String, Integer> items) {}

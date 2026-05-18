package app.pairs.save;

import app.pairs.model.GameType;

public record SaveEntry(long id, long updatedAt, GameType type) {}

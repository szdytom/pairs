package app.pairs.save;

import app.pairs.model.TilemapType;

public record SaveEntry(long id, long updatedAt, TilemapType type) {}

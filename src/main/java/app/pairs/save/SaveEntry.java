package app.pairs.save;

import app.pairs.model.Tilemap;

public record SaveEntry(long id, long updatedAt, Tilemap.Difficulty type) {}

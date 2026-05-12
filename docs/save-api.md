# Save System — UI API Reference

## Entry point

```java
Save save = Database.instance().saves();
```

`Database` is a singleton. The first call opens (or creates) the local SQLite file automatically.

## Save

```java
long id = save.save(state.getTilemap(), state.getOpLogsModel(), state.getGameStatus());
```

Persists the current game state. Returns the new save's `id`.

## List

```java
List<SaveEntry> entries = save.list();
```

Returns all saves, newest-played first. Each entry exposes:

| Field | Type | Description |
|---|---|---|
| `id()` | `long` | Unique identifier |
| `type()` | `Tilemap.Difficulty` | Difficulty of the saved game |
| `createdAt()` | `long` | Creation time (Unix ms) |
| `updatedAt()` | `long` | Last overwrite time (Unix ms) |

## Load

```java
GameState restored = GameState.fromSnapshot(save.load(id));
```

Restores a fully playable game — same board, score, and undo history.

## Delete

```java
save.delete(id);
```

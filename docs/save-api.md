# Save System — UI API Reference

## Entry point

All save operations go through the current `User`. The view never calls
`Database` directly.

```java
User user = /* the logged-in RealUser or a NullUser for guests */;
```

`NullUser` silently ignores writes and returns empty results, so the view
never needs to branch on login state.

## Save

```java
long id = user.saveGame(
    state.getTilemap(), state.getOpLogsModel(), state.getGameStatus());
```

Persists the current game state under the current user. Returns the new
save's `id`, or `-1` if the user is a guest.

## List

```java
List<SaveEntry> entries = user.listSaves();
```

Returns the user's saves, newest first. Returns an empty list for guests.
Each entry exposes:

| Field | Type | Description |
|---|---|---|
| `id()` | `long` | Unique identifier |
| `type()` | `Tilemap.Difficulty` | Difficulty of the saved game |
| `updatedAt()` | `long` | Last save time (Unix ms) |

## Load

```java
Optional<GameSnapshot> snapshot = user.loadSave(id);
snapshot.ifPresent(s -> GameState.fromSnapshot(s));
```

Restores a fully playable game — same board, score, and undo history.
Returns `Optional.empty()` for guests. Throws `IllegalStateException` if the
save does not exist or does not belong to this user (fail-fast; callers should
only pass IDs obtained from `listSaves()`).

## Delete

```java
user.deleteSave(id);
```

No-op for guests.

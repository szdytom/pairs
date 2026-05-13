# Save System

This document describes the save/load mechanism: what data is persisted, how the database is structured, and what interfaces are available to the frontend.

## What needs to be saved

To restore a game session completely, three things must be captured:

1. **The current board state** — the full 2D tile grid plus the difficulty level.
2. **The score** — a single integer maintained in `GameStatus`.
3. **The full undo history** — every `OpElimination` that has been executed, in chronological order. Without this, `undo()` would stop working after a load.

A seed-based replay approach was rejected because it would couple the save format to the map generator and break as soon as `restart()` is called. Snapshotting the actual tile grid is simpler and future-proof.

## Database

SQLite is used as the database. `Database` is the single entry point; it owns the connection and creates all tables on first open.

### Tables

```sql
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE saves (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER REFERENCES users(id),
    updated_at INTEGER NOT NULL,
    type       TEXT NOT NULL,
    json_data  TEXT NOT NULL
);

CREATE TABLE scores (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id),
    difficulty TEXT NOT NULL,
    score      INTEGER NOT NULL,
    played_at  INTEGER NOT NULL
);
```

All three tables are active. In production, save rows are always user-scoped (`user_id` is set). `user_id` is nullable in the schema to support unscoped saves used by tests via `Database.saves()`; guest sessions (`NullUser`) do not write to the database at all.

### Storage path

`SavePath.get()` returns the platform-appropriate path for the database file:

| OS | Path |
|---|---|
| macOS | `~/Library/Application Support/pairs/save.db` |
| Windows | `%APPDATA%\pairs\save.db` |
| Linux | `$XDG_DATA_HOME/pairs/save.db` or `~/.local/share/pairs/save.db` |

The directory is created automatically if it does not exist.

## Data format

The `json_data` column holds a `GameSnapshot` serialized via Gson:

```json
{
  "difficulty": "HARD",
  "score": 3000,
  "tilemap": [[1,2,0],[0,3,3],[1,2,0]],
  "operations": [
    { "row1":0,"col1":0,"row2":2,"col2":0,"tileId":1,"time":2000,"deltaScore":1500 },
    { "row1":0,"col1":1,"row2":2,"col2":1,"tileId":2,"time":4000,"deltaScore":1000 }
  ]
}
```

`tilemap` is the **current** board state (zeros where tiles were eliminated). `operations` is the complete undo history in chronological order. This pattern keeps the SQL schema stable — adding new fields to a save requires no `ALTER TABLE`, only a change to the Java record.

## Layer boundaries

- **`app.pairs.save`** — knows how to store and retrieve raw data (SQLite + JSON). Unaware of game rules.
- **`app.pairs.model`** — provides the snapshot records (`GameSnapshot`, `OperationSnapshot`) that serve as the data contract between the two sides.
- **`app.pairs.logic.GameState`** — knows how to reconstruct a live game from a `GameSnapshot`.

The save system never imports `GameState` or anything from `logic/` or `view/`.

## How restore works

`OpElimination` normally validates that the tiles it operates on are present on the board. After load, those tiles are already gone, so normal construction would fail.

The solution is `OpElimination.restored(...)`, a package-private factory that:
1. Skips the tile presence check.
2. Sets `executed = true` immediately.
3. Stores the pre-computed `deltaScore` directly.

`GameState.fromSnapshot()` pushes one restored `OpElimination` per entry in `snapshot.operations()` onto the `OpLogs` stack. Calling `undo()` correctly puts tiles back and deducts the score. `restart()` regenerates the board from the snapshot's tile grid.

## Available interfaces

### `User` (`app.pairs.user.User`)

The view layer always operates through a `User` instance. `RealUser` routes
calls to the database; `NullUser` (guest mode) is a silent no-op.

```java
long saveGame(Tilemap tilemap, OpLogs opLogs, GameStatus gameStatus)
```
Serializes the current game state and inserts a new row owned by this user.
Returns the auto-generated row `id`, or `-1` for guests.

---

```java
List<SaveEntry> listSaves()
```
Returns the user's saves ordered by `updatedAt` descending (newest first).
Returns an empty list for guests.

---

```java
Optional<GameSnapshot> loadSave(long id)
```
Deserializes the row with the given `id` that belongs to this user. Returns
`Optional.empty()` for guests. Throws `IllegalStateException` if the save does
not exist or does not belong to this user. Pass the result to
`GameState.fromSnapshot()` to get a playable game.

---

```java
void deleteSave(long id)
```
Deletes the row with the given `id` owned by this user. No-op for guests.

---

### `Database` (`app.pairs.save.Database`)

Not accessed directly by the view. The package-private `Database(String path)`
constructor and `saves()`/`saves(String username)` methods are used internally
by `RealUser` and in tests.

---

### `GameState.fromSnapshot` (`app.pairs.logic.GameState`)

```java
static GameState fromSnapshot(GameSnapshot snapshot)
```
Reconstructs a fully playable `GameState` with the same board, score, and undo
history as when the save was taken. `restart()` regenerates the board from the
snapshot's tile grid.

---

### Data records (`app.pairs.model`)

```java
record GameSnapshot(Difficulty difficulty, int score, int combo, int[][] tilemap, List<OperationSnapshot> operations)
record OperationSnapshot(int row1, int col1, int row2, int col2, int tileId, long time, int deltaScore, List<Integer> path, int comboBefore)
record SaveEntry(long id, long updatedAt, Tilemap.Difficulty type)
```

## Typical usage

```java
User user = /* RealUser or NullUser */;

// Save
long id = user.saveGame(
    state.getTilemap(), state.getOpLogsModel(), state.getGameStatus());

// List (for UI menu)
List<SaveEntry> entries = user.listSaves();

// Load
user.loadSave(entries.get(0).id())
    .map(GameState::fromSnapshot)
    .ifPresent(restored -> /* switch to restored game */);

// Delete
user.deleteSave(id);
```

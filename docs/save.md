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
    user_id    INTEGER REFERENCES users(id),  -- NULL until user system is wired in
    created_at INTEGER NOT NULL,
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

`users` and `scores` are created but not yet exposed — the user/leaderboard system is planned for a future milestone. Only `saves` is currently accessible.

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

### `Database` (`app.pairs.save.Database`)

```java
Database.instance()      // returns the singleton; opens db on first call
Save saves()             // returns the save accessor for this connection
```

The package-private `Database(String path)` constructor is reserved for tests.

---

### `Save` (`app.pairs.save.Save`)

```java
long save(Tilemap tilemap, OpLogs opLogs, GameStatus gameStatus)
```
Serializes the current game state and inserts a new row. Returns the auto-generated row `id`.

---

```java
List<SaveEntry> list()
```
Returns all saves ordered by `created_at` descending (newest first). Each `SaveEntry` carries `id` and `createdAt` (Unix epoch milliseconds).

---

```java
GameSnapshot load(long id)
```
Deserializes the row with the given `id`. Throws `IllegalStateException` if not found. Pass the result to `GameState.fromSnapshot()` to get a playable game.

---

```java
void delete(long id)
```
Deletes the row with the given `id`.

---

### `GameState.fromSnapshot` (`app.pairs.logic.GameState`)

```java
static GameState fromSnapshot(GameSnapshot snapshot)
```
Reconstructs a fully playable `GameState` with the same board, score, and undo history as when the save was taken. `restart()` regenerates the board from the snapshot's tile grid.

---

### Data records (`app.pairs.model`)

```java
record GameSnapshot(Difficulty difficulty, int score, int[][] tilemap, List<OperationSnapshot> operations)
record OperationSnapshot(int row1, int col1, int row2, int col2, int tileId, int time, int deltaScore)
record SaveEntry(long id, long createdAt)
```

## Typical usage

```java
Save save = Database.instance().saves();

// Save
long id = save.save(state.getTilemap(), state.getOpLogsModel(), state.getGameStatus());

// List (for UI menu)
List<SaveEntry> entries = save.list();

// Load
GameSnapshot snapshot = save.load(entries.get(0).id());
GameState restored = GameState.fromSnapshot(snapshot);

// Delete
save.delete(id);
```

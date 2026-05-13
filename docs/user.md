# User System

## Why

The view needs to do things that only make sense for a logged-in player
(save a game, record a score) and things that are always valid (play a game).
Rather than scattering `if (loggedIn)` checks everywhere, the codebase uses
the Null Object pattern: every view component holds a `User` and calls it
unconditionally.

## What

`User` is an interface with two responsibilities:

- **Score** — `save(GameState)` records the final score for the current game.
- **Save slots** — `saveGame`, `listSaves`, `loadSave`, `deleteSave` let the
  player persist and restore in-progress games (see [save-api.md](save-api.md)).

`RealUser` implements both by delegating to the database under the logged-in
username. `NullUser` (guest mode) is a silent no-op for writes and returns
empty for reads — the view never needs to check.

## Getting a User

```java
// Authenticated (login or auto-register on first run)
Optional<RealUser> user = UserManager.loginOrRegister(username, password);

// Guest
User user = new NullUser();
```

`loginOrRegister` returns `Optional.empty()` only when the username exists but
the password is wrong. Any other outcome (new account, existing account with
correct password) returns a `RealUser`.

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
// Login
Optional<RealUser> user = UserManager.login(username, password);

// Register
Optional<RealUser> user = UserManager.register(username, password);

// Change password
boolean ok = UserManager.changePassword(username, oldPassword, newPassword);

// Guest
User user = new NullUser();
```

- `login` returns `Optional.empty()` if the user doesn't exist or the password
  is wrong.
- `register` returns `Optional.empty()` if the username already exists.
- `changePassword` returns `false` if the old password doesn't match.

# AGENTS.md

## Common Commands

### Build & Run

- `./gradlew build`: Build the entire project
- `./gradlew run`: Run the main application
- `./gradlew test`: Run unit tests
- `./gradlew clean`: Clean build artifacts

On Windows, use `gradlew.bat` instead of `./gradlew` for all commands above.

### Scripts

- `./scripts/format-j.sh`: Format all Java source files (Run before committing code)

Application entry point: `app.pairs.Main` (configured in `build.gradle`)

## Code Style

- Use `./scripts/format-j.sh` to format Java code according to project standards.

- Use English for all comments in code.
- Write comments using Markdown syntax, even though they are not rendered. For example, "// `object` is borrowed", not "// object is borrowed".
- Keep comments concise: let the code explain itself if possible. Comment "what" and "why", not "how". Avoid redundant comments that restate the code.

- Use wildcard imports for libsdl4j JNI bindings as much as possible, to reduce clutter.
- Follow standard Java naming conventions for classes, methods, and variables.
- Split long functions into smaller, focused methods to improve readability and maintainability.
- Use concise variable and method names. Don't restate types in names.
- Fail-fast for error handling for unexpected state: prefer throwing IllegalStateException.

## General Instructions

- Always run tests after making changes.
- Consult `docs/libsdl4j.md` for common problems and solutions related to libsdl4j. Document any new issues and their fixes in that file.
- Write tests, not too many, mostly integrated.

# AGENTS.md

This project is a link and cancel game (连连看) implemented in Java using libsdl4j. This note contains instructions for agents contributing to the project. These instructions MUST be STRICTLY followed.

To AI Agents: STOP if you are asked to edit this file.

## Code Style

- Use format-j script to format Java code according to project standards.

- Use English for all comments in code.
- Keep comments concise: let the code explain itself if possible. Comment "what" and "why", not "how". Avoid redundant comments that restate the code.

- Use wildcard imports for libsdl4j JNI bindings as much as possible, to reduce clutter.
- Follow standard Java naming conventions for classes, methods, and variables.
- Split long functions into smaller, focused methods to improve readability and maintainability.
- Use concise variable and method names. Don't restate types in names.
- Fail-fast for error handling for unexpected state: prefer throwing IllegalStateException.

## General Instructions

- Always run tests after making changes.
- Write tests, not too many, mostly integration.
- No error handling for impossible scenarios.
- Simplicity is a virtue: If you write 200 lines and it could be 50, rewrite it.

## Domain-Specific Guidelines Index

For specific domains, READ and only read the relevant documentation:

- When working on UI components, READ and FOLLOW the instructions in [UI Guidelines](docs/ui-guidelines.md).
- When working on isometric projection, consult [Isometric Projection](docs/isometric-projection.md) for its algorithms and internals.
- When working on assets loading and management, consult [Assets Management](docs/assets.md) for best practices and patterns.

## Common Commands

### Build & Run

Gradle is used as the build system for this project.

- `./gradlew build`: Build the entire project
- `./gradlew run`: Run the main application
- `./gradlew test`: Run unit tests
- `./gradlew clean`: Clean build artifacts

On Windows, use `gradlew.bat` instead of `./gradlew` for all commands above.

Application entry point: `app.pairs.Main` (configured in `build.gradle`)

### Scripts

- `./scripts/format-j.sh`: Format all Java source files (Run before committing code)
- `./scripts/format-j.ps1`: PowerShell version of the above script for Windows users

### git

- Keep the commit history linear. 

### audio

- Make sure all audio files are 48KHz, wav. If not, use ffmepg to transform them. 

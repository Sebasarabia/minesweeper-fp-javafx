# BuscaMinas (Immutable Minesweeper)

BuscaMinas is a JavaFX Minesweeper built with a strongly functional core. The
board is immutable: every action returns a new board instance, and the UI reacts
to those updates. The result is easier reasoning about game state, safer undo/
redo hooks, and cleaner UI code.

## Features

- JavaFX desktop UI with keyboard, mouse, trackpad, and zoom support.
- Automatic layout; fit-to-window or manual zoom with panning.
- System-theme awareness: light/dark palette is picked from the OS when the
  window gains focus. Mines change colour for high contrast in either mode.
- Immutable board logic with pure `reveal`, `toggleFlag`, and `chord`
  operations.
- Configurable presets (Beginner / Intermediate / Advanced) plus custom games.
- Safer first click: a safe zone is generated around the first reveal even on
  very dense boards.
- Two-strike safety: the first mine you open is revealed without losing; the
  second mine hit ends the round.
- Clearing safe territory auto-reveals the bordering mines so you can plan the
  next move without toggling flags.

## Requirements

- JDK 17.
- JavaFX SDK matching your JDK version, unless you run through Maven.

## Getting Started

### Clone

```bash
git clone <repo-url>
cd BuscaMinas
```

### Run with Maven (recommended)

The project ships with a `pom.xml` using the JavaFX Maven plugin. After
installing Maven and a JDK:

```bash
mvn javafx:run
```

The plugin resolves the JavaFX modules and launches the `app.Main` application.

### Run manually via `java`

If you prefer to start the application manually, supply the JavaFX SDK on the
module path. Replace `/path/to/javafx-sdk` with the extracted SDK matching your
JDK version.

```bash
JDK_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
JFX_SDK=/path/to/javafx-sdk-17

$JDK_HOME/bin/java \
  --module-path "$JFX_SDK/lib" \
  --add-modules javafx.controls,javafx.fxml \
  -cp target/classes app.Main
```

You can also pass `beginner`, `intermediate`, `advanced`, or `<rows> <cols>
<mines>` as CLI arguments to preselect a difficulty.

### Build without running

```bash
mvn clean compile
```

Compiled classes will be written to `target/classes`.

## Project Structure

```
src/
  app/              -> JavaFX entry point (extends `Application`)
  core/             -> Immutable board, mine placement interface & implementation
  ui/fx/            -> JavaFX UI: window, custom canvas view, themes
pom.xml             -> Maven build definition
```

Key classes:

- `core.GridBoard` – immutable game board implementing `Board`.
- `core.RandomMinePlacer` – mine generator supporting safe-zone trimming for
  dense custom boards.
- `ui.fx.FxMinesweeperView` – canvas-based renderer, handles input and emits
  functional board operations.
- `ui.fx.FxMinesweeperWindow` – orchestrates UI widgets, theme detection, and
  board state.

## Development Notes

- The board is immutable; to add new actions follow the pattern of returning a
  new `Board` from each operation.
- UI interactions communicate via `UnaryOperator<Board>` callbacks so the
  window owns the authoritative board instance.
- JavaFX theme detection uses platform-specific commands (`defaults`, `reg`,
  `gsettings`). If your environment does not support them, the app defaults to
  light mode.
- Scrolling pans the board (inverted axes per the current OS expectations).
  Hold Ctrl/Cmd while scrolling or pinch to zoom.

## License

See [LICENSE](LICENSE).

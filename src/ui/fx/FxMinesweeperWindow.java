package ui.fx;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import core.Board;
import core.GridBoard;
import core.RandomMinePlacer;

import java.util.function.UnaryOperator;

public final class FxMinesweeperWindow {
    private Board board;
    private FxMinesweeperView view;

    // UI bits
    private final Label status = new Label("Ready");
    private final Label minesLeft = new Label("Mines: 000");
    private final ComboBox<String> presets = new ComboBox<>();
    private boolean lastDarkTheme = false;

    public FxMinesweeperWindow(Stage stage, int rows, int cols, int mines) {
        this.board = new GridBoard(rows, cols, mines, new RandomMinePlacer());
        this.view  = new FxMinesweeperView(board, this::applyBoardAction);

        // ----- Top bar -----
        ToolBar bar = new ToolBar();
        presets.getItems().addAll("Beginner (9x9,10)", "Intermediate (16x16,40)", "Advanced (16x30,99)");
        presets.getSelectionModel().select(presetIndex(rows, cols, mines));

        Button newBtn = new Button("New");
        newBtn.setOnAction(e -> {
            switch (presets.getSelectionModel().getSelectedIndex()) {
                case 0 -> newGame(9, 9, 10);
                case 1 -> newGame(16, 16, 40);
                case 2 -> newGame(16, 30, 99);
                default -> newGame(rows, cols, mines);
            }
        });

        Button customBtn = new Button("Custom…");
        customBtn.setOnAction(e -> openCustomDialog(stage));

        // Zoom controls
        ToggleButton fitBtn = new ToggleButton("Fit");
        fitBtn.setSelected(true);
        fitBtn.setOnAction(e -> view.setFitToWindow(fitBtn.isSelected()));

        Button zoomOut = new Button("−");
        Button zoomIn  = new Button("+");
        Button zoom100 = new Button("100%");
        zoomOut.setOnAction(e -> view.zoomOut());
        zoomIn.setOnAction(e -> view.zoomIn());
        zoom100.setOnAction(e -> view.zoomReset());

        // Counters look
        minesLeft.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-background-color: #222; -fx-text-fill: #e53935; -fx-padding: 4 10 4 10; -fx-background-radius: 6;");

        bar.getItems().addAll(
                new Label("Preset:"), presets,
                new Separator(), minesLeft,
                new Separator(),
                zoomOut, zoomIn, zoom100, fitBtn,
                new Separator(),
                newBtn, customBtn
        );

        // ----- Center (view in a resizable pane) -----
        StackPane center = new StackPane(view);
        center.setPadding(new Insets(6));

        // ----- Status bar -----
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6, 10, 6, 10));
        bottom.setStyle("-fx-background-color: -fx-shadow-highlight-color, -fx-outer-border, -fx-inner-border, -fx-body-color;");

        BorderPane root = new BorderPane(center, bar, null, bottom, null);
        Scene scene = new Scene(root, 900, 720);

        // basic accelerators
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+PLUS"), view::zoomIn);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+EQUALS"), view::zoomIn);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+MINUS"), view::zoomOut);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+0"), view::zoomReset);

        stage.setTitle("Minesweeper");
        stage.setScene(scene);
        stage.setMinWidth(380);
        stage.setMinHeight(320);
        stage.show();

        applyTheme(scene, detectSystemDark());
        updateUIState();
        view.requestFocus();

        stage.focusedProperty().addListener((obs, oldV, focused) -> {
            if (focused) refreshTheme(scene);
        });
    }

    private int presetIndex(int r, int c, int m) {
        if (r == 9 && c == 9 && m == 10) return 0;
        if (r == 16 && c == 16 && m == 40) return 1;
        if (r == 16 && c == 30 && m == 99) return 2;
        return 1;
    }

    private void newGame(int rows, int cols, int mines) {
        this.board = new GridBoard(rows, cols, mines, new RandomMinePlacer());
        this.view.setBoard(board);
        presets.getSelectionModel().select(presetIndex(rows, cols, mines));
        view.requestFocus();
        updateUIState();
    }

    private void applyBoardAction(UnaryOperator<Board> op) {
        if (op == null) return;
        Board updated = op.apply(board);
        if (updated == null) return;
        this.board = updated;
        this.view.applyBoard(updated);
        updateUIState();
        view.requestFocus();
    }

    private void openCustomDialog(Stage owner) {
        Dialog<int[]> dlg = new Dialog<>();
        dlg.initOwner(owner);
        dlg.setTitle("Custom Game");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField r = new TextField("16"), c = new TextField("16"), m = new TextField("40");
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Rows:"), r);
        grid.addRow(1, new Label("Cols:"), c);
        grid.addRow(2, new Label("Mines:"), m);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int rr = Integer.parseInt(r.getText().trim());
                    int cc = Integer.parseInt(c.getText().trim());
                    int mm = Integer.parseInt(m.getText().trim());
                    if (rr <= 0 || cc <= 0 || mm < 0 || mm >= rr*cc) return null;
                    return new int[]{rr, cc, mm};
                } catch (NumberFormatException ex) { return null; }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(vals -> newGame(vals[0], vals[1], vals[2]));
    }

    private void updateUIState() {
        int left = Math.max(0, board.totalMines() - board.flaggedCount());
        minesLeft.setText(String.format("Mines: %03d", left));
        if (board.isLost())      status.setText("Game over — click New to play again. (Scroll to pan, pinch or Cmd/Ctrl+Wheel to zoom)");
        else if (board.isWon())  status.setText("You win! (Scroll to pan, pinch or Cmd/Ctrl+Wheel to zoom)");
        else                     status.setText("Left: reveal | Right: flag | Double: chord | Scroll pan | Arrows move | Space reveal | F flag | Enter chord");
    }

    private void refreshTheme(Scene scene) {
        applyTheme(scene, detectSystemDark());
    }

    private void applyTheme(Scene scene, boolean dark) {
        if (scene == null) return;
        if (dark == lastDarkTheme && !scene.getStylesheets().isEmpty()) return;
        scene.getStylesheets().clear();
        // minimal light/dark palette (affects toolbar, status bar, labels)
        String darkCss = """
            .root { -fx-base: #202124; -fx-background: #202124; -fx-text-fill: #ECECEC; }
            .label { -fx-text-fill: #ECECEC; }
            .tool-bar { -fx-background-color: #2A2B2E; }
            .separator *.line { -fx-background-color: #444; }
        """;
        String lightCss = """
            .root { -fx-base: #f6f6f6; }
        """;

        scene.getRoot().setStyle(dark ? "-fx-background-color: #202124;" : "-fx-background-color: #f6f6f6;");
        scene.getStylesheets().add("data:text/css," + (dark ? urlEncode(darkCss) : urlEncode(lightCss)));

        // Update custom board view theme to match
        view.setTheme(dark ? Theme.dark() : Theme.light());
        lastDarkTheme = dark;
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    /**
     * Best-effort OS theme detection (no AWT).
     * macOS: checks 'AppleInterfaceStyle' via 'defaults'.
     * Windows: reads 'AppsUseLightTheme' via 'reg' (0=dark,1=light).
     * Linux: tries 'gsettings' (GNOME) color-scheme prefers-dark.
     * If anything fails, defaults to light.
     */
    private boolean detectSystemDark() {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").start();
                p.waitFor();
                String out = new String(p.getInputStream().readAllBytes());
                return out.trim().equalsIgnoreCase("Dark");
            } else if (os.contains("win")) {
                Process p = new ProcessBuilder("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme").start();
                p.waitFor();
                String out = new String(p.getInputStream().readAllBytes());
                return out.contains("0x0"); // 0 = dark
            } else { // Linux (GNOME)
                Process p = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme").start();
                p.waitFor();
                String out = new String(p.getInputStream().readAllBytes()).toLowerCase();
                return out.contains("dark");
            }
        } catch (Exception ignored) { }
        return false;
    }
}

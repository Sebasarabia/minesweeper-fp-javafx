package ui.fx;

import javafx.scene.paint.Color;

/**
 * Minimal theme palette for the custom-drawn board.
 * Separates drawing colors from logic so we can switch light/dark easily.
 */
final class Theme {
    final Color tileRevealed;
    final Color tileHidden;
    final Color tileBorder;
    final Color hoverOverlay;
    final Color selectionStroke;
    final Color mine;
    final Color flagFill;
    final Color flagPole;

    private final Color[] numberColors = new Color[9]; // 1..8 used

    private Theme(
            Color tileRevealed,
            Color tileHidden,
            Color tileBorder,
            Color hoverOverlay,
            Color selectionStroke,
            Color mine,
            Color flagFill,
            Color flagPole,
            Color[] numbers
    ) {
        this.tileRevealed = tileRevealed;
        this.tileHidden = tileHidden;
        this.tileBorder = tileBorder;
        this.hoverOverlay = hoverOverlay;
        this.selectionStroke = selectionStroke;
        this.mine = mine;
        this.flagFill = flagFill;
        this.flagPole = flagPole;
        System.arraycopy(numbers, 0, numberColors, 0, Math.min(numbers.length, numberColors.length));
    }

    Color numberColor(int n) {
        if (n < 1 || n > 8) return Color.DARKGRAY;
        Color c = numberColors[n];
        return c == null ? Color.DARKGRAY : c;
    }

    static Theme light() {
        return new Theme(
                Color.web("#FFFFFF"), // tileRevealed (board light)
                Color.web("#E5E5E5"), // tileHidden
                Color.web("#B0B0B0"), // tileBorder
                Color.color(0, 0, 0, 0.08), // subtle dark overlay on hover
                Color.web("#1976D2"), // selectionStroke
                Color.web("#000000"), // mine (dark on light)
                Color.web("#D32F2F"), // flag fill
                Color.web("#333333"), // flag pole
                new Color[]{
                        null,
                        Color.web("#1565C0"), // 1
                        Color.web("#2E7D32"), // 2
                        Color.web("#C62828"), // 3
                        Color.web("#4527A0"), // 4
                        Color.web("#6D4C41"), // 5
                        Color.web("#00695C"), // 6
                        Color.web("#212121"), // 7
                        Color.web("#757575")  // 8
                }
        );
    }

    static Theme dark() {
        return new Theme(
                Color.web("#000000"), // tileRevealed (board dark)
                Color.web("#1C1C1C"), // tileHidden
                Color.web("#333333"), // tileBorder
                Color.color(1, 1, 1, 0.15), // light overlay for hover
                Color.web("#90CAF9"), // selectionStroke
                Color.web("#FFFFFF"), // mine (light on dark)
                Color.web("#EF5350"), // flag fill
                Color.web("#FAFAFA"), // flag pole
                new Color[]{
                        null,
                        Color.web("#64B5F6"), // 1
                        Color.web("#81C784"), // 2
                        Color.web("#E57373"), // 3
                        Color.web("#B39DDB"), // 4
                        Color.web("#FFB74D"), // 5
                        Color.web("#4DB6AC"), // 6
                        Color.web("#E0E0E0"), // 7
                        Color.web("#BDBDBD")  // 8
                }
        );
    }
}

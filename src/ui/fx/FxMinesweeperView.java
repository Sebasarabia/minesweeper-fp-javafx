package ui.fx;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import core.Board;
import core.VisibleState;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

final class FxMinesweeperView extends Region {
    private static final double PAD = 8.0;
    private static final double BASE_CELL = 28.0;   // for zoom
    private static final double MIN_CELL  = 14.0;

    private Board board;
    private final Consumer<UnaryOperator<Board>> boardChangeListener;

    // Rendering canvas (we fully own size & paint)
    private final Canvas canvas = new Canvas();

    // Interaction / layout state
    private boolean fitToWindow = true;
    private double zoom = 1.0; // when not fitting, cell = BASE_CELL * zoom
    private double panX = 0.0, panY = 0.0;
    private int hoverR = -1, hoverC = -1;
    private int selR = 0, selC = 0;

    // Theme for custom drawing
    private Theme theme = Theme.light();

    FxMinesweeperView(Board board, Consumer<UnaryOperator<Board>> onBoardChange) {
        this.board = Objects.requireNonNull(board, "board");
        this.boardChangeListener = onBoardChange;

        getChildren().add(canvas);
        setFocusTraversable(true);

        // Mouse
        setOnMousePressed(e -> {
            requestFocus();
            int[] cell = hit(e.getX(), e.getY());
            if (cell == null) return;
            int r = cell[0], c = cell[1];
            selR = r; selC = c;

            if (e.getButton() == MouseButton.SECONDARY) {
                publish(b -> b.toggleFlag(r, c));
            } else if (e.getButton() == MouseButton.PRIMARY) {
                publish(e.getClickCount() >= 2 ? b -> b.chord(r, c) : b -> b.reveal(r, c));
            }
        });

        setOnMouseMoved(e -> {
            int[] cell = hit(e.getX(), e.getY());
            int nr = (cell == null ? -1 : cell[0]);
            int nc = (cell == null ? -1 : cell[1]);
            if (nr != hoverR || nc != hoverC) {
                hoverR = nr; hoverC = nc;
                repaint();
            }
        });
        setOnMouseExited(e -> {
            if (hoverR != -1 || hoverC != -1) { hoverR = hoverC = -1; repaint(); }
        });

        // Trackpad/Mouse zoom (Ctrl/Cmd + wheel) + pinch
        addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() || e.isShortcutDown()) {
                if (e.getDeltaY() > 0) zoomIn(); else zoomOut();
            } else {
                panBy(e.getDeltaX(), e.getDeltaY());
            }
            e.consume();
        });
        addEventFilter(ZoomEvent.ZOOM, e -> {
            setZoom(zoom * e.getZoomFactor());
            e.consume();
        });

        // Keyboard
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT  -> { if (selC > 0) selC--; ensureSelectionVisible(); repaint(); }
                case RIGHT -> { if (selC + 1 < board.cols()) selC++; ensureSelectionVisible(); repaint(); }
                case UP    -> { if (selR > 0) selR--; ensureSelectionVisible(); repaint(); }
                case DOWN  -> { if (selR + 1 < board.rows()) selR++; ensureSelectionVisible(); repaint(); }
                case SPACE -> publish(b -> b.reveal(selR, selC));
                case F     -> publish(b -> b.toggleFlag(selR, selC));
                case ENTER -> publish(b -> b.chord(selR, selC));
                case PLUS, EQUALS -> zoomIn();
                case MINUS        -> zoomOut();
                case DIGIT0       -> zoomReset();
            }
            e.consume();
        });

        // Repaint on size changes
        widthProperty().addListener((obs, o, n) -> layoutAndPaint());
        heightProperty().addListener((obs, o, n) -> layoutAndPaint());

        layoutAndPaint();
    }

    /* ------------ API for window ------------ */

    void setBoard(Board b) {
        this.board = Objects.requireNonNull(b, "board");
        this.selR = this.selC = 0;
        this.hoverR = this.hoverC = -1;
        resetViewTransforms();
        layoutAndPaint();
    }

    void applyBoard(Board b) {
        this.board = Objects.requireNonNull(b, "board");
        repaint();
    }

    private void publish(UnaryOperator<Board> op) {
        if (boardChangeListener != null && op != null) boardChangeListener.accept(op);
    }

    void setTheme(Theme t) {
        if (t == null) return;
        this.theme = t;
        repaint();
    }

    boolean isFitToWindow() { return fitToWindow; }

    void setFitToWindow(boolean fit) {
        this.fitToWindow = fit;
        if (fit) {
            resetViewTransforms();
        }
        layoutAndPaint();
    }

    double zoom() { return zoom; }
    void zoomIn()   { setZoom(zoom * 1.15); }
    void zoomOut()  { setZoom(zoom / 1.15); }
    void zoomReset(){
        this.fitToWindow = false;
        this.zoom = 1.0;
        this.panX = this.panY = 0.0;
        clampPan();
        layoutAndPaint();
    }

    void setZoom(double z) {
        this.zoom = Math.max(0.5, Math.min(3.0, z));
        this.fitToWindow = false;
        clampPan();
        layoutAndPaint();
    }

    /* ------------ Layout / metrics ------------ */

    private static final class Metrics {
        final double cell, ox, oy, gridW, gridH;
        Metrics(double cell, double ox, double oy, double gridW, double gridH) {
            this.cell = cell; this.ox = ox; this.oy = oy; this.gridW = gridW; this.gridH = gridH;
        }
    }

    private Metrics metrics() {
        double w = getWidth(), h = getHeight();
        int rows = board.rows(), cols = board.cols();

        double cell;
        if (fitToWindow) {
            cell = Math.max(MIN_CELL, Math.min((w - 2*PAD) / cols, (h - 2*PAD) / rows));
        } else {
            cell = Math.max(MIN_CELL, BASE_CELL * zoom);
        }
        double gridW = cell * cols, gridH = cell * rows;
        double ox;
        double oy;
        if (fitToWindow) {
            ox = Math.max(PAD, (w - gridW) / 2.0);
            oy = Math.max(PAD, (h - gridH) / 2.0);
        } else {
            double minX = Math.min(PAD, w - PAD - gridW);
            double minY = Math.min(PAD, h - PAD - gridH);
            double maxX = PAD;
            double maxY = PAD;
            double baseX = PAD + panX;
            double baseY = PAD + panY;
            ox = clamp(baseX, minX, maxX);
            oy = clamp(baseY, minY, maxY);
        }
        return new Metrics(cell, ox, oy, gridW, gridH);
    }

    private int[] hit(double mx, double my) {
        Metrics m = metrics();
        if (mx < m.ox || my < m.oy || mx >= m.ox + m.gridW || my >= m.oy + m.gridH) return null;
        int c = (int)((mx - m.ox) / m.cell);
        int r = (int)((my - m.oy) / m.cell);
        return board.inBounds(r, c) ? new int[]{r, c} : null;
    }

    /* ------------ Painting ------------ */

    private void layoutAndPaint() {
        // canvas fills Region; ScrollPane outside will add scrollbars if needed
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        repaint();
    }

    void repaint() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Metrics m = metrics();
        int rows = board.rows(), cols = board.cols();

        // scale font by cell
        g.setFont(Font.font(Math.max(12, m.cell * 0.6)));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);

        // tiles
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = m.ox + c * m.cell;
                double y = m.oy + r * m.cell;

                VisibleState state = board.visibleAt(r, c);
                g.setFill(state == VisibleState.REVEALED ? theme.tileRevealed : theme.tileHidden);
                g.fillRect(x, y, m.cell, m.cell);

                // hover highlight
                if (r == hoverR && c == hoverC && state != VisibleState.REVEALED) {
                    g.setFill(theme.hoverOverlay);
                    g.fillRect(x, y, m.cell, m.cell);
                }

                // selection ring
                if (r == selR && c == selC) {
                    g.setStroke(theme.selectionStroke);
                    g.setLineWidth(Math.max(2, m.cell * 0.06));
                    g.strokeRect(x + 1, y + 1, m.cell - 2, m.cell - 2);
                }

                // border
                g.setStroke(theme.tileBorder);
                g.setLineWidth(1);
                g.strokeRect(x, y, m.cell, m.cell);

                // content
                if (state == VisibleState.FLAGGED) {
                    g.setFill(theme.flagFill);
                    g.fillPolygon(new double[]{x + m.cell*0.28, x + m.cell*0.72, x + m.cell*0.28},
                            new double[]{y + m.cell*0.25, y + m.cell*0.50, y + m.cell*0.75}, 3);
                    g.setFill(theme.flagPole);
                    g.fillRect(x + m.cell*0.25, y + m.cell*0.25, Math.max(2, m.cell*0.06), m.cell*0.5);
                } else if (state == VisibleState.REVEALED) {
                    int n = board.adjacentMines(r, c);
                    if (n == -1) {
                        g.setFill(theme.mine);
                        double d = m.cell * 0.55;
                        g.fillOval(x + (m.cell - d)/2, y + (m.cell - d)/2, d, d);
                    } else if (n > 0) {
                        g.setFill(theme.numberColor(n));
                        g.fillText(Integer.toString(n), x + m.cell/2, y + m.cell/2);
                    }
                } else if (board.isLost() && board.isMine(r, c)) {
                    g.setFill(theme.mine);
                    double d = m.cell * 0.55;
                    g.fillOval(x + (m.cell - d)/2, y + (m.cell - d)/2, d, d);
                }
            }
        }
    }

    private void panBy(double dx, double dy) {
        if (fitToWindow) {
            double currentCell = metrics().cell;
            zoom = Math.max(0.5, Math.min(3.0, currentCell / BASE_CELL));
            fitToWindow = false;
        }
        panX += dx;
        panY += dy;
        clampPan();
        repaint();
    }

    private void clampPan() {
        double cell = Math.max(MIN_CELL, BASE_CELL * zoom);
        double gridW = board.cols() * cell;
        double gridH = board.rows() * cell;
        double w = Math.max(getWidth(), 1);
        double h = Math.max(getHeight(), 1);
        double minX = Math.min(PAD, w - PAD - gridW);
        double minY = Math.min(PAD, h - PAD - gridH);
        double maxX = PAD;
        double maxY = PAD;
        panX = clamp(PAD + panX, minX, maxX) - PAD;
        panY = clamp(PAD + panY, minY, maxY) - PAD;
    }

    private void ensureSelectionVisible() {
        if (fitToWindow) return;
        Metrics m = metrics();
        double cell = m.cell;
        double x = m.ox + selC * cell;
        double y = m.oy + selR * cell;
        double w = getWidth();
        double h = getHeight();
        double dx = 0;
        double dy = 0;
        if (x < PAD) dx = PAD - x;
        else if (x + cell > w - PAD) dx = (w - PAD) - (x + cell);
        if (y < PAD) dy = PAD - y;
        else if (y + cell > h - PAD) dy = (h - PAD) - (y + cell);
        if (dx != 0 || dy != 0) {
            panBy(dx, dy);
        }
    }

    private void resetViewTransforms() {
        fitToWindow = true;
        zoom = 1.0;
        panX = panY = 0.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

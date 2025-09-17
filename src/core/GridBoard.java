package core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GridBoard implements Board {
    private final int rows;
    private final int cols;
    private final int mines;
    private final MinePlacer placer;
    private final int[][] layout; // null until first reveal triggers placement
    private final VisibleState[][] visible;
    private final boolean lost;
    private final int revealedCount;
    private final int flaggedCount;
    private final int mineHits; // number of manual mine hits so far

    public GridBoard(int rows, int cols, int mines, MinePlacer placer) {
        this(rows, cols, mines, placer, null, fillVisible(rows, cols, VisibleState.HIDDEN), false, 0, 0, 0);
    }

    private GridBoard(int rows, int cols, int mines, MinePlacer placer,
                      int[][] layout, VisibleState[][] visible,
                      boolean lost, int revealedCount, int flaggedCount, int mineHits) {
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("Invalid size");
        if (mines < 0 || mines >= rows * cols) throw new IllegalArgumentException("Invalid mine count");
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
        this.placer = Objects.requireNonNull(placer, "placer");
        this.layout = layout;
        this.visible = visible;
        this.lost = lost;
        this.revealedCount = revealedCount;
        this.flaggedCount = flaggedCount;
        this.mineHits = mineHits;
    }

    private static VisibleState[][] fillVisible(int rows, int cols, VisibleState state) {
        VisibleState[][] grid = new VisibleState[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = state;
            }
        }
        return grid;
    }

    private static VisibleState[][] copyVisible(VisibleState[][] source) {
        VisibleState[][] copy = new VisibleState[source.length][];
        for (int r = 0; r < source.length; r++) {
            copy[r] = source[r].clone();
        }
        return copy;
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int cols() {
        return cols;
    }

    @Override
    public boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    @Override
    public boolean isMine(int r, int c) {
        return layout != null && layout[r][c] == -1;
    }

    @Override
    public int adjacentMines(int r, int c) {
        return layout == null ? 0 : layout[r][c];
    }

    @Override
    public VisibleState visibleAt(int r, int c) {
        return visible[r][c];
    }

    @Override
    public boolean isLost() {
        return lost;
    }

    @Override
    public boolean isWon() {
        return !lost && revealedCount == rows * cols - mines;
    }

    @Override
    public int totalMines() {
        return mines;
    }

    @Override
    public int flaggedCount() {
        return flaggedCount;
    }

    @Override
    public Board reveal(int r, int c) {
        if (!inBounds(r, c) || lost) return this;
        if (!visible[r][c].isHidden()) return this;
        if (layout == null) {
            return ensureLayout(r, c).reveal(r, c);
        }
        if (layout[r][c] == -1) {
            return revealMine(r, c);
        }
        return floodReveal(r, c);
    }

    @Override
    public Board toggleFlag(int r, int c) {
        if (!inBounds(r, c) || lost) return this;
        VisibleState current = visible[r][c];
        if (current.isRevealed()) return this;

        VisibleState[][] next = copyVisible(visible);
        int nextFlagged = flaggedCount;
        if (current.isFlagged()) {
            next[r][c] = VisibleState.HIDDEN;
            nextFlagged = Math.max(0, nextFlagged - 1);
        } else {
            next[r][c] = VisibleState.FLAGGED;
            nextFlagged++;
        }
        return new GridBoard(rows, cols, mines, placer, layout, next, lost, revealedCount, nextFlagged, mineHits);
    }

    @Override
    public Board chord(int r, int c) {
        if (!inBounds(r, c) || lost || layout == null) return this;
        if (!visible[r][c].isRevealed()) return this;
        int required = layout[r][c];
        if (required <= 0) return this;

        int flaggedAround = 0;
        List<int[]> hiddenNeighbors = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr;
                int nc = c + dc;
                if (!inBounds(nr, nc)) continue;
                VisibleState state = visible[nr][nc];
                if (state.isFlagged()) flaggedAround++;
                else if (state.isHidden()) hiddenNeighbors.add(new int[]{nr, nc});
            }
        }

        if (flaggedAround != required) return this;

        GridBoard current = this;
        for (int[] cell : hiddenNeighbors) {
            current = (GridBoard) current.reveal(cell[0], cell[1]);
        }
        return current;
    }

    private GridBoard ensureLayout(int safeR, int safeC) {
        int[][] generated = placer.placeMines(rows, cols, mines, safeR, safeC);
        return new GridBoard(rows, cols, mines, placer, generated, visible, lost, revealedCount, flaggedCount, mineHits);
    }

    private GridBoard floodReveal(int startR, int startC) {
        VisibleState[][] next = copyVisible(visible);
        int nextRevealed = revealedCount;
        int nextFlagged = flaggedCount;
        List<int[]> newlyOpened = new ArrayList<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startR, startC});
        while (!queue.isEmpty()) {
            int[] cell = queue.removeFirst();
            int r = cell[0];
            int c = cell[1];
            if (!inBounds(r, c)) continue;
            if (!next[r][c].isHidden()) continue;
            next[r][c] = VisibleState.REVEALED;
            nextRevealed++;
            newlyOpened.add(new int[]{r, c});
            if (layout[r][c] == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        queue.add(new int[]{r + dr, c + dc});
                    }
                }
            }
        }
        if (!newlyOpened.isEmpty()) {
            // Reveal bordering mines around the newly opened area without ending the game.
            Set<Long> minesToReveal = new HashSet<>();
            for (int[] cell : newlyOpened) {
                int r = cell[0];
                int c = cell[1];
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr;
                        int nc = c + dc;
                        if (!inBounds(nr, nc)) continue;
                        if (layout[nr][nc] == -1) {
                            minesToReveal.add(cellKey(nr, nc));
                        }
                    }
                }
            }
            for (long key : minesToReveal) {
                int mr = (int) (key >> 32);
                int mc = (int) key;
                VisibleState current = next[mr][mc];
                if (current.isRevealed()) continue;
                if (current.isFlagged()) {
                    nextFlagged = Math.max(0, nextFlagged - 1);
                }
                next[mr][mc] = VisibleState.REVEALED;
            }
        }
        return new GridBoard(rows, cols, mines, placer, layout, next, false, nextRevealed, nextFlagged, mineHits);
    }

    private GridBoard revealMine(int r, int c) {
        // First mine hit is forgiven; the second ends the game.
        if (mineHits == 0) {
            VisibleState[][] next = copyVisible(visible);
            int nextFlagged = flaggedCount;
            if (next[r][c].isFlagged()) {
                nextFlagged = Math.max(0, nextFlagged - 1);
            }
            next[r][c] = VisibleState.REVEALED;
            return new GridBoard(rows, cols, mines, placer, layout, next, false, revealedCount, nextFlagged, mineHits + 1);
        }
        return revealAllMines(1);
    }

    private GridBoard revealAllMines(int additionalHits) {
        VisibleState[][] next = copyVisible(visible);
        int nextRevealed = revealedCount;
        int nextFlagged = flaggedCount;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (layout[r][c] != -1) continue;
                VisibleState current = next[r][c];
                if (current.isRevealed()) continue;
                if (current.isFlagged()) {
                    nextFlagged = Math.max(0, nextFlagged - 1);
                }
                next[r][c] = VisibleState.REVEALED;
            }
        }
        return new GridBoard(rows, cols, mines, placer, layout, next, true, nextRevealed, nextFlagged, mineHits + Math.max(0, additionalHits));
    }

    private static long cellKey(int r, int c) {
        return (((long) r) << 32) ^ (c & 0xffffffffL);
    }
}

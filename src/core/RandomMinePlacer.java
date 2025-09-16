package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class RandomMinePlacer implements MinePlacer {
    private final Random rng;

    public RandomMinePlacer() {
        this(new Random());
    }

    public RandomMinePlacer(Random rng) {
        this.rng = Objects.requireNonNull(rng, "rng");
    }

    @Override
    public int[][] placeMines(int rows, int cols, int mineCount, int safeR, int safeC) {
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("Invalid board size");
        if (mineCount < 0 || mineCount >= rows * cols)
            throw new IllegalArgumentException("Invalid mine count");

        int[][] result = new int[rows][cols];

        List<int[]> safeCells = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = safeR + dr;
                int nc = safeC + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    safeCells.add(new int[]{nr, nc});
                }
            }
        }

        int maxSafe = Math.max(1, rows * cols - mineCount);
        if (safeCells.size() > maxSafe) {
            safeCells.sort(Comparator.comparingInt(cell -> -distanceSquared(cell[0], cell[1], safeR, safeC)));
            while (safeCells.size() > maxSafe) {
                safeCells.remove(0); // trim farthest first
            }
        }

        Set<Long> safeSet = new HashSet<>();
        for (int[] cell : safeCells) {
            safeSet.add(key(cell[0], cell[1]));
        }

        List<int[]> candidates = new ArrayList<>(rows * cols - safeSet.size());
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!safeSet.contains(key(r, c))) {
                    candidates.add(new int[]{r, c});
                }
            }
        }

        if (mineCount > candidates.size()) {
            throw new IllegalArgumentException("Too many mines for size/safe zone.");
        }

        Collections.shuffle(candidates, rng);
        for (int i = 0; i < mineCount; i++) {
            int[] cell = candidates.get(i);
            result[cell[0]][cell[1]] = -1;
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (result[r][c] == -1) continue;
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && result[nr][nc] == -1) {
                            count++;
                        }
                    }
                }
                result[r][c] = count;
            }
        }

        return result;
    }

    private static int distanceSquared(int r, int c, int sr, int sc) {
        int dr = r - sr;
        int dc = c - sc;
        return dr * dr + dc * dc;
    }

    private static long key(int r, int c) {
        return (((long) r) << 32) ^ (c & 0xffffffffL);
    }
}

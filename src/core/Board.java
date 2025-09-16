package core;

public interface Board {
    int rows();
    int cols();
    boolean inBounds(int r, int c);
    boolean isMine(int r, int c);
    int adjacentMines(int r, int c); // -1 if mine
    VisibleState visibleAt(int r, int c);
    boolean isLost();
    boolean isWon();

    // for UI counters
    int totalMines();
    int flaggedCount();

    Board reveal(int r, int c);     // lazy mine placement on first reveal
    Board toggleFlag(int r, int c); // flag/unflag
    Board chord(int r, int c);      // open neighbors if flags == number
}

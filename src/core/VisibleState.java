package core;

/**
 * Enumeration describing the UI-visible state of a cell. Using an enum keeps
 * the values type-safe and naturally immutable which plays nicely with the
 * functional board representation.
 */
public enum VisibleState {
    HIDDEN,
    REVEALED,
    FLAGGED;

    public boolean isHidden() {
        return this == HIDDEN;
    }

    public boolean isRevealed() {
        return this == REVEALED;
    }

    public boolean isFlagged() { return this == FLAGGED; }
}

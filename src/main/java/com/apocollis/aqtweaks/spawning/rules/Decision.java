package com.apocollis.aqtweaks.spawning.rules;

/** What the conditions table decided for a spawn: the result, and the row that decided it. */
public final class Decision {
    public enum Result {
        ALLOW, DENY, DEFAULT
    }

    /** No row matched: the spawn is left to the game. */
    public static final Decision NONE = new Decision(null, -1, false);

    public final Result result;
    /** The index of the deciding row in the table, or -1. */
    public final int row;
    /** True when a row with a 'random' chance was tried on the way, so the answer may differ run to run. */
    public final boolean usedRandom;

    Decision(Result result, int row, boolean usedRandom) {
        this.result = result;
        this.row = row;
        this.usedRandom = usedRandom;
    }

    public boolean matched() {
        return result != null;
    }

    @Override
    public String toString() {
        return result == null ? "none" : result + " (row " + row + ")";
    }
}

package com.apocollis.aqtweaks.twilightforest;

/** Pure column search for a standable spot: a solid block with two clear blocks above it. */
public final class ColumnSearch {
    private ColumnSearch() {}

    /**
     * Scans one column from the top down. {@code solid[i]} is whether the block at {@code minY + i} blocks movement.
     * Returns the world Y of the first clear block that rests on a solid block and has a second clear block above it,
     * or -1 if there is none.
     */
    public static int topStandableY(boolean[] solid, int minY) {
        if (solid == null) return -1;
        for (int i = solid.length - 3; i >= 1; i--) {
            if (solid[i - 1] && !solid[i] && !solid[i + 1]) return minY + i;
        }
        return -1;
    }
}

package com.apocollis.aqtweaks.nether;

/** Pure split of one per-tick flood-fill budget across the clouds that are filling. No Minecraft types. */
public final class FillBudget {
    private FillBudget() {}

    /**
     * Splits {@code total} into {@code n} shares that sum to {@code total}. The remainder goes one cell each to
     * the shares starting at {@code start} (wrapping), so a rotating start never starves the same cloud.
     */
    public static int[] split(int total, int n, int start) {
        if (n <= 0) return new int[0];
        int[] shares = new int[n];
        int base = Math.max(0, total) / n;
        int rest = Math.max(0, total) - base * n;
        int first = Math.floorMod(start, n);
        for (int i = 0; i < n; i++) {
            shares[i] = base;
        }
        for (int k = 0; k < rest; k++) {
            shares[(first + k) % n]++;
        }
        return shares;
    }
}

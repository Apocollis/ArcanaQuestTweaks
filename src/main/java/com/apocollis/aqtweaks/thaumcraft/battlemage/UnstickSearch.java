package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The order in which the stuck-in-a-wall emergency tries spots (plan 3.14): nearest first, going up costing less than
 * going down, so the mage surfaces rather than digging deeper. The world checks (room, ground, hazards) are done by the
 * caller on each offset in this order. Pure.
 */
public final class UnstickSearch {

    private UnstickSearch() {}

    /** Horizontal and vertical offsets (blocks) in the order to try them. */
    public static List<int[]> offsets(int radius, int up, int down) {
        List<int[]> all = new ArrayList<>();
        int r = Math.max(1, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -Math.max(0, down); dy <= Math.max(0, up); dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    all.add(new int[] {dx, dy, dz});
                }
            }
        }
        Collections.sort(all, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return Double.compare(cost(a), cost(b));
            }
        });
        return all;
    }

    /** Euclidean distance, with a downward step costing half again as much as an upward one. */
    static double cost(int[] o) {
        double vertical = o[1] < 0 ? -o[1] * 1.5D : o[1];
        return Math.sqrt(o[0] * o[0] + o[2] * o[2] + vertical * vertical);
    }
}

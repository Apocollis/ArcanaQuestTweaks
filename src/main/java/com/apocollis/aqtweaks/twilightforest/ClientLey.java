package com.apocollis.aqtweaks.twilightforest;

/**
 * Client copy of the player's Ley-Rupture exposure factor and of the nearby sheltered structure boxes the server
 * sent. Plain statics so any client class may read them.
 */
public final class ClientLey {
    private ClientLey() {}

    public static volatile float exposure;
    /** Boxes of Shelter Features structures near the player, six ints each: minX, minY, minZ, maxX, maxY, maxZ. */
    public static volatile int[] shelterBoxes = new int[0];

    public static void clear() {
        exposure = 0.0F;
        shelterBoxes = new int[0];
    }

    /** True if the point lies inside a sheltered structure's box. */
    public static boolean isInShelter(double x, double y, double z) {
        int[] b = shelterBoxes;
        for (int i = 0; i + 5 < b.length; i += 6) {
            if (x >= b[i] && x < b[i + 3] + 1 && y >= b[i + 1] && y < b[i + 4] + 1 && z >= b[i + 2] && z < b[i + 5] + 1) {
                return true;
            }
        }
        return false;
    }

    /** True if the column lies inside a sheltered structure's footprint, at any height. */
    public static boolean inShelterFootprint(double x, double z) {
        int[] b = shelterBoxes;
        for (int i = 0; i + 5 < b.length; i += 6) {
            if (x >= b[i] && x < b[i + 3] + 1 && z >= b[i + 2] && z < b[i + 5] + 1) return true;
        }
        return false;
    }
}

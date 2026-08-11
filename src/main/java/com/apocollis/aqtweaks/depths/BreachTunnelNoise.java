package com.apocollis.aqtweaks.depths;

/**
 * Compatibility façade: +Y seam mouths follow {@link UpperTunnelNetwork}.
 */
public final class BreachTunnelNoise {

    public static final int SEAM_MIN_Y = -1;
    public static final int SEAM_MAX_Y = UpperTunnelNetwork.SEAM_MAX_Y;
    public static final int TOP = UpperTunnelNetwork.SEAM_TOP;

    private BreachTunnelNoise() {}

    public static synchronized void init(long worldSeed) {
        UpperTunnelNetwork.init(worldSeed);
    }

    public static boolean shouldOpenSeam(int worldX, int worldZ) {
        return UpperTunnelNetwork.shouldOpenSeam(worldX, worldZ);
    }
}

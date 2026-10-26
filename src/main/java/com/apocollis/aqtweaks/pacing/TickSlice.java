package com.apocollis.aqtweaks.pacing;

import net.minecraft.server.MinecraftServer;

/**
 * One shared clock for the relight work that runs inside a server tick ({@code DeferredRelight},
 * {@code PasteRelightQueue}, {@code RtgLightFixQueue}, the chunk gap recheck pacing). The first user in a tick
 * starts the clock; every user measures elapsed time against that same start, so the combined work in a tick
 * stays near the largest single limit instead of adding up. Server thread only.
 */
public final class TickSlice {

    private static int tick = Integer.MIN_VALUE;
    private static long startNs;

    private TickSlice() {}

    /** Start time (nanoTime) of this server tick's shared slice; set by the first caller in the tick. */
    public static long start(MinecraftServer server) {
        int t = server == null ? tick : server.getTickCounter();
        if (server == null || t != tick) {
            tick = t;
            startNs = System.nanoTime();
        }
        return startNs;
    }

    public static double elapsedMs(MinecraftServer server) {
        return (System.nanoTime() - start(server)) / 1_000_000.0;
    }
}

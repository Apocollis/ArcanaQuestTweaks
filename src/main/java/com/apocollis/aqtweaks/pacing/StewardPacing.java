package com.apocollis.aqtweaks.pacing;

import net.minecraftforge.fml.common.Loader;

/**
 * Pack bridge to Chunk Steward. The slice is sized from the live API.
 * A missing mod or a stale jar skips the slice clock ({@code sliceBudgetMs} returns a negative value)
 * instead of turning the 10 ms headroom fallback into an 8 ms budget.
 */
public final class StewardPacing {
    private static final boolean PRESENT = Loader.isModLoaded("chunksteward");
    /** Below this average headroom, chunk generation pauses optional lighting. */
    private static final double GENERATE_HEADROOM_MS = 4.0;

    private StewardPacing() {}

    public static boolean isPresent() {
        return PRESENT;
    }

    public static double remainingMs(double targetMs) {
        return PRESENT ? Api.remaining(targetMs) : targetMs;
    }

    /**
     * True when the server is behind 20 TPS, or chunks are generating and average headroom is under 4 ms.
     * At full TPS with at least 4 ms of headroom, optional refreshes run while chunks trickle in.
     */
    public static boolean shouldPauseOptionalWork() {
        return PRESENT && Api.busy();
    }

    public static double getHeadroomMs() {
        return PRESENT ? Api.headroom() : 10.0;
    }

    /** {@code 1.0} at 20 TPS. The {@code 1.0} fallback applies only when the API cannot be called. */
    public static double getLoadFactor() {
        return PRESENT ? Api.loadFactor() : 1.0;
    }

    /**
     * Milliseconds of relight work for this server tick. One value, shared by every world.
     * Negative: slice clock off (Steward missing or {@code LinkageError}). Callers keep count caps only.
     * Full TPS: {@code min(maxSliceMs, headroom)}, and 0 is allowed. Below full TPS down to
     * {@code pauseLoadFactor}: remaining time against 40 ms (10 ms tighter than headroom's 50 ms average).
     * Under {@code pauseLoadFactor}: 0.
     */
    public static double sliceBudgetMs(double maxSliceMs, double pauseLoadFactor) {
        if (!PRESENT) {
            return -1.0;
        }
        return Api.slice(maxSliceMs, pauseLoadFactor);
    }

    private static final class Api {
        // ChunkStewardAPI is only resolved by the ClassLoader if Api is touched (when PRESENT is true).
        // Every method catches LinkageError (stale jar without api/) and falls back to unpaced values.
        static double remaining(double t) {
            try {
                return com.apocollis.chunksteward.api.ChunkStewardAPI.getRemainingTickBudgetMs(t);
            } catch (LinkageError e) {
                return t;
            }
        }

        static boolean busy() {
            try {
                if (com.apocollis.chunksteward.api.ChunkStewardAPI.isServerBehind()) {
                    return true;
                }
                return com.apocollis.chunksteward.api.ChunkStewardAPI.isGeneratingChunks()
                        && com.apocollis.chunksteward.api.ChunkStewardAPI.getHeadroomMs() < GENERATE_HEADROOM_MS;
            } catch (LinkageError e) {
                return false;
            }
        }

        static double headroom() {
            try {
                return com.apocollis.chunksteward.api.ChunkStewardAPI.getHeadroomMs();
            } catch (LinkageError e) {
                return 10.0;
            }
        }

        static double loadFactor() {
            try {
                return com.apocollis.chunksteward.api.ChunkStewardAPI.getLoadFactor();
            } catch (LinkageError e) {
                return 1.0;
            }
        }

        /** Negative on {@code LinkageError} so a stale jar does not feed the 10 ms headroom fallback into the slice. */
        static double slice(double maxSliceMs, double pauseLoadFactor) {
            try {
                double load = com.apocollis.chunksteward.api.ChunkStewardAPI.getLoadFactor();
                if (load >= 1.0) {
                    double headroom = com.apocollis.chunksteward.api.ChunkStewardAPI.getHeadroomMs();
                    return Math.min(maxSliceMs, Math.max(0.0, headroom));
                }
                if (load < pauseLoadFactor) {
                    return 0.0;
                }
                double rem = com.apocollis.chunksteward.api.ChunkStewardAPI.getRemainingTickBudgetMs(40.0);
                return rem < 2.0 ? 0.0 : Math.min(rem, maxSliceMs);
            } catch (LinkageError e) {
                return -1.0;
            }
        }
    }
}

package com.apocollis.aqtweaks.pacing;

import net.minecraftforge.fml.common.Loader;

public final class StewardPacing {
    private static final boolean PRESENT = Loader.isModLoaded("chunksteward");

    private StewardPacing() {}

    public static boolean isPresent() {
        return PRESENT;
    }

    public static double remainingMs(double targetMs) {
        return PRESENT ? Api.remaining(targetMs) : targetMs;
    }

    public static boolean shouldPauseOptionalWork() {
        return PRESENT && Api.busy();
    }

    public static double getHeadroomMs() {
        return PRESENT ? Api.headroom() : 10.0;
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
                return com.apocollis.chunksteward.api.ChunkStewardAPI.isServerBehind()
                        || com.apocollis.chunksteward.api.ChunkStewardAPI.isGeneratingChunks();
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
    }
}

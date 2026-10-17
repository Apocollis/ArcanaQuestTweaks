package com.apocollis.aqtweaks.depths;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sayys.depthsupdate.core.HeightContext;

import java.util.Map;

/**
 * Dimension-indexed lookup table for Depths Update {@code HeightManager.get(int)} and
 * {@code isExtended(int)}. Avoids per-call {@code Integer.valueOf(dim)} boxing and
 * {@code Map.getOrDefault}/{@code containsKey} dispatch on hot world/chunk/light paths.
 */
public final class HeightCache {

    private static final Logger LOGGER = LogManager.getLogger(ArcanaQuestTweaks.MODID);

    public static final int MIN_DIM = -64;
    public static final int LEN = 256;

    public static final class Table {
        public final HeightContext[] byDim;
        public final Map<Integer, HeightContext> source;

        Table(HeightContext[] byDim, Map<Integer, HeightContext> source) {
            this.byDim = byDim;
            this.source = source;
        }
    }

    public static volatile Table table;

    private static final Object LOCK = new Object();
    private static volatile long epoch;
    private static volatile boolean verifyMismatchLogged;

    private HeightCache() {}

    public static void invalidate() {
        synchronized (LOCK) {
            epoch++;
            table = null;
            verifyMismatchLogged = false;
        }
    }

    public static HeightContext getSlow(int dim, Map<Integer, HeightContext> contexts) {
        if (contexts == null) {
            return HeightContext.VANILLA;
        }
        HeightContext expected = contexts.getOrDefault(Integer.valueOf(dim), HeightContext.VANILLA);
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheEnabled) {
            return expected;
        }
        Table t = ensureTable(contexts);
        if (ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheVerify && t != null) {
            int i = dim - MIN_DIM;
            if (i >= 0 && i < LEN) {
                HeightContext cached = t.byDim[i];
                HeightContext fast = cached != null ? cached : HeightContext.VANILLA;
                if (fast != expected && !verifyMismatchLogged) {
                    verifyMismatchLogged = true;
                    LOGGER.error(
                            "[AQ-DEPTHS] HeightCache.get mismatch for dim={}: fast={} expected={}",
                            dim, fast, expected);
                }
            }
        }
        return expected;
    }

    public static boolean isExtendedSlow(int dim, Map<Integer, HeightContext> contexts) {
        if (contexts == null) {
            return false;
        }
        boolean expected = contexts.containsKey(Integer.valueOf(dim));
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheEnabled) {
            return expected;
        }
        Table t = ensureTable(contexts);
        if (ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheVerify && t != null) {
            int i = dim - MIN_DIM;
            if (i >= 0 && i < LEN) {
                boolean fast = t.byDim[i] != null;
                if (fast != expected && !verifyMismatchLogged) {
                    verifyMismatchLogged = true;
                    LOGGER.error(
                            "[AQ-DEPTHS] HeightCache.isExtended mismatch for dim={}: fast={} expected={}",
                            dim, fast, expected);
                }
            }
        }
        return expected;
    }

    private static Table ensureTable(Map<Integer, HeightContext> contexts) {
        Table current = table;
        if (current != null && current.source == contexts) {
            return current;
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            long ep = epoch;
            HeightContext[] byDim = new HeightContext[LEN];
            for (Map.Entry<Integer, HeightContext> entry : contexts.entrySet()) {
                Integer key = entry.getKey();
                if (key == null) continue;
                int idx = key.intValue() - MIN_DIM;
                if (idx >= 0 && idx < LEN) {
                    byDim[idx] = entry.getValue();
                }
            }
            synchronized (LOCK) {
                if (epoch == ep) {
                    Table built = new Table(byDim, contexts);
                    table = built;
                    return built;
                }
            }
        }
        return table;
    }
}

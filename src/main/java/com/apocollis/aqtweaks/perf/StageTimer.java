package com.apocollis.aqtweaks.perf;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Step 0 worldgen stage timer. Accumulates {@code System.nanoTime()} per stage and logs a summary
 * every 5 seconds on server tick END when {@code perfTimers} is enabled in {@code aqtweaks_rtg.cfg}.
 */
public final class StageTimer {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Perf");
    private static final long REPORT_INTERVAL_NS = 5_000_000_000L;

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger();
        final AtomicLong totalNs = new AtomicLong();
        final AtomicLong maxNs = new AtomicLong();
        volatile String maxDetail = "";

        void record(long ns, String detail) {
            count.incrementAndGet();
            totalNs.addAndGet(ns);
            long prev;
            do {
                prev = maxNs.get();
                if (ns <= prev) {
                    return;
                }
            } while (!maxNs.compareAndSet(prev, ns));
            if (detail != null) {
                maxDetail = detail;
            }
        }
    }

    private static final Map<String, Bucket> BUCKETS = new ConcurrentHashMap<>();
    private static volatile long nextReportNs = 0L;

    private StageTimer() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.RtgModuleConfig.surface.perfTimers;
    }

    public static long start() {
        return enabled() ? System.nanoTime() : 0L;
    }

    public static void end(String stage, long startNs) {
        if (startNs == 0L || !enabled()) {
            return;
        }
        recordNs(stage, System.nanoTime() - startNs, null);
    }

    public static void end(String stage, long startNs, String detail) {
        if (startNs == 0L || !enabled()) {
            return;
        }
        recordNs(stage, System.nanoTime() - startNs, detail);
    }

    public static void recordNs(String stage, long elapsedNs, String detail) {
        if (!enabled() || elapsedNs < 0L) {
            return;
        }
        BUCKETS.computeIfAbsent(stage, k -> new Bucket()).record(elapsedNs, detail);
    }

    private static void reportIfDue() {
        if (!enabled()) {
            if (!BUCKETS.isEmpty()) {
                BUCKETS.clear();
            }
            return;
        }
        long now = System.nanoTime();
        if (nextReportNs == 0L) {
            nextReportNs = now + REPORT_INTERVAL_NS;
            return;
        }
        if (now < nextReportNs) {
            return;
        }
        nextReportNs = now + REPORT_INTERVAL_NS;
        if (BUCKETS.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder("[AQ-PERF] 5s stages:");
        boolean any = false;
        for (Map.Entry<String, Bucket> entry : BUCKETS.entrySet()) {
            Bucket b = entry.getValue();
            int c = b.count.getAndSet(0);
            long tot = b.totalNs.getAndSet(0L);
            long mx = b.maxNs.getAndSet(0L);
            String det = b.maxDetail;
            b.maxDetail = "";
            if (c <= 0) {
                continue;
            }
            any = true;
            double totMs = tot / 1_000_000.0;
            double avgMs = totMs / c;
            double maxMs = mx / 1_000_000.0;
            sb.append(' ').append(entry.getKey())
                    .append(String.format(Locale.ROOT, "=%.1fms/%d(avg=%.2fms,max=%.2fms", totMs, c, avgMs, maxMs));
            if (det != null && !det.isEmpty()) {
                sb.append(',').append(det);
            }
            sb.append(')');
        }
        if (any) {
            LOGGER.info(sb.toString());
        }
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static final class Events {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                reportIfDue();
            }
        }
    }
}

package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An opt-in log of what happened to the mobs you are watching, written to {@code spawn_trace.log} in
 * {@code config/arcanaquesttweaks/} so it can be sent: each spawn check and join (with In Control's result, the
 * conditions table's result, the live counts), each time the potential-spawn table offered or withheld the mob, and
 * what the pack filler and spawn parties did. Off by default; switch on with the config option or
 * {@code /aqspawntable trace on}.
 */
public final class SpawnTrace {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private static volatile boolean runtimeOn;
    private static volatile Set<String> runtimeMobs;
    private static final Map<String, Long> LAST = new HashMap<String, Long>();
    private static BufferedWriter out;
    private static File file;
    private static boolean failed;

    private SpawnTrace() {
    }

    public static boolean enabled() {
        return runtimeOn || SpawningModuleConfig.general.spawnTrace;
    }

    public static void setRuntime(boolean on) {
        runtimeOn = on;
        if (on) {
            line("-- trace on; watching " + describeWatched());
        }
    }

    /** Replaces the watch list until the game restarts; "*" watches every mob. */
    public static void setRuntimeMobs(Set<String> mobs) {
        runtimeMobs = mobs;
        line("-- watching " + describeWatched());
    }

    public static String describeWatched() {
        Set<String> mobs = watchedSet();
        String auto = AUTO.isEmpty() ? "" : " (plus, after a shadow mismatch: " + String.join(", ", AUTO) + ")";
        return (mobs.contains("*") ? "every mob" : String.join(", ", mobs)) + auto;
    }

    private static Set<String> watchedSet() {
        Set<String> chosen = runtimeMobs;
        if (chosen != null) {
            return chosen;
        }
        Set<String> fromConfig = new HashSet<String>();
        String[] listed = SpawningModuleConfig.general.spawnTraceMobs;
        if (listed != null) {
            for (String id : listed) {
                fromConfig.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }
        return fromConfig;
    }

    public static boolean watching(String mobId) {
        if (!enabled() || mobId == null) {
            return false;
        }
        Set<String> watched = watchedSet();
        return watched.contains("*") || watched.contains(mobId) || AUTO.contains(mobId);
    }

    /** Mobs that were not named but showed a mismatch in the shadow comparison, and are now watched in full. */
    private static final Set<String> AUTO = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<String>());
    private static final int MAX_AUTO = 50;

    /**
     * Called when the shadow comparison finds a mismatch for a mob. A mob that is not already watched starts being
     * watched in full from now on (every spawn check, join, offer, pack and party line), so the next events show why.
     */
    public static void escalate(String mobId, String reason) {
        if (mobId == null || !enabled() || !SpawningModuleConfig.general.spawnTraceAllMobs) {
            return;
        }
        if (watchedSet().contains("*") || watchedSet().contains(mobId) || AUTO.size() >= MAX_AUTO) {
            return;
        }
        if (AUTO.add(mobId)) {
            line("-- shadow mismatch for " + mobId + "; now watching it in full: " + reason);
        }
    }

    /** Writes a line if this mob is watched. */
    public static void log(String mobId, String message) {
        if (watching(mobId)) {
            line(message);
        }
    }

    /** True at most once per interval for a key, so a query that runs every tick writes a few lines, not thousands. */
    public static synchronized boolean every(String key, long millis) {
        long now = System.currentTimeMillis();
        Long last = LAST.get(key);
        if (last != null && now - last < millis) {
            return false;
        }
        LAST.put(key, now);
        return true;
    }

    /** True when mobs that are not on the watch list are tallied and, when something notable happens, logged. */
    public static boolean others() {
        return enabled() && SpawningModuleConfig.general.spawnTraceAllMobs;
    }

    private static final Map<String, Map<String, Long>> TALLY = new java.util.TreeMap<String, Map<String, Long>>();

    /** Counts one event for a mob under a label; written out and cleared by the next summary. */
    public static synchronized void count(String key, String label) {
        Map<String, Long> labels = TALLY.get(key);
        if (labels == null) {
            labels = new java.util.TreeMap<String, Long>();
            TALLY.put(key, labels);
        }
        Long was = labels.get(label);
        labels.put(label, was == null ? 1L : was + 1L);
    }

    /** Writes the tallies since the last call, busiest mobs first, and clears them. */
    public static synchronized void writeTally() {
        if (TALLY.isEmpty()) {
            line("tally  (no spawn events since the last summary)");
            return;
        }
        java.util.List<Map.Entry<String, Map<String, Long>>> entries =
                new java.util.ArrayList<Map.Entry<String, Map<String, Long>>>(TALLY.entrySet());
        entries.sort((a, b) -> Long.compare(total(b.getValue()), total(a.getValue())));
        int shown = 0;
        for (Map.Entry<String, Map<String, Long>> entry : entries) {
            if (shown++ >= 60) {
                line("tally  ... and " + (entries.size() - 60) + " more mobs");
                break;
            }
            StringBuilder text = new StringBuilder("tally  " + entry.getKey() + ":");
            for (Map.Entry<String, Long> label : entry.getValue().entrySet()) {
                text.append(' ').append(label.getKey()).append('=').append(label.getValue());
            }
            line(text.toString());
        }
        TALLY.clear();
    }

    private static long total(Map<String, Long> labels) {
        long sum = 0;
        for (long each : labels.values()) {
            sum += each;
        }
        return sum;
    }

    public static File file() {
        return file != null ? file : target();
    }

    private static File target() {
        File config = PotentialSpawnTable.configDirectory();
        return config == null ? null : new File(config, "arcanaquesttweaks/spawn_trace.log");
    }

    public static synchronized void line(String message) {
        if (failed) {
            return;
        }
        try {
            if (out == null) {
                file = target();
                if (file == null) {
                    return;
                }
                Files.createDirectories(file.getParentFile().toPath());
                if (file.isFile() && file.length() > MAX_BYTES) {
                    File old = new File(file.getParentFile(), "spawn_trace.old.log");
                    Files.deleteIfExists(old.toPath());
                    Files.move(file.toPath(), old.toPath());
                }
                out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                out.write("== trace started " + java.time.LocalDateTime.now() + "; watching " + describeWatched() + "\n");
            }
            out.write("[" + LocalTime.now().format(CLOCK) + "] " + message + "\n");
            out.flush();
        } catch (IOException problem) {
            failed = true;
            LOGGER.warn("[AQTweaks] Could not write the spawn trace; it is switched off for this session.", problem);
        }
    }
}

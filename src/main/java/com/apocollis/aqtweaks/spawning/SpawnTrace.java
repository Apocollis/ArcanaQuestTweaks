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
        return mobs.contains("*") ? "every mob" : String.join(", ", mobs);
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
        return watched.contains("*") || watched.contains(mobId);
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

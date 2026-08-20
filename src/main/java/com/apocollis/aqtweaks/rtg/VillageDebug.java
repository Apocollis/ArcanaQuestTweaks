package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Village terrain traces for {@code logs/villagepatch.log}. Not written to latest.log.
 */
public final class VillageDebug {

    private static final Object LOCK = new Object();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static boolean opened;
    private static Path path;

    private VillageDebug() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageFlattenDebug;
    }

    public static boolean once(String key) {
        return key != null && ONCE.add(key);
    }

    public static void log(String format, Object... args) {
        if (!enabled()) return;
        String message;
        try {
            message = args == null || args.length == 0 ? format : String.format(format, args);
        } catch (Throwable t) {
            message = format;
        }
        write(message);
    }

    private static void write(String message) {
        String line = LocalDateTime.now().format(TIME) + " " + message;
        synchronized (LOCK) {
            try {
                if (!opened) {
                    path = Paths.get("logs", "villagepatch.log");
                    Path parent = path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.write(path, Collections.singletonList(
                            LocalDateTime.now().format(TIME) + " villagepatch log started"),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                    opened = true;
                }
                Files.write(path, Collections.singletonList(line), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Throwable ignored) {
            }
        }
    }
}

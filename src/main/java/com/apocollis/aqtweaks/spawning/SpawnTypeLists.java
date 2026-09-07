package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pack JSON {@code config/arcanaquest/mob_overworldspawntype.json}.
 * Exclusive sets = surface − underground and underground − surface. Intersection is both-layer.
 */
public final class SpawnTypeLists {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final Gson GSON = new GsonBuilder().create();

    private static File configDir;
    private static volatile boolean ready;
    private static volatile Set<String> surfaceOnly = Set.of();
    private static volatile Set<String> undergroundOnly = Set.of();

    private SpawnTypeLists() {}

    public static void load(File configDirectory) {
        configDir = configDirectory;
        reload();
    }

    public static void reload() {
        File dir = configDir;
        if (dir == null) {
            clear(false);
            return;
        }
        String relative = SpawningModuleConfig.general.spawnTypeFile;
        if (relative == null || relative.isBlank()) {
            LOGGER.warn("[AQTweaks] Spawn type file path is empty; surface/cave pool filter off.");
            clear(false);
            return;
        }
        File file = new File(dir, relative.replace('/', File.separatorChar));
        if (!file.isFile()) {
            LOGGER.warn("[AQTweaks] Missing {}; surface/cave pool filter off.", file.getAbsolutePath());
            clear(false);
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            FileDto parsed = GSON.fromJson(reader, FileDto.class);
            if (parsed == null) {
                LOGGER.warn("[AQTweaks] {} is empty; surface/cave pool filter off.", file.getAbsolutePath());
                clear(false);
                return;
            }
            Set<String> surface = toIdSet(parsed.surface);
            Set<String> underground = toIdSet(parsed.underground);
            Set<String> surfaceExclusive = new HashSet<>(surface);
            surfaceExclusive.removeAll(underground);
            Set<String> undergroundExclusive = new HashSet<>(underground);
            undergroundExclusive.removeAll(surface);
            surfaceOnly = Set.copyOf(surfaceExclusive);
            undergroundOnly = Set.copyOf(undergroundExclusive);
            ready = true;
            LOGGER.info("[AQTweaks] Loaded spawn types from {} (surface-only {}, underground-only {}, both {}).",
                    file.getAbsolutePath(),
                    surfaceOnly.size(),
                    undergroundOnly.size(),
                    surface.size() - surfaceExclusive.size());
        } catch (Exception e) {
            LOGGER.warn("[AQTweaks] Failed to parse {}; surface/cave pool filter off.", file.getAbsolutePath(), e);
            clear(false);
        }
    }

    public static boolean ready() {
        return ready;
    }

    public static Set<String> surfaceOnly() {
        return surfaceOnly;
    }

    public static Set<String> undergroundOnly() {
        return undergroundOnly;
    }

    private static void clear(boolean isReady) {
        surfaceOnly = Set.of();
        undergroundOnly = Set.of();
        ready = isReady;
    }

    private static Set<String> toIdSet(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            String id = entry.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    @SuppressWarnings("unused")
    private static final class FileDto {
        @SerializedName("surface")
        List<String> surface;
        @SerializedName("underground")
        List<String> underground;
    }
}

package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pack JSON {@code config/arcanaquest/mob_structurespawns.json}: structure name → entity ids.
 */
public final class SpawnStructureLists {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type FILE_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private static File configDir;
    private static volatile Map<String, Set<String>> structuresById = Map.of();
    private static volatile Set<String> structureNames = Set.of();

    private SpawnStructureLists() {}

    public static void load(File configDirectory) {
        configDir = configDirectory;
        reload();
    }

    public static void reload() {
        File dir = configDir;
        if (dir == null) {
            clear();
            return;
        }
        String relative = SpawningModuleConfig.general.spawnStructureFile;
        if (relative == null || relative.isBlank()) {
            LOGGER.warn("[AQTweaks] Spawn structure file path is empty; structure cave exemption off.");
            clear();
            return;
        }
        File file = new File(dir, relative.replace('/', File.separatorChar));
        if (!file.isFile()) {
            LOGGER.warn("[AQTweaks] Missing {}; structure cave exemption off.", file.getAbsolutePath());
            clear();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            Map<String, List<String>> parsed = GSON.fromJson(reader, FILE_TYPE);
            if (parsed == null || parsed.isEmpty()) {
                LOGGER.warn("[AQTweaks] {} is empty; structure cave exemption off.", file.getAbsolutePath());
                clear();
                return;
            }
            Map<String, Set<String>> byStruct = new HashMap<>();
            Map<String, Set<String>> byId = new HashMap<>();
            int idCount = 0;
            for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String structure = entry.getKey().trim();
                if (structure.isEmpty()) {
                    continue;
                }
                Set<String> ids = toIdSet(entry.getValue());
                if (ids.isEmpty()) {
                    continue;
                }
                byStruct.put(structure, ids);
                idCount += ids.size();
                for (String id : ids) {
                    byId.computeIfAbsent(id, ignored -> new HashSet<>()).add(structure);
                }
            }
            Map<String, Set<String>> frozenById = new HashMap<>();
            for (Map.Entry<String, Set<String>> row : byId.entrySet()) {
                frozenById.put(row.getKey(), Set.copyOf(row.getValue()));
            }
            structuresById = Map.copyOf(frozenById);
            structureNames = Set.copyOf(byStruct.keySet());
            LOGGER.info("[AQTweaks] Loaded structure spawns from {} ({} structures, {} id rows).",
                    file.getAbsolutePath(), byStruct.size(), idCount);
        } catch (Exception e) {
            LOGGER.warn("[AQTweaks] Failed to parse {}; structure cave exemption off.", file.getAbsolutePath(), e);
            clear();
        }
    }

    public static Set<String> structureNames() {
        return structureNames;
    }

    public static Set<String> structuresFor(String entityId) {
        if (entityId == null) {
            return Collections.emptySet();
        }
        return structuresById.getOrDefault(entityId, Collections.emptySet());
    }

    private static void clear() {
        structuresById = Map.of();
        structureNames = Set.of();
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
        return Set.copyOf(ids);
    }
}

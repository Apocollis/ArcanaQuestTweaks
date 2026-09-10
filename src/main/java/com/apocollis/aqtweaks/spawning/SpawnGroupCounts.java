package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pack {@code mob_tier.json} + {@code mob_spawnrules.cfg}. Fill uses these group min/max, not vanilla 4–4.
 */
public final class SpawnGroupCounts {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type TIER_MAP = new TypeToken<Map<String, List<String>>>() {}.getType();
    private static final Pattern GROUP_LINE = Pattern.compile(
            "^\\s*I:(?:\"([A-Za-z0-9_]+)_group_(min|max)\"|([A-Za-z0-9_]+)_group_(min|max))\\s*=\\s*(-?\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static File configDir;
    private static volatile Map<String, SpawnGroupSizes.Range> BY_ID = Map.of();

    private SpawnGroupCounts() {}

    public static void load(File configDirectory) {
        configDir = configDirectory;
        reload();
    }

    public static void reload() {
        File dir = configDir;
        if (dir == null) {
            BY_ID = Map.of();
            return;
        }
        Map<String, int[]> minsMaxes = parseSpawnRules(dir);
        if (minsMaxes.isEmpty()) {
            BY_ID = Map.of();
            return;
        }
        Map<String, SpawnGroupSizes.Range> byId = parseTiers(dir, minsMaxes);
        BY_ID = Map.copyOf(byId);
        LOGGER.info("[AQTweaks] Loaded pack spawn group sizes for {} mobs.", BY_ID.size());
    }

    public static SpawnGroupSizes.Range rangeOf(String entityId) {
        if (entityId == null) {
            return null;
        }
        return BY_ID.get(entityId);
    }

    private static Map<String, int[]> parseSpawnRules(File dir) {
        String relative = SpawningModuleConfig.general.spawnRulesFile;
        if (relative == null || relative.isBlank()) {
            LOGGER.warn("[AQTweaks] Spawn rules file path is empty; pack group sizes off.");
            return Map.of();
        }
        File file = new File(dir, relative.replace('/', File.separatorChar));
        if (!file.isFile()) {
            LOGGER.warn("[AQTweaks] Missing {}; pack group sizes off.", file.getAbsolutePath());
            return Map.of();
        }
        Map<String, int[]> table = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int hash = line.indexOf('#');
                if (hash >= 0) {
                    line = line.substring(0, hash);
                }
                Matcher m = GROUP_LINE.matcher(line);
                if (!m.matches()) {
                    continue;
                }
                String tier = firstNonNull(m.group(1), m.group(3)).toLowerCase();
                String which = firstNonNull(m.group(2), m.group(4)).toLowerCase();
                int value = Integer.parseInt(m.group(5));
                int[] pair = table.computeIfAbsent(tier, k -> new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE});
                if ("min".equals(which)) {
                    pair[0] = value;
                } else {
                    pair[1] = value;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[AQTweaks] Failed to parse {}; pack group sizes off.", file.getAbsolutePath(), e);
            return Map.of();
        }
        Map<String, int[]> ready = new HashMap<>();
        for (var e : table.entrySet()) {
            int min = e.getValue()[0];
            int max = e.getValue()[1];
            if (min == Integer.MIN_VALUE && max == Integer.MIN_VALUE) {
                continue;
            }
            if (min == Integer.MIN_VALUE) {
                min = max;
            }
            if (max == Integer.MIN_VALUE) {
                max = min;
            }
            if (min < 1 || max < min) {
                LOGGER.warn("[AQTweaks] Ignoring spawn-rules tier {} (min {}, max {}).", e.getKey(), min, max);
                continue;
            }
            ready.put(e.getKey(), new int[] {min, max});
        }
        if (ready.isEmpty()) {
            LOGGER.warn("[AQTweaks] No group min/max in {}; pack group sizes off.", file.getAbsolutePath());
        }
        return ready;
    }

    private static Map<String, SpawnGroupSizes.Range> parseTiers(File dir, Map<String, int[]> minsMaxes) {
        String relative = SpawningModuleConfig.general.spawnTierFile;
        if (relative == null || relative.isBlank()) {
            LOGGER.warn("[AQTweaks] Spawn tier file path is empty; pack group sizes off.");
            return Map.of();
        }
        File file = new File(dir, relative.replace('/', File.separatorChar));
        if (!file.isFile()) {
            LOGGER.warn("[AQTweaks] Missing {}; pack group sizes off.", file.getAbsolutePath());
            return Map.of();
        }
        Map<String, SpawnGroupSizes.Range> byId = new HashMap<>();
        try (FileReader reader = new FileReader(file)) {
            Map<String, List<String>> tiers = GSON.fromJson(reader, TIER_MAP);
            if (tiers == null || tiers.isEmpty()) {
                LOGGER.warn("[AQTweaks] {} has no tiers; pack group sizes off.", file.getAbsolutePath());
                return Map.of();
            }
            for (var tierEntry : tiers.entrySet()) {
                if (tierEntry.getKey() == null || tierEntry.getValue() == null) {
                    continue;
                }
                String tier = tierEntry.getKey().trim().toLowerCase();
                int[] pair = minsMaxes.get(tier);
                if (pair == null) {
                    LOGGER.warn("[AQTweaks] mob_tier.json tier {} has no group min/max in spawn rules; skipped.", tier);
                    continue;
                }
                SpawnGroupSizes.Range range = new SpawnGroupSizes.Range(pair[0], pair[1]);
                for (String raw : tierEntry.getValue()) {
                    if (raw == null) {
                        continue;
                    }
                    String id = raw.trim();
                    if (!id.isEmpty()) {
                        byId.put(id, range);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[AQTweaks] Failed to parse {}; pack group sizes off.", file.getAbsolutePath(), e);
            return Map.of();
        }
        return byId;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}

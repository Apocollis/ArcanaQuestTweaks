package com.apocollis.aqtweaks.spawning.table;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The pack's classification files that {@code regenerate_potentialspawn.py} reads, parsed but not combined:
 * {@code disabled_mobs}, {@code mob_tier}, {@code mob_spawnrules.cfg}, {@code mob_biometypes},
 * {@code mob_extraspawns}, {@code biome_mappings}, {@code mob_types}, {@code mob_overworldspawntype},
 * {@code mob_structurespawns}, {@code mob_depthsspawns} and the optional {@code biome_dimensions}. The day-spawn files only matter to {@code spawn.json}.
 */
public final class SpawnTableSources {
    /** Reads a file of the pack by name; returns null when the file does not exist. */
    public interface FileReader {
        String read(String name) throws IOException;
    }

    /** What a tier gives each of its mobs. */
    public static final class TierParams {
        public int weight;
        public int groupMin;
        public int groupMax;
        public int limit;

        TierParams(int weight, int groupMin, int groupMax, int limit) {
            this.weight = weight;
            this.groupMin = groupMin;
            this.groupMax = groupMax;
            this.limit = limit;
        }

        TierParams copy() {
            return new TierParams(weight, groupMin, groupMax, limit);
        }
    }

    /** One mob of {@code mob_depthsspawns.json}; a null field falls back to the mob's tier. */
    public static final class DepthsEntry {
        public final String mob;
        public final Integer weight;
        public final Integer groupMin;
        public final Integer groupMax;
        public final Integer maxCount;
        public final Integer maxHeight;

        DepthsEntry(String mob, Integer weight, Integer groupMin, Integer groupMax, Integer maxCount,
                    Integer maxHeight) {
            this.mob = mob;
            this.weight = weight;
            this.groupMin = groupMin;
            this.groupMax = groupMax;
            this.maxCount = maxCount;
            this.maxHeight = maxHeight;
        }
    }

    private static final Pattern CFG_LINE = Pattern.compile("I:([a-zA-Z]+)_([a-zA-Z_]+)\\s*=\\s*(\\d+)");

    final Set<String> disabled;
    /** Tier name to mob ids, in file order; a mob listed twice belongs to the later tier. */
    final Map<String, List<String>> tiers;
    final Map<String, TierParams> tierParams;
    final Map<String, List<String>> biomeTypes;
    final Map<String, List<String>> extraSpawns;
    final Map<String, List<String>> biomeMappings;
    /** Category to the dimensions its biomes are in; a category not listed is in the Overworld. */
    final Map<String, List<Integer>> biomeDimensions;
    final Set<String> waterMobs;
    final Set<String> surface;
    final Set<String> underground;
    final Map<String, List<String>> structures;
    final List<DepthsEntry> depths;

    private SpawnTableSources(Set<String> disabled, Map<String, List<String>> tiers,
                              Map<String, TierParams> tierParams, Map<String, List<String>> biomeTypes,
                              Map<String, List<String>> extraSpawns, Map<String, List<String>> biomeMappings,
                              Map<String, List<Integer>> biomeDimensions, Set<String> waterMobs, Set<String> surface, Set<String> underground,
                              Map<String, List<String>> structures, List<DepthsEntry> depths) {
        this.disabled = disabled;
        this.tiers = tiers;
        this.tierParams = tierParams;
        this.biomeTypes = biomeTypes;
        this.extraSpawns = extraSpawns;
        this.biomeMappings = biomeMappings;
        this.biomeDimensions = biomeDimensions;
        this.waterMobs = waterMobs;
        this.surface = surface;
        this.underground = underground;
        this.structures = structures;
        this.depths = depths;
    }

    /**
     * Reads the files. A missing required file is an IOException; the cfg, the structure list and the depths list
     * are optional (the script's defaults, no structure rows, no depths rows).
     */
    public static SpawnTableSources load(FileReader files) throws IOException {
        Set<String> disabled = new HashSet<String>(stringList(requireJson(files, "disabled_mobs.json")));
        Map<String, List<String>> tiers = stringListMap(requireJson(files, "mob_tier.json"));
        Map<String, List<String>> biomeTypes = stringListMap(requireJson(files, "mob_biometypes.json"));
        Map<String, List<String>> extraSpawns = stringListMap(requireJson(files, "mob_extraspawns.json"));
        Map<String, List<String>> biomeMappings = stringListMap(requireJson(files, "biome_mappings.json"));
        Map<String, List<String>> mobTypes = stringListMap(requireJson(files, "mob_types.json"));
        Map<String, List<String>> spawnTypes = stringListMap(requireJson(files, "mob_overworldspawntype.json"));
        Set<String> water = new HashSet<String>(listOrEmpty(mobTypes, "watercreature"));
        Set<String> surface = new HashSet<String>(listOrEmpty(spawnTypes, "surface"));
        Set<String> underground = new HashSet<String>(listOrEmpty(spawnTypes, "underground"));

        Map<String, TierParams> params = defaultTierParams();
        String cfg = files.read("mob_spawnrules.cfg");
        if (cfg != null) {
            applyCfg(params, cfg);
        }

        Map<String, List<String>> structures = new LinkedHashMap<String, List<String>>();
        JsonElement structureJson = optionalJson(files, "mob_structurespawns.json");
        if (structureJson != null && structureJson.isJsonObject()) {
            structures = stringListMap(structureJson);
        }

        List<DepthsEntry> depths = new ArrayList<DepthsEntry>();
        JsonElement depthsJson = optionalJson(files, "mob_depthsspawns.json");
        if (depthsJson != null) {
            readDepths(depthsJson, disabled, depths);
        }
        Map<String, List<Integer>> dimensions = new LinkedHashMap<String, List<Integer>>();
        JsonElement dimensionJson = optionalJson(files, "biome_dimensions.json");
        if (dimensionJson != null && dimensionJson.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : dimensionJson.getAsJsonObject().entrySet()) {
                List<Integer> ids = new ArrayList<Integer>();
                JsonElement value = entry.getValue();
                if (value.isJsonArray()) {
                    for (JsonElement id : value.getAsJsonArray()) {
                        ids.add(id.getAsInt());
                    }
                } else {
                    ids.add(value.getAsInt());
                }
                dimensions.put(entry.getKey(), ids);
            }
        }
        return new SpawnTableSources(disabled, tiers, params, biomeTypes, extraSpawns, biomeMappings, dimensions,
                water, surface, underground, structures, depths);
    }

    private static Map<String, TierParams> defaultTierParams() {
        Map<String, TierParams> defaults = new LinkedHashMap<String, TierParams>();
        defaults.put("common", new TierParams(100, 2, 4, 16));
        defaults.put("uncommon", new TierParams(60, 1, 3, 10));
        defaults.put("rare", new TierParams(20, 1, 1, 5));
        defaults.put("elite", new TierParams(10, 1, 1, 3));
        defaults.put("deadly", new TierParams(7, 1, 1, 1));
        defaults.put("dragon", new TierParams(5, 1, 1, 1));
        defaults.put("boss", new TierParams(1, 1, 1, 1));
        return defaults;
    }

    private static void applyCfg(Map<String, TierParams> params, String text) {
        Matcher matcher = CFG_LINE.matcher(text);
        while (matcher.find()) {
            TierParams tier = params.get(matcher.group(1).toLowerCase(java.util.Locale.ROOT));
            if (tier == null) {
                continue;
            }
            int value = Integer.parseInt(matcher.group(3));
            switch (matcher.group(2).toLowerCase(java.util.Locale.ROOT)) {
                case "weight":
                    tier.weight = value;
                    break;
                case "group_min":
                    tier.groupMin = value;
                    break;
                case "group_max":
                    tier.groupMax = value;
                    break;
                case "limit":
                    tier.limit = value;
                    break;
                default:
                    break;
            }
        }
    }

    private static void readDepths(JsonElement json, Set<String> disabled, List<DepthsEntry> out) {
        if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                if (!disabled.contains(entry.getKey())) {
                    JsonObject config = entry.getValue().isJsonObject() ? entry.getValue().getAsJsonObject()
                            : new JsonObject();
                    out.add(depthsEntry(entry.getKey(), config));
                }
            }
        } else if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonObject() && element.getAsJsonObject().has("mob")
                        && !disabled.contains(element.getAsJsonObject().get("mob").getAsString())) {
                    out.add(depthsEntry(element.getAsJsonObject().get("mob").getAsString(),
                            element.getAsJsonObject()));
                } else if (element.isJsonPrimitive() && !disabled.contains(element.getAsString())) {
                    out.add(depthsEntry(element.getAsString(), new JsonObject()));
                }
            }
        }
    }

    private static DepthsEntry depthsEntry(String mob, JsonObject config) {
        return new DepthsEntry(mob, intOrNull(config, "weight"), intOrNull(config, "groupcountmin"),
                intOrNull(config, "groupcountmax"), intOrNull(config, "maxcount"), intOrNull(config, "maxheight"));
    }

    private static Integer intOrNull(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? Integer.valueOf(object.get(key).getAsInt()) : null;
    }

    private static JsonElement requireJson(FileReader files, String name) throws IOException {
        String text = files.read(name);
        if (text == null) {
            throw new IOException("missing " + name);
        }
        return LenientJson.parse(text);
    }

    private static JsonElement optionalJson(FileReader files, String name) throws IOException {
        String text = files.read(name);
        return text == null ? null : LenientJson.parse(text);
    }

    private static List<String> stringList(JsonElement element) {
        List<String> out = new ArrayList<String>();
        if (element != null && element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement value : array) {
                out.add(value.getAsString());
            }
        }
        return out;
    }

    private static Map<String, List<String>> stringListMap(JsonElement element) {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        if (element != null && element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                out.put(entry.getKey(), stringList(entry.getValue()));
            }
        }
        return out;
    }

    private static List<String> listOrEmpty(Map<String, List<String>> map, String key) {
        List<String> list = map.get(key);
        return list == null ? Collections.<String>emptyList() : list;
    }
}

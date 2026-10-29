package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One row of {@code spawn_conditions.json} ready to test against a {@link SpawnQuery}. The tests run in the order
 * In Control runs its checks, so the costly ones (structure, collision, counts) come last.
 */
final class ConditionRow {
    private static final int DAY_LENGTH = 24000;

    final Decision.Result result;
    /** False for a row that runs at the spawn check only. */
    final boolean bothPhases;
    final int index;
    final boolean random;

    private final Boolean hostile;
    private final Boolean spawner;
    private final Set<String> mobs;
    private final Float chance;
    private final Set<Integer> dimensions;
    private final Integer minTime;
    private final Integer maxTime;
    private final List<int[]> windows;
    private final Integer minHeight;
    private final Integer maxHeight;
    private final String weather;
    private final Double minPlayerDist;
    private final Double maxPlayerDist;
    private final Boolean seesSky;
    private final Set<String> biomes;
    private final String stage;
    private final String structure;
    private final Boolean notColliding;
    private final Set<String> mods;
    private final CountSpec minCount;
    private final CountSpec maxCount;

    private ConditionRow(JsonObject json, int index, Map<String, List<String>> categories) {
        this.index = index;
        this.result = Decision.Result.valueOf(json.get("result").getAsString().toUpperCase(Locale.ROOT));
        this.bothPhases = !(json.has("phase") && json.get("phase").getAsString().equals("spawn"));
        hostile = bool(json, "hostile");
        spawner = bool(json, "spawner");
        mobs = stringSet(json, "mob");
        mods = stringSet(json, "mod");
        chance = json.has("random") ? Float.valueOf(json.get("random").getAsString()) : null;
        random = chance != null;
        dimensions = json.has("dimension") ? intSet(json.get("dimension")) : null;
        minTime = json.has("mintime") ? Integer.valueOf(json.get("mintime").getAsInt()) : null;
        maxTime = json.has("maxtime") ? Integer.valueOf(json.get("maxtime").getAsInt()) : null;
        windows = json.has("time") ? windows(json.get("time")) : null;
        minHeight = json.has("minheight") ? Integer.valueOf(json.get("minheight").getAsInt()) : null;
        maxHeight = json.has("maxheight") ? Integer.valueOf(json.get("maxheight").getAsInt()) : null;
        weather = json.has("weather") ? json.get("weather").getAsString().toLowerCase(Locale.ROOT) : null;
        minPlayerDist = json.has("minplayerdist") ? Double.valueOf(json.get("minplayerdist").getAsDouble()) : null;
        maxPlayerDist = json.has("maxplayerdist") ? Double.valueOf(json.get("maxplayerdist").getAsDouble()) : null;
        seesSky = bool(json, "seesky");
        Set<String> biomeIds = stringSet(json, "biomereg");
        if (json.has("biomecat")) {
            if (biomeIds == null) {
                biomeIds = new HashSet<String>();
            }
            for (JsonElement name : list(json.get("biomecat"))) {
                List<String> inCategory = categories.get(name.getAsString());
                if (inCategory != null) {
                    biomeIds.addAll(inCategory);
                }
            }
        }
        biomes = biomeIds;
        stage = json.has("gamestage") ? json.get("gamestage").getAsString() : null;
        structure = json.has("structure") ? json.get("structure").getAsString() : null;
        notColliding = bool(json, "notcolliding");
        minCount = json.has("mincount") ? CountSpec.parse(json.get("mincount")) : null;
        maxCount = json.has("maxcount") ? CountSpec.parse(json.get("maxcount")) : null;
    }

    /** Builds a row from a validated rule; keys this class does not test (stats, gear, onjoin) are ignored. */
    static ConditionRow of(JsonObject json, int index, Map<String, List<String>> categories) {
        return new ConditionRow(json, index, categories);
    }

    boolean matches(SpawnQuery q) {
        if (hostile != null && q.hostile() != hostile) {
            return false;
        }
        if (spawner != null) {
            Boolean actual = q.spawner();
            if (actual == null || actual.booleanValue() != spawner.booleanValue()) {
                return false;
            }
        }
        if (mobs != null && !mobs.contains(q.mobId())) {
            return false;
        }
        if (chance != null && !(q.roll() < chance)) {
            return false;
        }
        if (dimensions != null && !dimensions.contains(q.dimension())) {
            return false;
        }
        int time = q.timeOfDay() % DAY_LENGTH;
        if (minTime != null && time < minTime) {
            return false;
        }
        if (maxTime != null && time > maxTime) {
            return false;
        }
        if (windows != null && !inWindow(time)) {
            return false;
        }
        if (minHeight != null && q.y() < minHeight) {
            return false;
        }
        if (maxHeight != null && q.y() > maxHeight) {
            return false;
        }
        if (weather != null && !(weather.startsWith("rain") ? q.raining() : q.thundering())) {
            return false;
        }
        if (minPlayerDist != null && q.anyPlayerWithin(minPlayerDist)) {
            return false;
        }
        if (maxPlayerDist != null && !q.anyPlayerWithin(maxPlayerDist)) {
            return false;
        }
        if (seesSky != null && q.seesSky() != seesSky) {
            return false;
        }
        if (biomes != null && !biomes.contains(q.biomeId())) {
            return false;
        }
        if (stage != null && !q.hasStage(stage)) {
            return false;
        }
        if (structure != null && !q.inStructure(structure)) {
            return false;
        }
        if (notColliding != null && q.notColliding() != notColliding) {
            return false;
        }
        if (mods != null && !mods.contains(q.modId())) {
            return false;
        }
        if (minCount != null && q.count(minCount) < q.scaledAmount(minCount)) {
            return false;
        }
        return maxCount == null || q.count(maxCount) < q.scaledAmount(maxCount);
    }

    private boolean inWindow(int time) {
        for (int[] window : windows) {
            if (time >= window[0] && time <= window[1]) {
                return true;
            }
        }
        return false;
    }

    private static List<int[]> windows(JsonElement value) {
        List<int[]> out = new ArrayList<int[]>();
        if (value.isJsonPrimitive()) {
            if (value.getAsString().equals("day")) {
                out.add(new int[] {0, 13000});
                out.add(new int[] {23000, 24000});
            } else {
                out.add(new int[] {13000, 23000});
            }
            return out;
        }
        for (JsonElement window : value.getAsJsonArray()) {
            JsonArray pair = window.getAsJsonArray();
            out.add(new int[] {pair.get(0).getAsInt(), pair.get(1).getAsInt()});
        }
        return out;
    }

    private static List<JsonElement> list(JsonElement value) {
        List<JsonElement> out = new ArrayList<JsonElement>();
        if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                out.add(element);
            }
        } else {
            out.add(value);
        }
        return out;
    }

    private static Boolean bool(JsonObject json, String key) {
        return json.has(key) ? Boolean.valueOf(json.get(key).getAsBoolean()) : null;
    }

    private static Set<String> stringSet(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        Set<String> out = new HashSet<String>();
        for (JsonElement element : list(json.get(key))) {
            out.add(element.getAsString());
        }
        return out;
    }

    private static Set<Integer> intSet(JsonElement value) {
        Set<Integer> out = new HashSet<Integer>();
        for (JsonElement element : list(value)) {
            out.add(element.getAsInt());
        }
        return out;
    }
}

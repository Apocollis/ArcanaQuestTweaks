package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What each rule file accepts: the keys, the type of each value, and the values that are allowed. Every mistake in a
 * rule is reported with the key it is about, so a misspelled key or a bad value is found at load, not as a mob that
 * silently never spawns.
 */
public final class RuleSchema {
    private enum Type {
        INT, NUMBER, BOOL, STRING, MOBS, DIMENSIONS, TIME, COUNT, RESULT, PHASE, POTIONS, ITEM, CATEGORIES,
        BIOMES, CHANCE, WEATHER, GENERATE
    }

    private static final Pattern ID = Pattern.compile("[a-z0-9_.\\-]+:[a-z0-9_./\\-]+");
    private static final Pattern POTION = Pattern.compile("[a-z0-9_.\\-]+:[a-z0-9_./\\-]+,-?\\d+,-?\\d+");

    public static final Set<String> CONDITION_GENERATORS = new HashSet<String>(Arrays.asList(
            "structure_allow", "depths_bypass", "stage_gating", "layer_restrictions", "day_spawns"));
    public static final Set<String> MODIFIER_GENERATORS = new HashSet<String>(Arrays.asList(
            "tier_underground", "dimension_buffs", "underground_tiers"));

    private static final Map<String, Type> MATCH = new LinkedHashMap<String, Type>();
    private static final Map<String, Type> CONDITIONS = new LinkedHashMap<String, Type>();
    private static final Map<String, Type> MODIFIERS = new LinkedHashMap<String, Type>();
    private static final Map<String, Type> GEAR = new LinkedHashMap<String, Type>();
    public static final List<String> ACTION_KEYS;
    public static final List<String> SLOT_KEYS = Arrays.asList("hand", "offhand", "head", "chest", "legs", "feet");

    static {
        MATCH.put("mob", Type.MOBS);
        MATCH.put("mod", Type.STRING);
        MATCH.put("dimension", Type.DIMENSIONS);
        MATCH.put("minheight", Type.INT);
        MATCH.put("maxheight", Type.INT);
        MATCH.put("seesky", Type.BOOL);
        MATCH.put("mintime", Type.INT);
        MATCH.put("maxtime", Type.INT);
        MATCH.put("time", Type.TIME);
        MATCH.put("weather", Type.WEATHER);
        MATCH.put("structure", Type.STRING);
        MATCH.put("biomereg", Type.BIOMES);
        MATCH.put("biomecat", Type.CATEGORIES);
        MATCH.put("hostile", Type.BOOL);
        MATCH.put("random", Type.CHANCE);

        CONDITIONS.putAll(MATCH);
        CONDITIONS.put("spawner", Type.BOOL);
        CONDITIONS.put("notcolliding", Type.BOOL);
        CONDITIONS.put("gamestage", Type.STRING);
        CONDITIONS.put("minplayerdist", Type.NUMBER);
        CONDITIONS.put("maxplayerdist", Type.NUMBER);
        CONDITIONS.put("mincount", Type.COUNT);
        CONDITIONS.put("maxcount", Type.COUNT);
        CONDITIONS.put("result", Type.RESULT);
        CONDITIONS.put("phase", Type.PHASE);
        CONDITIONS.put("generate", Type.GENERATE);

        MODIFIERS.putAll(MATCH);
        List<String> actions = new ArrayList<String>();
        for (String stat : new String[] {"health", "damage", "speed", "armor", "armortoughness",
                "knockbackresistance", "followrange", "flyingspeed"}) {
            MODIFIERS.put(stat + "multiply", Type.NUMBER);
            MODIFIERS.put(stat + "add", Type.NUMBER);
            actions.add(stat + "multiply");
            actions.add(stat + "add");
        }
        MODIFIERS.put("potion", Type.POTIONS);
        MODIFIERS.put("angry", Type.BOOL);
        actions.add("potion");
        actions.add("angry");
        ACTION_KEYS = java.util.Collections.unmodifiableList(actions);
        MODIFIERS.put("stop", Type.BOOL);
        MODIFIERS.put("generate", Type.GENERATE);

        for (String key : new String[] {"mob", "dimension", "minheight", "maxheight", "seesky", "weather", "random"}) {
            GEAR.put(key, MATCH.get(key));
        }
        for (String slot : SLOT_KEYS) {
            GEAR.put(slot, Type.ITEM);
        }
    }

    private final Collection<String> categories;

    /** @param categories the biome category names of biome_mappings.json, which {@code biomecat} may use */
    public RuleSchema(Collection<String> categories) {
        this.categories = categories;
    }

    /** Keys a rule of this kind may carry. */
    public static Set<String> keys(RuleKind kind) {
        return table(kind).keySet();
    }

    private static Map<String, Type> table(RuleKind kind) {
        switch (kind) {
            case CONDITIONS:
                return CONDITIONS;
            case MODIFIERS:
                return MODIFIERS;
            default:
                return GEAR;
        }
    }

    /** Checks one rule and adds a problem for each mistake in it. */
    public void validate(RuleKind kind, JsonObject rule, String file, int line, List<RuleProblem> problems) {
        Map<String, Type> allowed = table(kind);
        for (Map.Entry<String, JsonElement> entry : rule.entrySet()) {
            String key = entry.getKey();
            Type type = allowed.get(key);
            if (type == null) {
                problems.add(new RuleProblem(file, line, "unknown key '" + key + "'" + suggestion(key, allowed.keySet())
                        + " (this file accepts: " + String.join(", ", allowed.keySet()) + ")"));
                continue;
            }
            String error = check(type, entry.getValue(), kind);
            if (error != null) {
                problems.add(new RuleProblem(file, line, "'" + key + "': " + error));
            }
        }
        if (rule.has("generate")) {
            if (rule.size() > 1) {
                problems.add(new RuleProblem(file, line, "a 'generate' row stands alone; it has other keys too"));
            }
            return;
        }
        switch (kind) {
            case CONDITIONS:
                if (!rule.has("result")) {
                    problems.add(new RuleProblem(file, line, "missing 'result' (allow, deny or default)"));
                }
                break;
            case MODIFIERS:
                if (!hasAny(rule, ACTION_KEYS)) {
                    problems.add(new RuleProblem(file, line, "this row changes nothing: give it a stat, 'potion' or 'angry'"));
                }
                break;
            default:
                if (!rule.has("mob")) {
                    problems.add(new RuleProblem(file, line, "missing 'mob'"));
                }
                if (!hasAny(rule, SLOT_KEYS)) {
                    problems.add(new RuleProblem(file, line, "this row equips nothing: give it hand, offhand, head, chest, legs or feet"));
                }
                break;
        }
        if (rule.has("mintime") && rule.has("time") || rule.has("maxtime") && rule.has("time")) {
            problems.add(new RuleProblem(file, line, "use either 'time' or 'mintime'/'maxtime', not both"));
        }
        if (rule.has("minheight") && rule.has("maxheight") && isInt(rule.get("minheight")) && isInt(rule.get("maxheight"))
                && rule.get("minheight").getAsInt() > rule.get("maxheight").getAsInt()) {
            problems.add(new RuleProblem(file, line, "'minheight' is above 'maxheight', so this row can never match"));
        }
    }

    private static boolean hasAny(JsonObject rule, List<String> keys) {
        for (String key : keys) {
            if (rule.has(key)) {
                return true;
            }
        }
        return false;
    }

    private String check(Type type, JsonElement value, RuleKind kind) {
        switch (type) {
            case INT:
                return isInt(value) ? null : "must be a whole number";
            case NUMBER:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? null : "must be a number";
            case BOOL:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? null : "must be true or false";
            case STRING:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && !value.getAsString().trim().isEmpty() ? null : "must be a non-empty string";
            case MOBS:
                return idList(value, "a mob id like modid:name, or a list of them");
            case BIOMES:
                return idList(value, "a biome id like modid:name, or a list of them");
            case DIMENSIONS:
                return dimensions(value);
            case TIME:
                return time(value);
            case COUNT:
                return count(value);
            case RESULT:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && Arrays.asList("allow", "deny", "default").contains(value.getAsString())
                        ? null : "must be \"allow\", \"deny\" or \"default\"";
            case PHASE:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && Arrays.asList("spawn", "both").contains(value.getAsString())
                        ? null : "must be \"spawn\" or \"both\"";
            case POTIONS:
                return potions(value);
            case ITEM:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && value.getAsString().contains(":") ? null : "must be an item id like modid:name";
            case CATEGORIES:
                return categories(value);
            case CHANCE:
                return chance(value);
            case WEATHER:
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && (value.getAsString().toLowerCase(java.util.Locale.ROOT).startsWith("rain")
                        || value.getAsString().toLowerCase(java.util.Locale.ROOT).startsWith("thunder"))
                        ? null : "must be \"rain\" or \"thunder\"";
            case GENERATE:
                Set<String> names = kind == RuleKind.CONDITIONS ? CONDITION_GENERATORS : MODIFIER_GENERATORS;
                if (kind == RuleKind.GEAR) {
                    return "gear rows are not generated";
                }
                return value.isJsonPrimitive() && names.contains(value.getAsString()) ? null
                        : "must be one of " + String.join(", ", names);
            default:
                return null;
        }
    }

    private static boolean isInt(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return false;
        }
        double d = value.getAsDouble();
        return d == Math.rint(d);
    }

    private static List<JsonElement> asList(JsonElement value) {
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

    private static String idList(JsonElement value, String what) {
        List<JsonElement> ids = asList(value);
        if (ids.isEmpty()) {
            return "the list is empty";
        }
        for (JsonElement id : ids) {
            if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString() || !ID.matcher(id.getAsString()).matches()) {
                return "must be " + what + (id.isJsonPrimitive() ? " (got " + id + ")" : "");
            }
        }
        return null;
    }

    private static String dimensions(JsonElement value) {
        List<JsonElement> ids = asList(value);
        if (ids.isEmpty()) {
            return "the list is empty";
        }
        for (JsonElement id : ids) {
            if (!isInt(id)) {
                return "must be a dimension number or a list of them";
            }
        }
        return null;
    }

    private static String time(JsonElement value) {
        if (value.isJsonPrimitive()) {
            String name = value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
            return name.equals("day") || name.equals("night") ? null
                    : "must be \"day\", \"night\" or a list of [from, to] windows";
        }
        if (!value.isJsonArray() || value.getAsJsonArray().size() == 0) {
            return "must be \"day\", \"night\" or a list of [from, to] windows";
        }
        for (JsonElement window : value.getAsJsonArray()) {
            if (!window.isJsonArray() || window.getAsJsonArray().size() != 2
                    || !isInt(window.getAsJsonArray().get(0)) || !isInt(window.getAsJsonArray().get(1))) {
                return "each window must be [from, to]";
            }
            int from = window.getAsJsonArray().get(0).getAsInt();
            int to = window.getAsJsonArray().get(1).getAsInt();
            if (from < 0 || to > 24000 || from > to) {
                return "a window [" + from + ", " + to + "] must satisfy 0 <= from <= to <= 24000";
            }
        }
        return null;
    }

    private static String count(JsonElement value) {
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isNumber()) {
                return isInt(value) ? null : "must be a whole number";
            }
            String[] parts = value.getAsString().split(",");
            try {
                Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                return "must be a number, \"amount,modid:mob\", or {\"amount\": n, ...}";
            }
            return parts.length <= 2 ? null : "must be \"amount\" or \"amount,modid:mob\"";
        }
        if (!value.isJsonObject()) {
            return "must be a number or {\"amount\": n, ...}";
        }
        JsonObject object = value.getAsJsonObject();
        if (!object.has("amount") || !isInt(object.get("amount"))) {
            return "needs a whole-number 'amount'";
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key.equals("amount")) {
                continue;
            }
            if (key.equals("mob")) {
                String error = idList(entry.getValue(), "a mob id or a list of them");
                if (error != null) {
                    return "mob: " + error;
                }
            } else if (key.equals("mod")) {
                if (!entry.getValue().isJsonPrimitive()) {
                    return "mod must be a mod id";
                }
            } else if (key.equals("perplayer") || key.equals("perchunk") || key.equals("hostile")
                    || key.equals("passive")) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isBoolean()) {
                    return key + " must be true or false";
                }
            } else {
                return "unknown count key '" + key + "'";
            }
        }
        if (isTrue(object, "perplayer") && isTrue(object, "perchunk")) {
            return "perplayer and perchunk cannot be combined";
        }
        if (isTrue(object, "hostile") && isTrue(object, "passive")) {
            return "hostile and passive cannot be combined";
        }
        if ((isTrue(object, "hostile") || isTrue(object, "passive")) && object.has("mob")) {
            return "hostile/passive cannot be combined with mob";
        }
        return null;
    }

    private static boolean isTrue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
    }

    private static String potions(JsonElement value) {
        List<JsonElement> list = asList(value);
        if (list.isEmpty()) {
            return "the list is empty";
        }
        for (JsonElement potion : list) {
            if (!potion.isJsonPrimitive() || !potion.getAsJsonPrimitive().isString()
                    || !POTION.matcher(potion.getAsString()).matches()) {
                return "each potion must be \"modid:effect,duration,amplifier\"" + (potion.isJsonPrimitive()
                        ? " (got " + potion + ")" : "");
            }
        }
        return null;
    }

    private String categories(JsonElement value) {
        List<JsonElement> names = asList(value);
        if (names.isEmpty()) {
            return "the list is empty";
        }
        for (JsonElement name : names) {
            if (!name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()) {
                return "must be a category name or a list of them";
            }
            if (!categories.contains(name.getAsString())) {
                return "no biome category '" + name.getAsString() + "' in biome_mappings.json"
                        + suggestion(name.getAsString(), categories);
            }
        }
        return null;
    }

    private static String chance(JsonElement value) {
        if (!value.isJsonPrimitive()) {
            return "must be a chance between 0 and 1";
        }
        double d;
        try {
            d = value.getAsDouble();
        } catch (NumberFormatException e) {
            return "must be a chance between 0 and 1";
        }
        return d > 0 && d <= 1 ? null : "must be above 0 and at most 1";
    }

    private static String suggestion(String word, Collection<String> candidates) {
        String best = null;
        int bestDistance = 3;
        for (String candidate : candidates) {
            int distance = distance(word, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best == null ? "" : " (did you mean '" + best + "'?)";
    }

    private static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }
        return previous[b.length()];
    }
}

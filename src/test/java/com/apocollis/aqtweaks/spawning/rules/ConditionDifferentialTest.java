package com.apocollis.aqtweaks.spawning.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Runs the pack's original {@code spawn.json} the way In Control does (every rule at the spawn check, the {@code onjoin}
 * rules at join, the first match deciding) beside the converted conditions table, over a large number of generated
 * situations, and requires the same outcome.
 */
class ConditionDifferentialTest {
    private static String resource(String name) throws IOException {
        try (InputStream in = ConditionDifferentialTest.class.getResourceAsStream("/spawntable/" + name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** In Control's two walks over the original rules. */
    private static final class Reference {
        final List<ConditionRow> rows = new ArrayList<ConditionRow>();
        final List<Boolean> hasResult = new ArrayList<Boolean>();
        final List<Boolean> onJoin = new ArrayList<Boolean>();

        Reference(List<JsonObject> rules) {
            for (JsonObject rule : rules) {
                JsonObject copy = LenientJson.parse(rule.toString()).getAsJsonObject();
                hasResult.add(rule.has("result"));
                onJoin.add(rule.has("onjoin") && rule.get("onjoin").getAsBoolean());
                if (!copy.has("result")) {
                    copy.addProperty("result", "default");
                }
                copy.remove("phase");
                rows.add(ConditionRow.of(copy, rows.size(), new HashMap<String, List<String>>()));
            }
        }

        /** The result a spawn check leaves on the event; DEFAULT when no rule gave one. */
        Decision.Result spawnCheck(SpawnQuery q) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).matches(q)) {
                    return hasResult.get(i) ? rows.get(i).result : Decision.Result.DEFAULT;
                }
            }
            return Decision.Result.DEFAULT;
        }

        boolean joinCancelled(SpawnQuery q) {
            for (int i = 0; i < rows.size(); i++) {
                if (onJoin.get(i) && rows.get(i).matches(q)) {
                    return hasResult.get(i) && rows.get(i).result == Decision.Result.DENY;
                }
            }
            return false;
        }
    }

    private static final String[] DIMENSIONS_AS_TEXT = {"0", "-1", "1", "17", "-2", "10", "7", "4"};
    private static final int[] YS = {-70, -31, -30, -16, -15, 0, 15, 16, 49, 50, 55, 56, 59, 60, 61, 70, 120};
    private static final int[] TIMES = {0, 6000, 12999, 13000, 18000, 22999, 23000, 23999};
    private static final String[] BIOMES = {"minecraft:plains", "biomesoplenty:ominous_woods", "minecraft:ocean"};
    private static final String[][] STRUCTURES = {{}, {"Village"}, {"Mineshaft"}, {"Village", "Mineshaft"}};
    private static final String[][] STAGES = {{}, {"elite"}, {"dragon"}, {"deadly", "elite"}, {"crimsoncult"},
            {"elite", "dragon", "deadly", "crimsoncult"}};
    private static final int[] COUNTS = {0, 3, 12, 30, 60, 300};

    private static FakeQuery scenario(Random rnd, List<String> mobs, boolean spawnCheck, float roll) {
        FakeQuery q = new FakeQuery();
        q.mob = mobs.get(rnd.nextInt(mobs.size()));
        q.hostile = rnd.nextInt(4) != 0;
        q.spawner = spawnCheck ? Boolean.valueOf(rnd.nextInt(6) == 0) : null;
        q.dimension = Integer.parseInt(DIMENSIONS_AS_TEXT[rnd.nextInt(DIMENSIONS_AS_TEXT.length)]);
        q.y = YS[rnd.nextInt(YS.length)];
        q.time = TIMES[rnd.nextInt(TIMES.length)];
        int weather = rnd.nextInt(4);
        q.raining = weather == 1;
        q.thundering = weather == 2;
        q.sky = rnd.nextBoolean();
        q.biome = BIOMES[rnd.nextInt(BIOMES.length)];
        q.structures.addAll(Arrays.asList(STRUCTURES[rnd.nextInt(STRUCTURES.length)]));
        q.notColliding = rnd.nextInt(5) != 0;
        q.stages.addAll(Arrays.asList(STAGES[rnd.nextInt(STAGES.length)]));
        q.playerNear = rnd.nextBoolean();
        q.count = COUNTS[rnd.nextInt(COUNTS.length)];
        q.players = 1 + rnd.nextInt(3);
        q.roll = roll;
        return q;
    }


    /** A situation built to satisfy the keys of one original rule, then nudged, so rare rules are exercised. */
    private static FakeQuery directed(Random rnd, JsonObject rule, boolean spawnCheck, float roll) {
        FakeQuery q = scenario(rnd, List.of("minecraft:zombie"), spawnCheck, roll);
        if (rule.has("mob")) {
            List<String> names = new ArrayList<String>();
            if (rule.get("mob").isJsonArray()) {
                rule.get("mob").getAsJsonArray().forEach(m -> names.add(m.getAsString()));
            } else {
                names.add(rule.get("mob").getAsString());
            }
            q.mob = names.get(rnd.nextInt(names.size()));
        }
        if (rule.has("dimension")) {
            com.google.gson.JsonElement d = rule.get("dimension");
            q.dimension = d.isJsonArray() ? d.getAsJsonArray().get(rnd.nextInt(d.getAsJsonArray().size())).getAsInt()
                    : d.getAsInt();
        }
        if (rule.has("minheight") || rule.has("maxheight")) {
            int lo = rule.has("minheight") ? rule.get("minheight").getAsInt() : -64;
            int hi = rule.has("maxheight") ? rule.get("maxheight").getAsInt() : 200;
            int[] picks = {lo, hi, lo - 1, hi + 1, (lo + hi) / 2};
            q.y = picks[rnd.nextInt(picks.length)];
        }
        if (rule.has("mintime") || rule.has("maxtime")) {
            int lo = rule.has("mintime") ? rule.get("mintime").getAsInt() : 0;
            int hi = rule.has("maxtime") ? rule.get("maxtime").getAsInt() : 23999;
            int[] picks = {lo, hi, lo - 1, hi + 1, (lo + hi) / 2};
            q.time = Math.floorMod(picks[rnd.nextInt(picks.length)], 24000);
        }
        if (rule.has("seesky")) {
            q.sky = rule.get("seesky").getAsBoolean();
        }
        if (rule.has("hostile")) {
            q.hostile = rule.get("hostile").getAsBoolean();
        }
        if (rule.has("spawner") && spawnCheck) {
            q.spawner = rule.get("spawner").getAsBoolean();
        }
        if (rule.has("notcolliding")) {
            q.notColliding = rule.get("notcolliding").getAsBoolean();
        }
        if (rule.has("structure")) {
            q.structures.add(rule.get("structure").getAsString());
        }
        if (rule.has("biomereg")) {
            q.biome = rule.get("biomereg").isJsonArray() ? rule.get("biomereg").getAsJsonArray().get(0).getAsString()
                    : rule.get("biomereg").getAsString();
        }
        if (rule.has("gamestage")) {
            q.stages.add(rule.get("gamestage").getAsString());
        }
        if (rule.has("weather")) {
            q.thundering = rule.get("weather").getAsString().startsWith("thunder");
            q.raining = !q.thundering;
        }
        if (rule.has("minplayerdist")) {
            q.playerNear = false;
        }
        if (rule.has("maxplayerdist")) {
            q.playerNear = true;
        }
        for (String key : new String[] {"mincount", "maxcount"}) {
            if (rule.has(key)) {
                int amount = rule.get(key).isJsonObject() ? rule.get(key).getAsJsonObject().get("amount").getAsInt()
                        : rule.get(key).isJsonPrimitive() && rule.get(key).getAsJsonPrimitive().isNumber()
                        ? rule.get(key).getAsInt() : 5;
                int[] picks = {Math.max(0, amount * q.players - 1), amount * q.players, amount * q.players + 1};
                q.count = picks[rnd.nextInt(picks.length)];
            }
        }
        if (rnd.nextInt(4) == 0) {
            FakeQuery other = scenario(rnd, List.of(q.mob), spawnCheck, roll);
            switch (rnd.nextInt(6)) {
                case 0:
                    q.dimension = other.dimension;
                    break;
                case 1:
                    q.y = other.y;
                    break;
                case 2:
                    q.time = other.time;
                    break;
                case 3:
                    q.sky = other.sky;
                    break;
                case 4:
                    q.stages = other.stages;
                    break;
                default:
                    q.structures = other.structures;
                    break;
            }
        }
        return q;
    }

    private static void compare(float roll, long seed) throws IOException {
        String text = resource("spawn.json");
        List<RuleProblem> problems = new ArrayList<RuleProblem>();
        List<JsonObject> originals = new ArrayList<JsonObject>();
        for (RawRule raw : RuleScanner.scan(text, "spawn.json", problems)) {
            originals.add(LenientJson.parse(raw.text).getAsJsonObject());
        }
        assertEquals(List.of(), problems);
        Reference reference = new Reference(originals);

        SpawnJsonConverter.Converted converted = SpawnJsonConverter.convert(text, new ArrayList<RuleProblem>());
        RuleFileLoader.Result loaded = RuleFileLoader.load(RuleKind.CONDITIONS, converted.conditionsText(), List.of());
        assertEquals(List.of(), loaded.problems);
        ConditionTable table = ConditionTable.compile(loaded.rows, new HashMap<String, List<String>>());

        Set<String> mobSet = new LinkedHashSet<String>(List.of("minecraft:zombie", "minecraft:skeleton",
                "minecraft:creeper", "minecraft:squid", "lycanitesmobs:unlisted"));
        for (JsonObject rule : originals) {
            if (rule.has("mob")) {
                if (rule.get("mob").isJsonArray()) {
                    rule.get("mob").getAsJsonArray().forEach(m -> mobSet.add(m.getAsString()));
                } else {
                    mobSet.add(rule.get("mob").getAsString());
                }
            }
        }
        List<String> mobs = new ArrayList<String>(mobSet);

        Random rnd = new Random(seed);
        Map<String, Integer> kinds = new LinkedHashMap<String, Integer>();
        Map<String, Integer> outcomes = new LinkedHashMap<String, Integer>();
        Set<Integer> reached = new java.util.TreeSet<Integer>();
        List<String> examples = new ArrayList<String>();
        int runs = 300000;
        for (int n = 0; n < runs; n++) {
            boolean spawnCheck = rnd.nextBoolean();
            FakeQuery q = rnd.nextBoolean() ? scenario(rnd, mobs, spawnCheck, roll)
                    : directed(rnd, originals.get(rnd.nextInt(originals.size())), spawnCheck, roll);
            String ours;
            String theirs;
            if (spawnCheck) {
                Decision d = table.decide(ConditionTable.Phase.SPAWN, q);
                reached.add(d.row);
                ours = d.result == null ? "DEFAULT" : d.result.toString();
                theirs = reference.spawnCheck(q).toString();
            } else {
                Decision d = table.decide(ConditionTable.Phase.JOIN, q);
                reached.add(d.row);
                ours = d.result == Decision.Result.DENY ? "CANCEL" : "ok";
                theirs = reference.joinCancelled(q) ? "CANCEL" : "ok";
            }
            outcomes.merge((spawnCheck ? "spawn " : "join ") + theirs, 1, Integer::sum);
            if (!ours.equals(theirs)) {
                String kind = (spawnCheck ? "spawn " : "join ") + "tweaks=" + ours + " incontrol=" + theirs;
                kinds.merge(kind, 1, Integer::sum);
                if (examples.size() < 12) {
                    examples.add(kind + " :: " + q.mob + " dim " + q.dimension + " y " + q.y + " t " + q.time
                            + " sky " + q.sky + " spawner " + q.spawner + " biome " + q.biome + " hostile " + q.hostile
                            + " structs " + q.structures + " stages " + q.stages + " count " + q.count);
                }
            }
        }
        List<Integer> unreached = new ArrayList<Integer>();
        for (int row = 0; row < table.size(); row++) {
            if (!reached.contains(row)) {
                unreached.add(row);
            }
        }
        System.out.println("roll " + roll + ": unreached condition rows " + unreached + " of " + table.size());
        List<Integer> expected = roll == 0.0f ? List.of(38, 39) : List.of(35, 38, 39);
        assertEquals(expected, unreached, "rows that never decide: 38 and 39 are shadowed by broader earlier rules,"
                + " 35 is the random Atum throttle");
        for (String needed : new String[] {"spawn ALLOW", "spawn DENY", "spawn DEFAULT", "join CANCEL", "join ok"}) {
            assertTrue(outcomes.getOrDefault(needed, 0) > 200, "too few '" + needed + "' scenarios: " + outcomes);
        }
        assertTrue(kinds.isEmpty(), "mismatches (roll " + roll + "): " + kinds + "\n" + String.join("\n", examples));
    }

    @Test
    void convertedConditionsDecideLikeTheOriginalSpawnJsonWhenRandomRowsPass() throws IOException {
        compare(0.0f, 1L);
    }

    @Test
    void convertedConditionsDecideLikeTheOriginalSpawnJsonWhenRandomRowsFail() throws IOException {
        compare(0.9999f, 2L);
    }
}

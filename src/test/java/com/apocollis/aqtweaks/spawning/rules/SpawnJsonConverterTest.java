package com.apocollis.aqtweaks.spawning.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpawnJsonConverterTest {
    private static final List<String> CATEGORIES = List.of("spooky", "desert");

    private static String resource(String name) throws IOException {
        try (InputStream in = SpawnJsonConverterTest.class.getResourceAsStream("/spawntable/" + name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static SpawnJsonConverter.Converted convert(String text) {
        List<RuleProblem> problems = new ArrayList<RuleProblem>();
        SpawnJsonConverter.Converted converted = SpawnJsonConverter.convert(text, problems);
        assertEquals(List.of(), problems);
        return converted;
    }

    private static JsonObject find(List<SpawnJsonConverter.Row> rows, String key, String value) {
        for (SpawnJsonConverter.Row row : rows) {
            if (row.json.has(key) && row.json.get(key).isJsonPrimitive() && value.equals(row.json.get(key).getAsString())) {
                return row.json;
            }
        }
        return null;
    }

    @Test
    void thePacksSpawnJsonConvertsToThreeFilesThatLoadCleanly() throws IOException {
        SpawnJsonConverter.Converted converted = convert(resource("spawn.json"));
        assertEquals(78, converted.sourceRules);
        assertEquals(48, converted.conditions.size());
        assertEquals(34, converted.modifiers.size());
        assertEquals(9, converted.gear.size());
        for (RuleKind kind : RuleKind.values()) {
            String text = kind == RuleKind.CONDITIONS ? converted.conditionsText()
                    : kind == RuleKind.MODIFIERS ? converted.modifiersText() : converted.gearText();
            RuleFileLoader.Result loaded = RuleFileLoader.load(kind, text, CATEGORIES);
            assertEquals(List.of(), loaded.problems, kind.toString());
        }
        assertEquals(48, RuleFileLoader.load(RuleKind.CONDITIONS, converted.conditionsText(), CATEGORIES).rows.size());
    }

    @Test
    void phaseComesFromEachRulesOwnOnjoinFlag() throws IOException {
        SpawnJsonConverter.Converted converted = convert(resource("spawn.json"));
        int spawnOnly = 0;
        for (SpawnJsonConverter.Row row : converted.conditions) {
            if (row.json.has("phase")) {
                assertEquals("spawn", row.json.get("phase").getAsString());
                spawnOnly++;
            }
        }
        // 19 of the 78 rules are not onjoin and every one of them is a condition row
        assertEquals(19, spawnOnly);
        JsonObject spawnerCage = find(converted.conditions, "notcolliding", "false");
        assertEquals("spawn", spawnerCage.get("phase").getAsString());
        JsonObject village = converted.conditions.get(3).json;
        assertEquals("Village", village.get("structure").getAsString());
        assertFalse(village.has("phase"));
    }

    @Test
    void everyConvertedModifierRowStopsSoTheFirstVersionDecidesAsToday() throws IOException {
        SpawnJsonConverter.Converted converted = convert(resource("spawn.json"));
        for (SpawnJsonConverter.Row row : converted.modifiers) {
            assertTrue(row.json.has("stop") && row.json.get("stop").getAsBoolean(), row.json.toString());
        }
        JsonObject vex = find(converted.modifiers, "mob", "minecraft:vex");
        assertEquals(0.25, vex.get("healthmultiply").getAsDouble());
        assertEquals(0.5, vex.get("damagemultiply").getAsDouble());
        JsonObject mimic = find(converted.modifiers, "mob", "artifacts:mimic");
        assertEquals(0.75, mimic.get("healthmultiply").getAsDouble());
    }

    @Test
    void theCrimsonCultistRowsMoveAboveTheCultistUndergroundRow() throws IOException {
        SpawnJsonConverter.Converted converted = convert(resource("spawn.json"));
        int archer = -1;
        int underground = -1;
        for (int i = 0; i < converted.modifiers.size(); i++) {
            JsonObject json = converted.modifiers.get(i).json;
            if (json.has("mob") && json.get("mob").isJsonPrimitive()
                    && json.get("mob").getAsString().equals("crimsonrevelations:cultist_archer")) {
                archer = i;
            }
            if (json.has("mob") && json.get("mob").isJsonArray() && json.has("maxheight")
                    && json.get("mob").toString().contains("cultistcleric")) {
                underground = i;
            }
        }
        assertTrue(archer >= 0 && underground >= 0 && archer < underground, archer + " vs " + underground);
        boolean noted = false;
        for (String line : converted.report) {
            noted |= line.contains("cultist_archer") && line.contains("moved above");
        }
        assertTrue(noted, converted.report.toString());
    }

    @Test
    void gearRowsCarryEveryMobsSlotsAndKeepTheirHeightConditions() throws IOException {
        SpawnJsonConverter.Converted converted = convert(resource("spawn.json"));
        JsonObject vindicator = find(converted.gear, "mob", "minecraft:vindication_illager");
        assertEquals("minecraft:iron_axe", vindicator.get("hand").getAsString());
        assertEquals(2, vindicator.size());
        JsonObject archer = find(converted.gear, "mob", "crimsonrevelations:cultist_archer");
        for (String slot : List.of("hand", "head", "chest", "legs", "feet")) {
            assertTrue(archer.has(slot), slot);
        }
        int cursed = 0;
        for (SpawnJsonConverter.Row row : converted.gear) {
            if (row.json.get("mob").getAsString().equals("eerieentities:cursed_armor")) {
                cursed++;
                assertTrue(row.json.has("minheight") ^ row.json.has("maxheight"));
            }
        }
        assertEquals(2, cursed);
    }

    @Test
    void aRuleThatGatesAndChangesAMobKeepsBothPartsWhileAGateComesAfterIt() {
        String text = "[\n"
                + "{ \"mob\": \"a:b\", \"dimension\": 0, \"maxheight\": -30, \"result\": \"default\","
                + " \"healthmultiply\": 2.0, \"onjoin\": true },\n"
                + "{ \"mob\": \"a:b\", \"result\": \"deny\", \"onjoin\": true },\n"
                + "{ \"mob\": \"c:d\", \"result\": \"default\", \"healthmultiply\": 3.0, \"onjoin\": true }\n"
                + "]";
        SpawnJsonConverter.Converted converted = convert(text);
        assertEquals(2, converted.conditions.size());
        assertEquals("default", converted.conditions.get(0).json.get("result").getAsString());
        assertFalse(converted.conditions.get(0).json.has("healthmultiply"));
        assertEquals(2, converted.modifiers.size());
        // the third rule has no gate after it, so only its modifier row is written
        assertEquals("a:b", converted.conditions.get(1).json.get("mob").getAsString());
        assertEquals("c:d", converted.modifiers.get(1).json.get("mob").getAsString());
    }

    @Test
    void potionNoneIsDroppedAndUnknownKeysAreReported() {
        String text = "[\n"
                + "{ \"mob\": \"a:b\", \"potion\": \"none\", \"healthmultiply\": 2.0, \"onjoin\": true },\n"
                + "{ \"mob\": \"a:c\", \"result\": \"deny\", \"lostcities\": true }\n"
                + "]";
        SpawnJsonConverter.Converted converted = convert(text);
        assertFalse(converted.modifiers.get(0).json.has("potion"));
        boolean reported = false;
        for (String line : converted.report) {
            reported |= line.contains("lostcities") && line.contains("line 3");
        }
        assertTrue(reported, converted.report.toString());
        assertNotNull(converted.conditions.get(0));
    }

    @Test
    void theRuleFilesOfAPackAreCheckedTogetherWithItsBiomeCategories() throws IOException {
        SpawnJsonConverter.Converted converted = convert(resource("spawn.json"));
        java.util.Map<String, String> pack = new java.util.HashMap<String, String>();
        pack.put("biome_mappings.json", resource("biome_mappings.json"));
        pack.put("spawn_conditions.json", converted.conditionsText());
        pack.put("spawn_modifiers.json", converted.modifiersText());
        RuleFiles.Report report = RuleFiles.check(pack::get);
        assertTrue(report.ok(), report.lines.toString());
        assertEquals(2, report.filesPresent);
        assertEquals("spawn_gear.json: not present", report.lines.get(2));

        pack.put("spawn_gear.json", "[{ \"mob\": \"a:b\", \"hand\": \"x\" }]");
        pack.put("spawn_conditions.json", "[{ \"biomecat\": \"spoky\", \"result\": \"deny\" }]");
        RuleFiles.Report broken = RuleFiles.check(pack::get);
        assertFalse(broken.ok());
        assertEquals(2, broken.problems);
        assertTrue(broken.lines.toString().contains("spawn_conditions.json:1"), broken.lines.toString());
        assertTrue(broken.lines.toString().contains("spawn_gear.json:1"), broken.lines.toString());
    }
}

package com.apocollis.aqtweaks.spawning.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PotentialSpawnVerifierTest {
    private static final List<String> BIOMES = Arrays.asList(
            "minecraft:plains", "minecraft:desert", "minecraft:ocean", "minecraft:river", "minecraft:hell",
            "minecraft:sky", "beneath:the_beneath", "biomesoplenty:woodland");
    private static final List<String> TYPES = Arrays.asList("MONSTER", "WATER_CREATURE", "CREATURE", "AMBIENT");

    private static List<SpawnRuleSpec> table() throws IOException {
        return SpawnTableBuilder.build(SpawnTableBuilderTest.packSources());
    }

    private static JsonArray file() throws IOException {
        JsonElement parsed = LenientJson.parse(SpawnTableBuilderTest.resource("expected_potentialspawn.json"));
        return parsed.getAsJsonArray();
    }

    @Test
    void theTableMatchesThePacksPotentialspawnJsonForEveryQuery() throws IOException {
        PotentialSpawnVerifier.Report report = PotentialSpawnVerifier.compare(table(), file(), BIOMES, TYPES);
        assertEquals(List.of(), report.differences);
        assertEquals(List.of(), report.unsupportedFields);
        assertEquals(159, report.fileRules);
        assertEquals(159, report.tableRules);
        assertTrue(report.queriesChecked > 1000);
    }

    @Test
    void aChangedWeightIsReportedForTheBiomesThatMobSpawnsIn() throws IOException {
        JsonArray edited = file();
        JsonObject rule = edited.get(0).getAsJsonObject();
        rule.getAsJsonArray("mobs").get(0).getAsJsonObject().addProperty("weight", 99);
        PotentialSpawnVerifier.Report report = PotentialSpawnVerifier.compare(table(), edited, BIOMES, TYPES);
        assertFalse(report.identical());
        assertTrue(report.differences.get(0).contains("weight=99"), report.differences.get(0));
        assertTrue(report.differences.get(0).contains("only in the table"), report.differences.get(0));
    }

    @Test
    void aMissingRuleAndAnExtraHandWrittenRuleAreBothReported() throws IOException {
        JsonArray edited = file();
        edited.remove(0);
        JsonObject extra = LenientJson.parse("{\"dimension\": 0, \"biomereg\": \"minecraft:plains\","
                + " \"mintime\": 13000, \"creaturetype\": \"MONSTER\","
                + " \"mobs\": [{\"mob\": \"minecraft:zombie\", \"weight\": 5}]}").getAsJsonObject();
        edited.add(extra);
        PotentialSpawnVerifier.Report report = PotentialSpawnVerifier.compare(table(), edited, BIOMES, TYPES);
        assertFalse(report.identical());
        assertEquals(List.of("mintime (1 rules)"), report.unsupportedFields);
        boolean sawZombie = false;
        for (String difference : report.differences) {
            sawZombie |= difference.contains("add minecraft:zombie weight=5 group=1-1 mintime=13000");
        }
        assertTrue(sawZombie, report.differences.toString());
        assertEquals(159, report.fileRules);
    }

    @Test
    void idsAreComparedAfterNormalisingTheNamespaceAndCase() throws IOException {
        JsonArray edited = file();
        JsonObject rule = edited.get(0).getAsJsonObject();
        JsonArray biomes = rule.getAsJsonArray("biomereg");
        JsonArray upper = new JsonArray();
        for (JsonElement biome : biomes) {
            upper.add(new com.google.gson.JsonPrimitive(biome.getAsString().toUpperCase()));
        }
        rule.add("biomereg", upper);
        PotentialSpawnVerifier.Report report = PotentialSpawnVerifier.compare(table(), edited, BIOMES, TYPES);
        assertEquals(List.of(), report.differences);
    }

    @Test
    void aRemovalRuleIsComparedInOrder() {
        List<SpawnRuleSpec> rules = List.of(
                SpawnRuleSpec.spawn(10, null, null, null, "a:b", 1, 1, 1, 1, false),
                SpawnRuleSpec.removal(10, List.of("a:b")));
        JsonArray swapped = new JsonArray();
        swapped.add(rules.get(1).toJson());
        swapped.add(rules.get(0).toJson());
        PotentialSpawnVerifier.Report report = PotentialSpawnVerifier.compare(rules, swapped, BIOMES, TYPES);
        assertFalse(report.identical());
        assertTrue(report.differences.get(0).contains("different order"), report.differences.get(0));
    }
}

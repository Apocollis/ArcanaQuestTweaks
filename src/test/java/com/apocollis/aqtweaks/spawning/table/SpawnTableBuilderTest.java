package com.apocollis.aqtweaks.spawning.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpawnTableBuilderTest {

    static String resource(String name) throws IOException {
        try (InputStream in = SpawnTableBuilderTest.class.getResourceAsStream("/spawntable/" + name)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static SpawnTableSources packSources() throws IOException {
        return SpawnTableSources.load(SpawnTableBuilderTest::resource);
    }

    private static SpawnTableSources sources(Map<String, String> files) throws IOException {
        return SpawnTableSources.load(files::get);
    }

    private static Map<String, String> minimalPack() {
        Map<String, String> files = new HashMap<String, String>();
        files.put("disabled_mobs.json", "[\"x:off\"]");
        files.put("mob_tier.json", "{\"rare\": [\"x:rare\"], \"common\": [\"x:cave\"]}");
        files.put("mob_biometypes.json", "{\"forest\": [\"x:rare\", \"x:cave\", \"x:off\", \"x:lava\"],"
                + " \"nether\": [\"x:hot\"], \"dark\": [\"lycanitesmobs:shade\", \"x:deep\"], \"any\": []}");
        files.put("mob_extraspawns.json", "{\"lava\": [\"x:lava\"]}");
        files.put("biome_mappings.json", "{\"forest\": [\"minecraft:forest\", \"biomesoplenty:woodland\"]}");
        files.put("mob_types.json", "{\"watercreature\": [\"x:rare\"]}");
        files.put("mob_overworldspawntype.json", "{\"surface\": [], \"underground\": [\"x:cave\"]}");
        return files;
    }

    @Test
    void packFilesProduceTheScriptsPotentialspawnJson() throws IOException {
        JsonElement expected = LenientJson.parse(resource("expected_potentialspawn.json"));
        JsonArray built = SpawnTableBuilder.toJson(SpawnTableBuilder.build(packSources()));
        assertEquals(159, expected.getAsJsonArray().size());
        assertEquals(expected, built);
    }

    @Test
    void overworldRowsAreSortedAndUseTierValues() throws IOException {
        List<SpawnRuleSpec> rules = SpawnTableBuilder.build(sources(minimalPack()));
        // Overworld: x:cave, x:rare; Nether: x:hot; Beneath: the two dark mobs; then the Beneath removal row.
        assertEquals(6, rules.size());
        SpawnRuleSpec cave = rules.get(0);
        assertEquals("x:cave", cave.mob);
        assertEquals(110, cave.weight);
        assertEquals(2, cave.groupMin);
        assertEquals(4, cave.groupMax);
        assertEquals(16, cave.limit);
        assertEquals(List.of("biomesoplenty:woodland", "minecraft:forest"), cave.biomes);
        SpawnRuleSpec rare = rules.get(1);
        assertEquals("x:rare", rare.mob);
        assertTrue(rare.water);
        assertEquals(SpawnRuleSpec.WATER_CREATURE, rare.creatureType());
        assertEquals(20, rare.weight);
        assertEquals(5, rare.limit);
    }

    @Test
    void disabledAndExtraMobsAreLeftOut() throws IOException {
        for (SpawnRuleSpec rule : SpawnTableBuilder.build(sources(minimalPack()))) {
            assertFalse("x:off".equals(rule.mob));
            assertFalse("x:lava".equals(rule.mob));
        }
    }

    @Test
    void darkMobsGoToTheBeneathAndTheLastRowRemovesVanillaThere() throws IOException {
        List<SpawnRuleSpec> rules = SpawnTableBuilder.build(sources(minimalPack()));
        SpawnRuleSpec hot = rules.get(2);
        assertEquals("x:hot", hot.mob);
        assertEquals(-1, hot.dimension);
        SpawnRuleSpec last = rules.get(rules.size() - 1);
        assertTrue(last.isRemoval());
        assertEquals(10, last.dimension);
        assertTrue(last.remove.contains("minecraft:zombie"));
        assertTrue(last.appliesTo(10, "anything", SpawnRuleSpec.MONSTER));
        assertTrue(last.appliesTo(10, "anything", "CREATURE"));
        assertFalse(last.appliesTo(0, "anything", SpawnRuleSpec.MONSTER));
    }

    @Test
    void missingRequiredFileIsAnError() {
        Map<String, String> files = minimalPack();
        files.remove("mob_tier.json");
        try {
            sources(files);
            throw new AssertionError("expected an IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("mob_tier.json"));
        }
    }

    @Test
    void cfgOverridesTierValuesAndCommentsAreIgnored() throws IOException {
        Map<String, String> files = minimalPack();
        files.put("mob_tier.json", "{\"rare\": [\"x:rare\"]} // trailing comment");
        files.put("mob_spawnrules.cfg", "general {\n    I:common_weight=75\n    I:rare_limit=9\n    I:rare_weight=33\n}");
        files.put("mob_depthsspawns.json", "{\"x:deep\": {\"weight\": 5, /* c */ \"maxcount\": 2,}}");
        List<SpawnRuleSpec> rules = SpawnTableBuilder.build(sources(files));
        SpawnRuleSpec cave = rules.get(0);
        assertEquals("x:cave", cave.mob);
        assertEquals(85, cave.weight);
        SpawnRuleSpec rare = rules.get(1);
        assertEquals(9, rare.limit);
        assertEquals(33, rare.weight);
        SpawnRuleSpec depths = rules.get(rules.size() - 2);
        assertEquals("x:deep", depths.mob);
        assertEquals(Integer.valueOf(-30), depths.maxHeight);
        assertEquals(5, depths.weight);
        assertEquals(2, depths.limit);
    }

    @Test
    void aCategoryWithADimensionMakesRowsInThatDimension() throws IOException {
        Map<String, String> files = minimalPack();
        files.put("mob_biometypes.json", "{\"forest\": [\"x:cave\", \"x:both\"],"
                + " \"atum\": [\"x:brigand\", \"x:both\"], \"atum_oasis\": [\"x:croc\"], \"nether\": [], \"dark\": [], \"any\": []}");
        files.put("biome_mappings.json", "{\"forest\": [\"minecraft:forest\"], \"atum\": [\"atum:desert\", \"atum:oasis\"],"
                + " \"atum_oasis\": [\"atum:oasis\"]}");
        files.put("biome_dimensions.json", "{\"atum\": 17, \"atum_oasis\": [17]} // others are the Overworld");
        List<SpawnRuleSpec> rules = SpawnTableBuilder.build(sources(files));
        SpawnRuleSpec brigand = null;
        SpawnRuleSpec croc = null;
        int both = 0;
        for (SpawnRuleSpec rule : rules) {
            if ("x:brigand".equals(rule.mob)) {
                brigand = rule;
            } else if ("x:croc".equals(rule.mob)) {
                croc = rule;
            } else if ("x:both".equals(rule.mob)) {
                both++;
            }
        }
        assertEquals(17, brigand.dimension);
        assertEquals(List.of("atum:desert", "atum:oasis"), brigand.biomes);
        assertEquals(17, croc.dimension);
        assertEquals(List.of("atum:oasis"), croc.biomes);
        assertEquals(2, both);
        assertTrue(brigand.appliesTo(17, "atum:oasis", SpawnRuleSpec.MONSTER));
        assertFalse(brigand.appliesTo(0, "atum:oasis", SpawnRuleSpec.MONSTER));
        // x:both is in an Overworld category and an Atum one: one row for each dimension, Overworld first
        SpawnRuleSpec first = null;
        SpawnRuleSpec second = null;
        for (SpawnRuleSpec rule : rules) {
            if ("x:both".equals(rule.mob)) {
                if (first == null) {
                    first = rule;
                } else {
                    second = rule;
                }
            }
        }
        assertEquals(0, first.dimension);
        assertEquals(List.of("minecraft:forest"), first.biomes);
        assertEquals(17, second.dimension);
    }

    @Test
    void noDimensionsFileMeansEveryCategoryIsOverworld() throws IOException {
        for (SpawnRuleSpec rule : SpawnTableBuilder.build(sources(minimalPack()))) {
            assertTrue(rule.dimension == 0 || rule.dimension == -1 || rule.dimension == 10);
        }
    }
}

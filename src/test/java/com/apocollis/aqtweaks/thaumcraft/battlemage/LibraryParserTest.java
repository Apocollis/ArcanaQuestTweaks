package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LibraryParserTest {

    private static final String LINE = "<thaumcraft:focus_2>.withTag({package: {nodes: []}, color: 5})";

    static String bundledText() throws IOException {
        return bundled();
    }

    private static String bundled() throws IOException {
        try (InputStream in = LibraryParserTest.class.getResourceAsStream("/assets/aqtweaks/battlemage/cultist_battlemages.json")) {
            assertNotNull(in, "bundled default library is missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void bundledLibraryParsesWithoutProblems() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        assertTrue(d.problems.isEmpty(), d.problems.toString());
        assertEquals(20, d.foci.size());
        assertEquals(2, d.tiers.size());
        assertEquals(8, d.tier(1).stage1Foci.size());
        assertEquals(10, d.tier(2).stage1Foci.size());
        assertEquals(2, d.variants.size());
    }

    @Test
    void tiersCarryTheirOwnResolvedPacing() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        assertEquals(40, d.tier(1).ai.globalCastGapTicks);
        assertEquals(5.5D, d.tier(1).ai.budgetRegenPerSecond);
        assertEquals(40, d.tier(1).ai.startupDelayTicks);
        assertEquals(30, d.tier(2).ai.globalCastGapTicks);
        assertEquals(8.0D, d.tier(2).ai.budgetRegenPerSecond);
        assertEquals(30, d.tier(2).ai.startupDelayTicks);
        assertEquals(0.65D, d.tier(2).ai.resumeFraction);
        assertEquals(0.25D, d.tier(2).ai.holdBelowFraction);
        assertEquals(100.0D, d.tier(2).ai.budgetMax);
        // the tiers are copies: changing one does not touch the global block or the other tier
        assertEquals(24, d.ai.globalCastGapTicks);
        assertEquals(40, d.tier(1).ai.globalCastGapTicks);
        assertTrue(d.ai != d.tier(1).ai && d.tier(1).ai != d.tier(2).ai);
        assertTrue(d.tier(1).ai.castBudgetEnabled);
        assertEquals(0.6D, d.tier(1).ai.healAllyHpFraction);
        assertEquals(16.0D, d.tier(1).ai.healAllyRange);
        assertEquals(2.5D, d.tier(1).ai.healAllyCastDistance);
        assertEquals(60, d.tier(1).ai.healAllyCooldownTicks);
        assertEquals(64, d.tier(1).ai.volleyGapTicks);
        assertEquals(180, d.tier(1).ai.volleyPauseTicks);
        assertEquals(300, d.tier(1).ai.volleyMaxTicks);
        assertEquals(80, d.tier(1).ai.blinkCooldownTicks);
        assertEquals(16, d.tier(1).ai.blinkSamples);
        assertEquals(24.0D, d.tier(1).ai.blinkMaxTravel);
        assertEquals(1.0D, d.tier(1).ai.blinkAllyMinDistance);
        assertEquals(2.0D, d.tier(1).ai.blinkAllyMaxDistance);
        assertEquals(80, d.aiFor(1, "swarm_mage").blinkCooldownTicks);
    }

    @Test
    void theSwarmMageIsAFixedTierOneVariantWithAnEscort() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        VariantDef v = d.variant("swarm_mage");
        assertNotNull(v);
        assertEquals(1, v.tier);
        assertEquals(java.util.Arrays.asList("heal_self", "knock_back", "blink", "rift", "curse_bat", "spell_bat_swarm",
                "healing_aura", "flux_bolt", "prismatic_bolt"), v.fixedLoadout);
        for (String id : v.fixedLoadout) {
            assertNotNull(d.focus(id), id);
        }
        assertEquals(2.0D, v.roleWeight("bolt"));
        assertEquals(2.0D, v.roleWeight("bolt_heavy"));
        assertEquals(1.0D, v.roleWeight("aoe"));
        assertEquals(3, v.escort.size());
        assertEquals("thaumcraft:cultistknight", v.escort.get(0).entity);
        assertEquals(2, v.escort.get(0).count);
        assertEquals("thaumcraft:cultistcleric", v.escort.get(1).entity);
        assertEquals("crimsonrevelations:cultist_archer", v.escort.get(2).entity);
        // inherits the tier 1 tuning (40 tick gap)
        assertEquals(40, d.aiFor(1, "swarm_mage").globalCastGapTicks);
        assertTrue(d.aiFor(1, "swarm_mage").castBudgetEnabled);
        assertEquals(1, d.variant("swarm_mage").tier);
        // Healing Aura guard (plan 20)
        AiTuning ai = d.aiFor(1, "swarm_mage");
        assertEquals(3.5D, ai.cloudGuardRange);
        assertEquals(6.0D, ai.cloudGuardNearRange);
        assertEquals(20, ai.cloudGuardCastTicks);
        assertEquals(1, ai.cloudGuardMeleeMin);
        assertEquals(2, ai.cloudGuardMeleeMax);
        assertEquals(30, ai.cloudGuardCooldownTicks);
        assertEquals(20, ai.cloudGuardCloseTicks);
        // the guard keys are a Swarm Mage thing; the other variants keep the AiTuning defaults
        assertEquals(30, d.aiFor(2, "bombardier_mage").cloudGuardCooldownTicks);
    }

    @Test
    void theBombardierIsAFixedTierTwoVariantWithTheBudgetOffAndAShortAreaRange() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        VariantDef v = d.variant("bombardier_mage");
        assertNotNull(v);
        assertEquals(2, v.tier);
        assertEquals(9, v.fixedLoadout.size());
        for (String id : java.util.Arrays.asList("entropic_cloud", "fire_and_ice", "tempest", "sands_of_atum", "aurorian_winds")) {
            assertTrue(v.fixedLoadout.contains(id), id);
        }
        for (String id : v.fixedLoadout) {
            assertTrue("aoe".equals(d.focus(id).slot) || "fixed".equals(d.focus(id).slot) || d.focus(id).performed, id);
        }
        AiTuning ai = d.aiFor(2, "bombardier_mage");
        assertFalse(ai.castBudgetEnabled);
        assertEquals(6.0D, ai.aoeMinRange);
        assertEquals(64, ai.volleyGapTicks);
        assertEquals(180, ai.volleyPauseTicks);
        assertEquals(300, ai.volleyMaxTicks);
        // close response and band blink (plan 19.8)
        assertEquals(1, ai.closeMeleeMin);
        assertEquals(2, ai.closeMeleeMax);
        assertEquals(160, ai.closeSequenceMaxTicks);
        assertEquals(8.0D, ai.blinkBandMinDistance);
        assertEquals(14.0D, ai.blinkBandMaxDistance);
        assertEquals(0.75D, ai.cloudAvoidMargin);
        assertEquals(6.0D, ai.enemyCloudCost);
        // the rest of the tier 2 tuning carries through
        assertEquals(8.0D, ai.budgetRegenPerSecond);
        assertEquals(30, ai.startupDelayTicks);
        assertTrue(d.aiFor(2, null).castBudgetEnabled);
        assertEquals(14.0D, d.aiFor(2, null).aoeMinRange);
    }

    @Test
    void bundledGearIsTheCrimsonPaladinSetWithRegistryNamedEnchantments() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        for (int tier = 1; tier <= 2; tier++) {
            java.util.Map<String, EquipmentDef> eq = d.tier(tier).equipment;
            assertEquals("crimsonrevelations:crimson_paladin_helmet", eq.get("head").item);
            assertEquals("crimsonrevelations:crimson_paladin_chestplate", eq.get("chest").item);
            assertEquals("crimsonrevelations:crimson_paladin_leggings", eq.get("legs").item);
            assertEquals("thaumcraft:crimson_boots", eq.get("feet").item);
            assertEquals("thaumcraft:crimson_blade", eq.get("mainhand").item);
            assertEquals("thaumcraft:caster_basic", eq.get("offhand").item);
            assertEquals(6, eq.size());
            for (EquipmentDef def : eq.values()) {
                assertNotNull(def.nbt, def.item);
                assertSnbtIsPlausible(def.nbt);
            }
            assertTrue(eq.get("head").nbt.contains("quality.mindful.name"));
            assertTrue(eq.get("chest").nbt.contains("quality.elite.name"));
            assertTrue(eq.get("feet").nbt.contains("quality.nimble.name"));
            assertTrue(eq.get("mainhand").nbt.contains("Crimson Battlemage's Blade"));
            assertTrue(eq.get("mainhand").nbt.contains("quality.serrated.name"));
            assertTrue(eq.get("offhand").nbt.contains("quality.mystical.name"));
            assertEquals(5, eq.get("mainhand").enchantments.size());
            assertEquals("msmlegacy:scorn", eq.get("mainhand").enchantments.get(0).id);
            assertEquals(1, eq.get("mainhand").enchantments.get(0).level);
            assertEquals(3, eq.get("mainhand").enchantments.get(4).level);
            assertTrue(eq.get("head").enchantments.isEmpty());
        }
    }

    /** SNBT, not ZenScript: no "as long", balanced braces and brackets outside strings, long values carry the L suffix. */
    private static void assertSnbtIsPlausible(String snbt) {
        assertFalse(snbt.contains(" as long"), snbt);
        assertFalse(snbt.contains(" as short"), snbt);
        int curly = 0;
        int square = 0;
        boolean inString = false;
        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (c == '\\' && inString) {
                i++;
                continue;
            }
            if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    curly++;
                } else if (c == '}') {
                    curly--;
                } else if (c == '[') {
                    square++;
                } else if (c == ']') {
                    square--;
                }
                assertTrue(curly >= 0 && square >= 0, snbt);
            }
        }
        assertFalse(inString, "unterminated string: " + snbt);
        assertEquals(0, curly, snbt);
        assertEquals(0, square, snbt);
        assertTrue(snbt.startsWith("{") && snbt.endsWith("}"), snbt);
        assertTrue(snbt.matches("(?s).*UUIDMost: -?\\d+L.*"), "UUIDMost must carry the L suffix: " + snbt);
    }

    @Test
    void variantsAndEquipmentParseLeniently() {
        String json = "{\"foci\":[],\"tiers\":[{\"tier\":1,\"equipment\":{\"head\":\"minecraft:iron_helmet\","
                + "\"chest\":{\"item\":\"minecraft:iron_chestplate\",\"nbt\":\"{a: 1}\",\"enchantments\":[{\"id\":\"minecraft:protection\",\"lvl\":2},{\"lvl\":1}]},"
                + "\"legs\":{\"nbt\":\"{}\"}}}],"
                + "\"variants\":[{\"id\":\"ok\",\"tier\":1,\"weights\":{\"bolt\":3,\"nonsense\":2},\"ai\":{\"globalCastGapTicks\":99}},"
                + "{\"id\":\"ok\",\"tier\":1},{\"id\":\"ghost\",\"tier\":7},{\"tier\":1}]}";
        LibraryData d = LibraryParser.parse(json);
        EquipmentDef head = d.tier(1).equipment.get("head");
        assertEquals("minecraft:iron_helmet", head.item);
        assertNull(head.nbt);
        EquipmentDef chest = d.tier(1).equipment.get("chest");
        assertEquals("{a: 1}", chest.nbt);
        assertEquals(1, chest.enchantments.size());
        assertEquals(2, chest.enchantments.get(0).level);
        assertNull(d.tier(1).equipment.get("legs"));
        assertEquals(1, d.variants.size());
        assertEquals(99, d.aiFor(1, "ok").globalCastGapTicks);
        assertEquals(24, d.aiFor(1, null).globalCastGapTicks);
        assertEquals(3.0D, d.variant("ok").roleWeight("bolt"));
        // problems: the enchantment with no id, the legs entry with no item, the unknown role weight, the duplicate id,
        // the unknown tier, the variant with no id
        assertEquals(6, d.problems.size(), d.problems.toString());
    }

    @Test
    void aVariantNeverMutatesItsTiersTuning() {
        LibraryData d = LibraryParser.parse("{\"ai\":{\"globalCastGapTicks\":10},\"tiers\":[{\"tier\":1,\"ai\":{\"startupDelayTicks\":7}}],"
                + "\"variants\":[{\"id\":\"v\",\"tier\":1,\"ai\":{\"startupDelayTicks\":99,\"castBudget\":{\"enabled\":false}}}]}");
        assertEquals(7, d.tier(1).ai.startupDelayTicks);
        assertEquals(99, d.aiFor(1, "v").startupDelayTicks);
        assertEquals(10, d.aiFor(1, "v").globalCastGapTicks);
        assertTrue(d.tier(1).ai.castBudgetEnabled);
        assertFalse(d.aiFor(1, "v").castBudgetEnabled);
        assertEquals(40, d.ai.startupDelayTicks);
    }

    @Test
    void bundledFociCarryNaturalComplexities() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        assertEquals(25, d.focus("frost").complexity);
        assertEquals(50, d.focus("entropic_cloud").complexity);
        assertEquals(17, d.focus("knock_back").complexity);
        assertTrue(d.focus("blink").performed);
        assertFalse(d.focus("frost").performed);
        assertNull(d.focus("frost").cooldownTicks);
        assertEquals(2, d.focus("aethers_bolt").minTier);
        assertEquals(4, d.focus("aethers_bolt").dimensions[0]);
    }

    @Test
    void tierFocusListsOnlyKnownFoci() throws IOException {
        LibraryData d = LibraryParser.parse(bundled());
        for (TierDef t : d.tiers.values()) {
            for (String id : t.stage1Foci) {
                assertNotNull(d.focus(id), id);
            }
        }
    }

    @Test
    void badEntriesAreDroppedNotFatal() {
        String json = "{\"foci\":["
                + "{\"id\":\"ok\",\"role\":\"bolt\",\"complexity\":20,\"nbt\":\"" + LINE.replace("\"", "\\\"") + "\"},"
                + "{\"id\":\"ok\",\"role\":\"bolt\",\"nbt\":\"" + LINE.replace("\"", "\\\"") + "\"},"
                + "{\"id\":\"badrole\",\"role\":\"wizardry\",\"nbt\":\"" + LINE.replace("\"", "\\\"") + "\"},"
                + "{\"id\":\"nonbt\",\"role\":\"bolt\"},"
                + "{\"id\":\"backwards\",\"role\":\"bolt\",\"minRange\":10,\"maxRange\":2,\"nbt\":\"" + LINE.replace("\"", "\\\"") + "\"},"
                + "{\"role\":\"bolt\"}"
                + "],\"tiers\":[{\"tier\":1,\"stage1Foci\":[\"ok\",\"ghost\"]}]}";
        LibraryData d = LibraryParser.parse(json);
        assertEquals(1, d.foci.size());
        assertEquals(1, d.tier(1).stage1Foci.size());
        assertEquals(6, d.problems.size(), d.problems.toString());
    }

    @Test
    void invalidJsonReportsAProblem() {
        assertFalse(LibraryParser.parse("{not json").problems.isEmpty());
        assertFalse(LibraryParser.parse("[]").problems.isEmpty());
    }

    @Test
    void aiBlockOverridesDefaults() {
        LibraryData d = LibraryParser.parse("{\"ai\":{\"decisionIntervalTicks\":5,\"idealRange\":{\"min\":6,\"max\":3},\"globalCastGapTicks\":9},\"tiers\":[{\"tier\":1}]}");
        assertEquals(5, d.ai.decisionIntervalTicks);
        assertEquals(9, d.ai.globalCastGapTicks);
        assertEquals(6.0D, d.ai.idealMin);
        assertEquals(6.0D, d.ai.idealMax);
    }

    @Test
    void performedFociNeedNoNbt() {
        LibraryData d = LibraryParser.parse("{\"foci\":[{\"id\":\"blink\",\"role\":\"blink\",\"performed\":true}],\"tiers\":[{\"tier\":1}]}");
        assertEquals(1, d.foci.size());
        assertFalse(d.focus("blink").castable());
    }
}

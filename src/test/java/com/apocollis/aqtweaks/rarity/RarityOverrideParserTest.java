package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RarityOverrideParserTest {
    private static RarityOverrideParser.Result parse(String... lines) {
        return RarityOverrideParser.parse(lines);
    }

    @Test
    void plainItemMetaAndWildcard() {
        RarityOverrideParser.Result r = parse("minecraft:nether_star=LEGENDARY", "minecraft:wool:5=rare", "botania:*=EPIC");
        assertTrue(r.warnings.isEmpty());
        assertEquals(RarityTier.LEGENDARY, r.table.lookup("minecraft", "nether_star", 0));
        assertEquals(RarityTier.RARE, r.table.lookup("minecraft", "wool", 5));
        assertNull(r.table.lookup("minecraft", "wool", 4));
        assertEquals(RarityTier.EPIC, r.table.lookup("botania", "anything", 3));
        assertNull(r.table.lookup("thaumcraft", "x", 0));
    }

    @Test
    void whitespaceCommentsAndBlanksAreIgnored() {
        RarityOverrideParser.Result r = parse("  ", "# a comment", "  aqtweaks:infinity_blade = legendary  ");
        assertTrue(r.warnings.isEmpty());
        assertEquals(1, r.table.size());
        assertEquals(RarityTier.LEGENDARY, r.table.lookup("aqtweaks", "infinity_blade", 0));
    }

    @Test
    void relicAliasAndCase() {
        RarityOverrideParser.Result r = parse("a:b=relic", "a:c=MYTHIC", "a:d=Epic");
        assertEquals(RarityTier.MYTHIC, r.table.lookup("a", "b", 0));
        assertEquals(RarityTier.MYTHIC, r.table.lookup("a", "c", 0));
        assertEquals(RarityTier.EPIC, r.table.lookup("a", "d", 0));
    }

    @Test
    void badLinesAreSkippedWithOneWarningEach() {
        RarityOverrideParser.Result r = parse("a:b=NOPE", "a:b", ":b=RARE", "a:=RARE", "a:b:x=RARE", "a:b:-1=RARE", "a:*:2=RARE", "justone=RARE");
        assertTrue(r.table.isEmpty());
        assertEquals(8, r.warnings.size());
    }

    @Test
    void duplicateKeepsTheLastAndPrecedenceIsMetaItemWildcard() {
        RarityOverrideParser.Result r = parse("m:i=COMMON", "m:i=RARE", "m:i:2=EPIC", "m:*=UNCOMMON");
        assertEquals(RarityTier.EPIC, r.table.lookup("m", "i", 2));
        assertEquals(RarityTier.RARE, r.table.lookup("m", "i", 1));
        assertEquals(RarityTier.UNCOMMON, r.table.lookup("m", "other", 0));
    }

    @Test
    void nullAndEmptyInput() {
        assertTrue(RarityOverrideParser.parse(null).table.isEmpty());
        assertTrue(RarityOverrideParser.parse(new String[0]).table.isEmpty());
        assertTrue(RarityOverrideParser.parse(new String[] {null}).table.isEmpty());
    }
}

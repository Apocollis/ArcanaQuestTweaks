package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RarityColorsTest {
    @Test
    void acceptsPlainCodesAndPrefixes() {
        assertEquals('a', RarityColors.parse("a"));
        assertEquals('a', RarityColors.parse("A"));
        assertEquals('a', RarityColors.parse(RarityColors.SECTION + "a"));
        assertEquals('9', RarityColors.parse("&9"));
        assertEquals('3', RarityColors.parse(" 3 "));
        assertEquals('0', RarityColors.parse("0"));
        assertEquals('f', RarityColors.parse("f"));
    }

    @Test
    void rejectsBadValuesAndFallsBack() {
        for (String bad : new String[] {"", "ab", "k", "l", "m", "n", "o", "r", "z", "g", "&", "&&a", null}) {
            assertEquals(0, RarityColors.parse(bad), String.valueOf(bad));
            assertTrue(RarityColors.isInvalid(bad));
            assertEquals('6', RarityColors.parseOr(bad, '6'));
        }
        assertFalse(RarityColors.isInvalid("d"));
        assertEquals('d', RarityColors.parseOr("d", '6'));
    }
}

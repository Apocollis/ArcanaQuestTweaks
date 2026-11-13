package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RarityRainbowTest {
    private static String strip(String s) {
        return s.replaceAll(RarityColors.SECTION + ".", "");
    }

    @Test
    void keepsTheTextAndSkipsSpaces() {
        String out = RarityRainbow.apply("Infinity Blade", 0L, false);
        assertEquals("Infinity Blade", strip(out));
        assertEquals(RarityColors.SECTION + "dI", out.substring(0, 3));
    }

    @Test
    void animationShiftsTheColors() {
        assertNotEquals(RarityRainbow.apply("Sword", 0L, true), RarityRainbow.apply("Sword", 120L, true));
        assertEquals(RarityRainbow.apply("Sword", 0L, false), RarityRainbow.apply("Sword", 999999L, false));
    }

    @Test
    void emptyAndNull() {
        assertEquals("", RarityRainbow.apply("", 0L, true));
        assertNull(RarityRainbow.apply(null, 0L, true));
    }
}

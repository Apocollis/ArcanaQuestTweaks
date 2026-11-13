package com.apocollis.aqtweaks.weather;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WildcardTest {
    @Test
    void exactAndStar() {
        assertTrue(Wildcard.matches("atum:helmet", "atum:helmet"));
        assertTrue(Wildcard.matches("atum:helmet", "ATUM:Helmet"));
        assertTrue(Wildcard.matches("anything", "*"));
        assertFalse(Wildcard.matches("atum:helmet", "atum:boots"));
        assertFalse(Wildcard.matches("atum:helmet", "atum:helmet2"));
    }

    @Test
    void prefixSuffixMiddle() {
        assertTrue(Wildcard.matches("thaumcraft:fortress_helm", "thaumcraft:fortress_*"));
        assertTrue(Wildcard.matches("a:b_goggles", "*goggles"));
        assertTrue(Wildcard.matches("a:big_goggles_x", "a:*goggles*"));
        assertTrue(Wildcard.matches("a:x_y_z", "a:*_z"));
        assertFalse(Wildcard.matches("a:x_y_w", "a:*_z"));
    }

    @Test
    void multipleStarsBacktrack() {
        assertTrue(Wildcard.matches("aXbXc", "a*b*c"));
        assertTrue(Wildcard.matches("abbbc", "a*b*c"));
        assertFalse(Wildcard.matches("acb", "a*b*c"));
        assertTrue(Wildcard.matches("abc", "a**c"));
        assertTrue(Wildcard.matches("ac", "a*c"));
    }

    @Test
    void emptyAndNull() {
        assertTrue(Wildcard.matches("", "*"));
        assertTrue(Wildcard.matches("", "**"));
        assertFalse(Wildcard.matches("", "a*"));
        assertFalse(Wildcard.matches(null, "*"));
        assertFalse(Wildcard.matches("a", null));
        assertFalse(Wildcard.matchesAny("a", null));
        assertTrue(Wildcard.matchesAny("a:b", new String[] {"x:*", "a:*"}));
    }

    @Test
    void regexCharactersAreLiteral() {
        assertTrue(Wildcard.matches("a.b", "a.b"));
        assertFalse(Wildcard.matches("axb", "a.b*"));
        assertTrue(Wildcard.matches("a+b(c)", "a+b(*)"));
    }
}

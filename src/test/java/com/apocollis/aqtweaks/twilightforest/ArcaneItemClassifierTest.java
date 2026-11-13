package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArcaneItemClassifierTest {
    private static final String[] PATTERNS = {"astralsorcery:item*wand", "bewitchment:*broom*", "aether_legacy:*_staff",
            "twilightforest:scepter*", "embers:caster_orb"};

    @Test
    void matchesTheSeededPatterns() {
        assertTrue(ArcaneItemClassifier.matchesPatterns("astralsorcery:itemwand", PATTERNS));
        assertTrue(ArcaneItemClassifier.matchesPatterns("bewitchment:cypress_broom", PATTERNS));
        assertTrue(ArcaneItemClassifier.matchesPatterns("aether_legacy:cloud_staff", PATTERNS));
        assertTrue(ArcaneItemClassifier.matchesPatterns("twilightforest:scepter_twilight", PATTERNS));
        assertTrue(ArcaneItemClassifier.matchesPatterns("embers:caster_orb", PATTERNS));
    }

    @Test
    void ordinaryItemsDoNotMatch() {
        assertFalse(ArcaneItemClassifier.matchesPatterns("minecraft:iron_sword", PATTERNS));
        assertFalse(ArcaneItemClassifier.matchesPatterns("bewitchment:athame", PATTERNS));
        assertFalse(ArcaneItemClassifier.matchesPatterns(null, PATTERNS));
    }
}

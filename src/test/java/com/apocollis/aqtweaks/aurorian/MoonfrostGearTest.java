package com.apocollis.aqtweaks.aurorian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.weather.Wildcard;
import org.junit.jupiter.api.Test;

class MoonfrostGearTest {

    @Test
    void tierRule() {
        assertEquals(MoonfrostTier.EXPOSED, MoonfrostTier.classify(false, true, true));
        assertEquals(MoonfrostTier.ARMORED, MoonfrostTier.classify(true, false, false));
        assertEquals(MoonfrostTier.PROPER, MoonfrostTier.classify(true, true, false));
        assertEquals(MoonfrostTier.PROPER, MoonfrostTier.classify(true, false, true));
    }

    @Test
    void wildcardMatching() {
        String[] list = {"thaumcraft:goggles", "thaumicaugmentation:thaumium_robes_*", "bewitchment:besmirched_*"};
        assertTrue(Wildcard.matchesAny("thaumcraft:goggles", list));
        assertTrue(Wildcard.matchesAny("thaumicaugmentation:thaumium_robes_chestplate", list));
        assertTrue(Wildcard.matchesAny("Bewitchment:Besmirched_Boots", list));
        assertFalse(Wildcard.matchesAny("atum:desert_helmet_iron", list));
        assertFalse(Wildcard.matchesAny("thaumcraft:thaumium_helm", list));
        assertTrue(Wildcard.matches("anything", "*"));
        assertFalse(Wildcard.matches(null, "*"));
    }

    @Test
    void metaItemMatching() {
        String[] eyeList = {"bibliocraft:biblioglasses:0", "bibliocraft:biblioglasses:1", "botania:cosmetic:4", "thaumcraft:goggles"};
        assertTrue(Wildcard.matchesAnyItem("bibliocraft:biblioglasses", 0, eyeList));
        assertTrue(Wildcard.matchesAnyItem("bibliocraft:biblioglasses", 1, eyeList));
        assertFalse(Wildcard.matchesAnyItem("bibliocraft:biblioglasses", 2, eyeList));
        assertTrue(Wildcard.matchesAnyItem("botania:cosmetic", 4, eyeList));
        assertFalse(Wildcard.matchesAnyItem("botania:cosmetic", 0, eyeList));
        assertTrue(Wildcard.matchesAnyItem("thaumcraft:goggles", 99, eyeList));
        assertEquals("botania:cosmetic", Wildcard.basePattern("botania:cosmetic:4"));
        assertEquals("thaumcraft:goggles", Wildcard.basePattern("thaumcraft:goggles"));
    }
}

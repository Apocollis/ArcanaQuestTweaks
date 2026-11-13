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
        String[] list = {"thaumcraft:goggles", "thaumcraft:thaumium_robes_*", "bewitchment:besmirched_*"};
        assertTrue(Wildcard.matchesAny("thaumcraft:goggles", list));
        assertTrue(Wildcard.matchesAny("thaumcraft:thaumium_robes_chest", list));
        assertTrue(Wildcard.matchesAny("Bewitchment:Besmirched_Boots", list));
        assertFalse(Wildcard.matchesAny("atum:desert_helmet_iron", list));
        assertFalse(Wildcard.matchesAny("thaumcraft:thaumium_helm", list));
        assertTrue(Wildcard.matches("anything", "*"));
        assertFalse(Wildcard.matches(null, "*"));
    }
}

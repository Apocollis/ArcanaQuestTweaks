package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RarityProbeKeyTest {
    @Test
    void damageDoesNotMakeNewKeysButSubtypesDo() {
        assertEquals(RarityOverrides.probeKey(276, false, 0), RarityOverrides.probeKey(276, false, 0));
        assertEquals(RarityOverrides.probeKey(276, false, 0), RarityOverrides.probeKey(276, false, 0) );
        assertNotEquals(RarityOverrides.probeKey(322, true, 0), RarityOverrides.probeKey(322, true, 1));
    }

    @Test
    void itemsNeverCollideAcrossIdsOrMeta() {
        assertNotEquals(RarityOverrides.probeKey(1, true, 2), RarityOverrides.probeKey(2, true, 1));
        assertNotEquals(RarityOverrides.probeKey(4096, true, 0), RarityOverrides.probeKey(0, true, 0));
        assertEquals(RarityOverrides.probeKey(5, true, 32767) >>> 32, 5L);
        assertEquals(RarityOverrides.probeKey(5, true, 32767) & 0xFFFFFFFFL, 32767L);
    }
}

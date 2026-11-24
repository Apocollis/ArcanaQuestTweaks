package com.apocollis.aqtweaks.thaumcraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExposureRulesTest {

    @Test
    void bandsInTheDark() {
        assertEquals("under", ExposureRules.undergroundKey(30, -20, 30, 0, 6));
        assertEquals("under", ExposureRules.undergroundKey(-20, -20, 30, 6, 6));
        assertEquals("underDeep", ExposureRules.undergroundKey(-21, -20, 30, 0, 6));
        assertNull(ExposureRules.undergroundKey(31, -20, 30, 0, 6));
    }

    @Test
    void litPlacesDoNotCount() {
        assertNull(ExposureRules.undergroundKey(10, -20, 30, 7, 6));
        assertNull(ExposureRules.undergroundKey(-40, -20, 30, 14, 6));
    }

    @Test
    void fifteenTurnsTheGateOff() {
        assertEquals("under", ExposureRules.undergroundKey(10, -20, 30, 15, 15));
        assertEquals("underDeep", ExposureRules.undergroundKey(-40, -20, 30, 15, 15));
    }
}

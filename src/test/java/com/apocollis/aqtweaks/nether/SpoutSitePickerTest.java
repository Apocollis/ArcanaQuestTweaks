package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpoutSitePickerTest {
    @Test
    void distanceWindow() {
        assertFalse(SpoutSitePicker.distanceOk(2, 2, 6, 24));
        assertTrue(SpoutSitePicker.distanceOk(10, 0, 6, 24));
        assertFalse(SpoutSitePicker.distanceOk(30, 0, 6, 24));
    }

    @Test
    void separationFromOtherSites() {
        int[][] others = {{0, 64, 0}};
        assertFalse(SpoutSitePicker.farFromOthers(5, 64, 0, others, 10.0D));
        assertTrue(SpoutSitePicker.farFromOthers(12, 64, 0, others, 10.0D));
    }
}

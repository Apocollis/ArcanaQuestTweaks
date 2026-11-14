package com.apocollis.aqtweaks.atum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AtumWeatherCommandHelperTest {
    @Test
    void parsesSecondsAndSkipInAnyOrder() {
        AtumWeatherCommandHelper.Parsed a = AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "45", "skipWarning"}, 1, true);
        assertNull(a.error);
        assertEquals(45, a.seconds);
        assertTrue(a.skipWarning);
        AtumWeatherCommandHelper.Parsed b = AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "SKIPWARNING", "60"}, 1, true);
        assertNull(b.error);
        assertEquals(60, b.seconds);
        assertTrue(b.skipWarning);
        AtumWeatherCommandHelper.Parsed c = AtumWeatherCommandHelper.parse(new String[] {"sandstorm"}, 1, true);
        assertNull(c.error);
        assertEquals(0, c.seconds);
        assertFalse(c.skipWarning);
    }

    @Test
    void rejectsBadArguments() {
        assertNotNull(AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "0"}, 1, true).error);
        assertNotNull(AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "-5"}, 1, true).error);
        assertNotNull(AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "1000001"}, 1, true).error);
        assertNotNull(AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "abc"}, 1, true).error);
        assertNotNull(AtumWeatherCommandHelper.parse(new String[] {"sandstorm", "10", "20"}, 1, true).error);
        assertNotNull(AtumWeatherCommandHelper.parse(new String[] {"clear", "skipWarning"}, 1, false).error);
        assertNull(AtumWeatherCommandHelper.parse(new String[] {"clear", "30"}, 1, false).error);
    }

    @Test
    void stormAndClearDurations() {
        assertEquals(900, AtumWeatherCommandHelper.stormTicks(45, true, 240, 420, n -> 0));
        assertEquals(240 * 20 + 7, AtumWeatherCommandHelper.stormTicks(0, true, 240, 420, n -> 7));
        assertEquals(30 * 20, AtumWeatherCommandHelper.stormTicks(0, true, 5, 5, n -> 99));
        assertEquals(6000 + 11, AtumWeatherCommandHelper.stormTicks(0, false, 240, 420, n -> 11));
        assertEquals(60 * 20, AtumWeatherCommandHelper.clearTicks(60, true, 360, 1200, n -> 0) / 20 * 20);
        assertEquals(360 * 20 + 5, AtumWeatherCommandHelper.clearTicks(0, true, 360, 1200, n -> 5));
        assertEquals((300 + 9) * 20, AtumWeatherCommandHelper.clearTicks(0, false, 360, 1200, n -> 9));
    }

    @Test
    void pendingAndWarningRules() {
        assertEquals(900, AtumWeatherCommandHelper.pendingTicks(45));
        assertEquals(0, AtumWeatherCommandHelper.pendingTicks(0));
        assertTrue(AtumWeatherCommandHelper.warns(false, 600));
        assertFalse(AtumWeatherCommandHelper.warns(true, 600));
        assertFalse(AtumWeatherCommandHelper.warns(false, 0));
        assertFalse(AtumWeatherCommandHelper.warns(false, AtumWarningRamp.PINNED_CLEAN_WEATHER));
    }
}

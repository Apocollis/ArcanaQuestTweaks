package com.apocollis.aqtweaks.weather;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShelterHoldTest {
    @Test
    void firstValueIsTaken() {
        assertTrue(new ShelterHold().update(true, 0, 40));
    }

    @Test
    void briefChangeIsIgnored() {
        ShelterHold h = new ShelterHold();
        h.update(false, 0, 40);
        assertFalse(h.update(true, 20, 40));
        assertFalse(h.update(false, 30, 40));
        assertFalse(h.update(true, 50, 40));
        assertFalse(h.update(true, 80, 40));
    }

    @Test
    void sustainedChangeFlipsAfterTheDelay() {
        ShelterHold h = new ShelterHold();
        h.update(false, 0, 40);
        assertFalse(h.update(true, 20, 40));
        assertFalse(h.update(true, 59, 40));
        assertTrue(h.update(true, 60, 40));
    }

    @Test
    void zeroDelayPassesThrough() {
        ShelterHold h = new ShelterHold();
        h.update(false, 0, 0);
        assertTrue(h.update(true, 1, 0));
    }
}

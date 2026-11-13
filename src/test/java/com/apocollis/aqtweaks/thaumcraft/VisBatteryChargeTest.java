package com.apocollis.aqtweaks.thaumcraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VisBatteryChargeTest {
    @Test
    void chargeMapsToDamage() {
        for (int c = 0; c <= 10; c++) assertEquals(c, VisBatteryCharge.damageFor(c));
    }

    @Test
    void outOfRangeIsClamped() {
        assertEquals(0, VisBatteryCharge.damageFor(-3));
        assertEquals(10, VisBatteryCharge.damageFor(15));
    }
}

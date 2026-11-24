package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FocusCooldownsTest {

    @Test
    void naturalCooldownMatchesTheBytecodeFormula() {
        assertEquals(5, FocusCooldowns.natural(10));
        assertEquals(12, FocusCooldowns.natural(17));
        assertEquals(20, FocusCooldowns.natural(20));
        assertEquals(30, FocusCooldowns.natural(25));
        assertEquals(56, FocusCooldowns.natural(35));
        assertEquals(120, FocusCooldowns.natural(50));
        assertEquals(9 > 5 ? 9 : 5, FocusCooldowns.natural(15));
    }

    @Test
    void scalesAndOverrides() {
        assertEquals(60, FocusCooldowns.effective(null, 50, 0.5D, 1.0D));
        assertEquals(45, FocusCooldowns.effective(null, 50, 0.5D, 0.75D));
        assertEquals(40, FocusCooldowns.effective(40, 50, 1.0D, 1.0D));
        assertEquals(1, FocusCooldowns.effective(null, 20, 0.0D, 1.0D));
    }
}

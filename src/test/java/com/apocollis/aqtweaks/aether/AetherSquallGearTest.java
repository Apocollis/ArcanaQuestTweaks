package com.apocollis.aqtweaks.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.aether.AetherSquallGear.GearClass;

import java.util.Random;

import org.junit.jupiter.api.Test;

class AetherSquallGearTest {
    @Test
    void classPrecedence() {
        assertEquals(GearClass.CHANNELING, AetherSquallGear.classify(true, true, true));
        assertEquals(GearClass.INSULATED, AetherSquallGear.classify(true, true, false));
        assertEquals(GearClass.CONDUCTIVE, AetherSquallGear.classify(false, true, false));
        assertEquals(GearClass.NORMAL, AetherSquallGear.classify(false, false, false));
    }

    @Test
    void everyoneButInsulatedIsLockedOn() {
        assertTrue(AetherSquallGear.locksOn(GearClass.NORMAL));
        assertTrue(AetherSquallGear.locksOn(GearClass.CONDUCTIVE));
        assertTrue(AetherSquallGear.locksOn(GearClass.CHANNELING));
        assertFalse(AetherSquallGear.locksOn(GearClass.INSULATED));
    }

    @Test
    void onlyChannelingIsImmune() {
        assertTrue(AetherSquallGear.immuneToBolt(GearClass.CHANNELING));
        assertFalse(AetherSquallGear.immuneToBolt(GearClass.INSULATED));
        assertFalse(AetherSquallGear.immuneToBolt(GearClass.CONDUCTIVE));
        assertFalse(AetherSquallGear.immuneToBolt(GearClass.NORMAL));
    }

    @Test
    void conductiveAndChannelingAreDrawnAndRetargeted() {
        assertTrue(AetherSquallGear.retargeted(GearClass.CONDUCTIVE));
        assertTrue(AetherSquallGear.retargeted(GearClass.CHANNELING));
        assertFalse(AetherSquallGear.retargeted(GearClass.NORMAL));
        assertFalse(AetherSquallGear.retargeted(GearClass.INSULATED));
        assertEquals(4, AetherSquallGear.attractionWeight(GearClass.CONDUCTIVE));
        assertEquals(4, AetherSquallGear.attractionWeight(GearClass.CHANNELING));
        assertEquals(1, AetherSquallGear.attractionWeight(GearClass.NORMAL));
        assertEquals(1, AetherSquallGear.attractionWeight(GearClass.INSULATED));
        assertTrue(AetherSquallGear.priority(GearClass.CONDUCTIVE) < AetherSquallGear.priority(GearClass.NORMAL));
        assertTrue(AetherSquallGear.priority(GearClass.NORMAL) < AetherSquallGear.priority(GearClass.INSULATED));
    }

    @Test
    void divertDistanceIsThreeToEight() {
        Random rand = new Random(5L);
        for (int i = 0; i < 2000; i++) {
            double d = AetherSquallGear.divertDistance(rand);
            assertTrue(d >= 3.0D && d < 8.0D);
        }
    }

    @Test
    void insulatedTakesReducedShock() {
        assertEquals(2.0D, AetherSquallGear.shockDamage(true, 6.0D, 2.0D));
        assertEquals(6.0D, AetherSquallGear.shockDamage(false, 6.0D, 2.0D));
    }
}

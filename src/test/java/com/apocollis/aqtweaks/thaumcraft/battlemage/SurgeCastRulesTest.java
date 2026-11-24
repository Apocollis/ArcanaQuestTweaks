package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SurgeCastRulesTest {

    @Test
    void restrictedOnlyBelowTheThresholdWhileOverchanneling() {
        assertTrue(SurgeCastRules.restricted(true, 0.09D, 0.10D));
        assertFalse(SurgeCastRules.restricted(true, 0.10D, 0.10D));
        assertFalse(SurgeCastRules.restricted(true, 0.5D, 0.10D));
        assertFalse(SurgeCastRules.restricted(false, 0.01D, 0.10D));
        assertFalse(SurgeCastRules.restricted(true, 0.01D, 0.0D));
    }

    @Test
    void whenRestrictedOnlyHealBlinkAndRiftRolesMayCast() {
        for (String role : new String[] {"heal_self", "heal_ally", "blink", "rift"}) {
            assertTrue(SurgeCastRules.castAllowed(role, true), role);
        }
        for (String role : new String[] {"ranged_primary", "ranged_heavy", "bolt", "bolt_heavy", "debuff", "aoe",
                "peel", "summon_upkeep"}) {
            assertFalse(SurgeCastRules.castAllowed(role, true), role);
        }
    }

    @Test
    void whenNotRestrictedEverythingMayCast() {
        assertTrue(SurgeCastRules.castAllowed("bolt", false));
        assertTrue(SurgeCastRules.castAllowed(null, false));
    }
}

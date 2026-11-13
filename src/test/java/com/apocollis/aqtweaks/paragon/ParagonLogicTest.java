package com.apocollis.aqtweaks.paragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParagonLogicTest {
    @Test
    void onlyTheNineTartariteItemsAreKinds() {
        assertEquals(TartariteKind.SWORD, TartariteKind.fromPath("metallurgy", "tartarite_sword"));
        assertEquals(TartariteKind.BOOTS, TartariteKind.fromPath("metallurgy", "tartarite_boots"));
        assertEquals(9, TartariteKind.values().length);
        assertNull(TartariteKind.fromPath("metallurgy", "tartarite_ingot"));
        assertNull(TartariteKind.fromPath("metallurgy", "tartarite_block"));
        assertNull(TartariteKind.fromPath("metallurgy", "steel_sword"));
        assertNull(TartariteKind.fromPath("spartanweaponry", "tartarite_sword"));
        assertNull(TartariteKind.fromPath("metallurgy", "sword_tartarite"));
        assertNull(TartariteKind.fromPath("metallurgy", null));
    }

    @Test
    void kindGroupsAreConsistent() {
        for (TartariteKind kind : TartariteKind.values()) {
            assertEquals(kind.isArmor(), !kind.isHandheld());
            assertEquals("tartarite_" + kind.name().toLowerCase(), kind.itemPath());
        }
        assertTrue(TartariteKind.PICKAXE.isTool());
        assertFalse(TartariteKind.SWORD.isTool());
        assertFalse(TartariteKind.HELMET.isTool());
        assertEquals(4, TartariteKind.armorPieces().size());
        assertEquals(4, TartariteKind.weaponsAndTools().size());
    }

    @Test
    void absorbRules() {
        assertEquals(ParagonRules.Absorb.ACCEPT, ParagonRules.absorb(null, false, "fiery", true));
        assertEquals(ParagonRules.Absorb.ACCEPT, ParagonRules.absorb("", false, "fiery", true));
        assertEquals(ParagonRules.Absorb.REPLACE, ParagonRules.absorb("octine", false, "fiery", true));
        assertEquals(ParagonRules.Absorb.REJECT_SAME, ParagonRules.absorb("fiery", false, "fiery", true));
        assertEquals(ParagonRules.Absorb.REJECT_METALLURGY, ParagonRules.absorb(null, true, "fiery", true));
        assertEquals(ParagonRules.Absorb.REJECT_METALLURGY, ParagonRules.absorb("octine", true, "fiery", true));
        assertEquals(ParagonRules.Absorb.REJECT_KIND, ParagonRules.absorb(null, false, "fiery", false));
    }

    @Test
    void aSetPerkNeedsAllFourPiecesWithTheSameTrait() {
        assertEquals("gravitite", ParagonRules.fullSet("gravitite", "gravitite", "gravitite", "gravitite"));
        assertNull(ParagonRules.fullSet("gravitite", "gravitite", "gravitite", null));
        assertNull(ParagonRules.fullSet("gravitite", "gravitite", "zanite", "gravitite"));
        assertNull(ParagonRules.fullSet(null, null, null, null));
        assertNull(ParagonRules.fullSet("", "", "", ""));
    }

    @Test
    void zaniteTiersFollowTheAetherThresholdsAsShares() {
        assertEquals(0, WearTier.tier(0, 250));
        // Aether Zanite (250 uses): 240 or more left is tier 0, 201 to 239 tier 1, 111 to 200 tier 2, 51 to 110 tier 3, 50 or fewer tier 4.
        assertEquals(0, WearTier.tier(10, 250));
        assertEquals(1, WearTier.tier(11, 250));
        assertEquals(1, WearTier.tier(49, 250));
        assertEquals(2, WearTier.tier(50, 250));
        assertEquals(2, WearTier.tier(139, 250));
        assertEquals(3, WearTier.tier(140, 250));
        assertEquals(3, WearTier.tier(199, 250));
        assertEquals(4, WearTier.tier(200, 250));
        assertEquals(4, WearTier.tier(250, 250));
        // The same shares on a longer-lived item.
        assertEquals(2, WearTier.tier(1000, 3000));
        assertEquals(4, WearTier.tier(2900, 3000));
    }

    @Test
    void zaniteTiersHandleOddInput() {
        assertEquals(0, WearTier.tier(5, 0));
        assertEquals(0, WearTier.tier(-5, 100));
        assertEquals(4, WearTier.tier(500, 100));
    }

    @Test
    void zaniteSpeedNeverBelowOneAndSwordBonusRises() {
        assertEquals(1.0F, WearTier.speedFactor(0), 1e-6);
        assertEquals(1.0F, WearTier.speedFactor(1), 1e-6);
        assertEquals(1.5F, WearTier.speedFactor(2), 1e-6);
        assertEquals(2.0F, WearTier.speedFactor(3), 1e-6);
        assertEquals(3.0F, WearTier.speedFactor(4), 1e-6);
        assertEquals(3.0F, WearTier.speedFactor(99), 1e-6);
        assertEquals(0.0F, WearTier.swordBonus(0), 1e-6);
        assertEquals(4.0F, WearTier.swordBonus(4), 1e-6);
    }
}

package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RarityTierCombineTest {
    @Test
    void highestTraitWins() {
        assertEquals(RarityTier.LEGENDARY, RarityTier.combine(RarityTier.COMMON, RarityTier.EPIC, RarityTier.LEGENDARY));
    }

    @Test
    void orderOfInputsDoesNotMatter() {
        assertEquals(RarityTier.LEGENDARY, RarityTier.combine(RarityTier.LEGENDARY, RarityTier.COMMON, RarityTier.EPIC));
        assertEquals(RarityTier.LEGENDARY, RarityTier.combine(RarityTier.EPIC, RarityTier.LEGENDARY, RarityTier.COMMON));
    }

    @Test
    void singleTraitGivesItsOwnTierAndNoTraitGivesTheBase() {
        assertEquals(RarityTier.EPIC, RarityTier.combine(RarityTier.COMMON, RarityTier.EPIC, null));
        assertEquals(RarityTier.RARE, RarityTier.combine(RarityTier.COMMON, null, RarityTier.RARE));
        assertEquals(RarityTier.UNCOMMON, RarityTier.combine(RarityTier.UNCOMMON, null, null));
        assertNull(RarityTier.combine(null, null, null));
    }

    @Test
    void equalTiersDoNotStackUpward() {
        assertEquals(RarityTier.UNCOMMON, RarityTier.combine(RarityTier.UNCOMMON, RarityTier.UNCOMMON, RarityTier.UNCOMMON));
    }

    @Test
    void parseAndRelicAlias() {
        assertEquals(RarityTier.MYTHIC, RarityTier.parse("relic"));
        assertEquals(RarityTier.RARE, RarityTier.parse(" Rare "));
        assertNull(RarityTier.parse("bogus"));
        assertNull(RarityTier.parse(null));
    }
}

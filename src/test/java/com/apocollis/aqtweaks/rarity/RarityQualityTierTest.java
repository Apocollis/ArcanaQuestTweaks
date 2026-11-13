package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class RarityQualityTierTest {
    private static final Map<String, RarityTier> D = RarityQualityTier.defaults();

    @Test
    void defaultMapAndUnmappedColors() {
        assertEquals(RarityTier.UNCOMMON, RarityQualityTier.tierFor(D, "green"));
        assertEquals(RarityTier.UNCOMMON, RarityQualityTier.tierFor(D, "yellow"));
        assertEquals(RarityTier.RARE, RarityQualityTier.tierFor(D, "dark_aqua"));
        assertEquals(RarityTier.LEGENDARY, RarityQualityTier.tierFor(D, "gold"));
        for (String none : new String[] {"red", "gray", "dark_gray", "", null}) {
            assertNull(RarityQualityTier.tierFor(D, none), String.valueOf(none));
        }
    }

    @Test
    void colorMatchingIsCaseInsensitive() {
        assertEquals(RarityTier.LEGENDARY, RarityQualityTier.tierFor(D, "GOLD"));
        assertEquals(RarityTier.RARE, RarityQualityTier.tierFor(D, "Dark_Aqua"));
    }

    @Test
    void qualityNeverLowersATier() {
        assertEquals(RarityTier.LEGENDARY, RarityTier.max(RarityTier.RARE, RarityQualityTier.tierFor(D, "gold")));
        assertEquals(RarityTier.EPIC, RarityTier.max(RarityTier.EPIC, RarityQualityTier.tierFor(D, "green")));
        assertEquals(RarityTier.RARE, RarityTier.max(RarityTier.RARE, RarityQualityTier.tierFor(D, "red")));
    }

    @Test
    void customEntriesAndBadLines() {
        RarityQualityTier.Parsed p = RarityQualityTier.parse(new String[] {"gold=MYTHIC", "aqua = epic", "red", "=RARE", "blue=NOPE", "# c"});
        assertEquals(RarityTier.MYTHIC, RarityQualityTier.tierFor(p.map, "gold"));
        assertEquals(RarityTier.EPIC, RarityQualityTier.tierFor(p.map, "aqua"));
        assertEquals(3, p.warnings.size());
        assertTrue(RarityQualityTier.parse(null).map.isEmpty());
    }
}

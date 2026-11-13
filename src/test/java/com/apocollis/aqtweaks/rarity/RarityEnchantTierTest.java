package com.apocollis.aqtweaks.rarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.apocollis.aqtweaks.rarity.RarityEnchantTier.EnchRank;

class RarityEnchantTierTest {
    private static final Map<EnchRank, RarityTier> D = RarityEnchantTier.defaults();

    @Test
    void defaultMap() {
        assertEquals(RarityTier.UNCOMMON, D.get(EnchRank.COMMON));
        assertEquals(RarityTier.UNCOMMON, D.get(EnchRank.UNCOMMON));
        assertEquals(RarityTier.RARE, D.get(EnchRank.RARE));
        assertEquals(RarityTier.EPIC, D.get(EnchRank.VERY_RARE));
    }

    @Test
    void bestOfSeveralWins() {
        assertEquals(RarityTier.RARE, RarityEnchantTier.best(D, List.of(EnchRank.COMMON, EnchRank.RARE, EnchRank.UNCOMMON)));
        assertEquals(RarityTier.EPIC, RarityEnchantTier.best(D, List.of(EnchRank.RARE, EnchRank.VERY_RARE)));
        assertNull(RarityEnchantTier.best(D, List.of()));
    }

    @Test
    void baseTierDistinguishesTheVanillaBumpFromAGenuineRare() {
        // native Rare with an unenchanted probe of Common: the Rare came from vanilla's enchant bump
        assertEquals(RarityTier.COMMON, RarityEnchantTier.base(RarityTier.RARE, RarityTier.COMMON));
        // a genuinely Rare item
        assertEquals(RarityTier.RARE, RarityEnchantTier.base(RarityTier.RARE, RarityTier.RARE));
        // every other native tier is its own base
        assertEquals(RarityTier.UNCOMMON, RarityEnchantTier.base(RarityTier.UNCOMMON, RarityTier.COMMON));
        assertEquals(RarityTier.EPIC, RarityEnchantTier.base(RarityTier.EPIC, RarityTier.COMMON));
        assertEquals(RarityTier.COMMON, RarityEnchantTier.base(RarityTier.COMMON, RarityTier.COMMON));
    }

    @Test
    void sharpnessSwordIsUncommonAndNeverDowngrades() {
        RarityTier base = RarityEnchantTier.base(RarityTier.RARE, RarityTier.COMMON);
        assertEquals(RarityTier.UNCOMMON, RarityTier.max(base, D.get(EnchRank.COMMON)));
        assertEquals(RarityTier.RARE, RarityTier.max(base, D.get(EnchRank.RARE)));
        assertEquals(RarityTier.EPIC, RarityTier.max(base, D.get(EnchRank.VERY_RARE)));
        // an Uncommon enchant on a genuinely Rare item stays Rare
        assertEquals(RarityTier.RARE, RarityTier.max(RarityTier.RARE, D.get(EnchRank.UNCOMMON)));
        // a Rare enchant on Epic or Legendary keeps the higher tier
        assertEquals(RarityTier.EPIC, RarityTier.max(RarityTier.EPIC, D.get(EnchRank.RARE)));
        assertEquals(RarityTier.LEGENDARY, RarityTier.max(RarityTier.LEGENDARY, D.get(EnchRank.VERY_RARE)));
    }

    @Test
    void customMapAndBadLines() {
        RarityEnchantTier.Parsed p = RarityEnchantTier.parse(new String[] {"very_rare=LEGENDARY", "COMMON", "bogus=RARE", "RARE=NOPE", "# c", ""});
        assertEquals(RarityTier.LEGENDARY, p.map.get(EnchRank.VERY_RARE));
        assertEquals(RarityTier.UNCOMMON, p.map.get(EnchRank.COMMON));
        assertEquals(RarityTier.RARE, p.map.get(EnchRank.RARE));
        assertEquals(3, p.warnings.size());
        assertTrue(RarityEnchantTier.parse(null).warnings.isEmpty());
    }
}

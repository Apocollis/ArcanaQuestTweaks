package com.apocollis.aqtweaks.rarity;

import java.util.Locale;

/** The six pack rarity tiers, lowest first. Pure: no Minecraft types, so it is unit tested. */
public enum RarityTier {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC;

    /** Parses a tier name, case-insensitive; {@code RELIC} is an alias for {@code MYTHIC}. Returns null if unknown. */
    public static RarityTier parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        if (s.equals("RELIC")) return MYTHIC;
        for (RarityTier t : values()) {
            if (t.name().equals(s)) return t;
        }
        return null;
    }

    public static RarityTier max(RarityTier a, RarityTier b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /** The highest trait wins. Null inputs are skipped. Traits never stack upward. */
    public static RarityTier combine(RarityTier base, RarityTier enchant, RarityTier quality) {
        return max(max(base, enchant), quality);
    }
}

package com.apocollis.aqtweaks.rarity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure enchantment-to-item-tier mapping. An adapter converts {@code Enchantment.Rarity} to {@link EnchRank}. */
public final class RarityEnchantTier {
    private RarityEnchantTier() {}

    /** Mirrors {@code Enchantment.Rarity}. */
    public enum EnchRank { COMMON, UNCOMMON, RARE, VERY_RARE }

    public static Map<EnchRank, RarityTier> defaults() {
        Map<EnchRank, RarityTier> m = new EnumMap<>(EnchRank.class);
        m.put(EnchRank.COMMON, RarityTier.UNCOMMON);
        m.put(EnchRank.UNCOMMON, RarityTier.UNCOMMON);
        m.put(EnchRank.RARE, RarityTier.RARE);
        m.put(EnchRank.VERY_RARE, RarityTier.EPIC);
        return m;
    }

    public static final class Parsed {
        public final Map<EnchRank, RarityTier> map = defaults();
        public final List<String> warnings = new ArrayList<>();
    }

    /** Lines are {@code ENCHANT_RARITY=TIER}. A bad line is skipped with a warning; its entry keeps the default. */
    public static Parsed parse(String[] lines) {
        Parsed p = new Parsed();
        if (lines == null) return p;
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) {
                p.warnings.add("missing '=' in \"" + line + "\"");
                continue;
            }
            EnchRank rank = rank(line.substring(0, eq));
            RarityTier tier = RarityTier.parse(line.substring(eq + 1));
            if (rank == null || tier == null) {
                p.warnings.add("bad enchantment rarity line \"" + line + "\"");
                continue;
            }
            p.map.put(rank, tier);
        }
        return p;
    }

    public static EnchRank rank(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (EnchRank r : EnchRank.values()) {
            if (r.name().equals(s)) return r;
        }
        return null;
    }

    /** The highest mapped tier among the ranks, or null when the list is empty (curses are filtered by the caller). */
    public static RarityTier best(Map<EnchRank, RarityTier> map, Iterable<EnchRank> ranks) {
        RarityTier best = null;
        for (EnchRank r : ranks) {
            best = RarityTier.max(best, map.get(r));
        }
        return best;
    }

    /**
     * The base tier of an enchanted stack. Vanilla's {@code Item.getRarity} answers Rare for any enchanted default item,
     * so when the native tier is Rare the caller probes the unenchanted rating: a Common probe means the Rare came from
     * the enchantment bump and the base is Common. Every other native tier is already its own base.
     */
    public static RarityTier base(RarityTier nativeTier, RarityTier unenchantedProbe) {
        if (nativeTier == RarityTier.RARE && unenchantedProbe == RarityTier.COMMON) return RarityTier.COMMON;
        return nativeTier;
    }
}

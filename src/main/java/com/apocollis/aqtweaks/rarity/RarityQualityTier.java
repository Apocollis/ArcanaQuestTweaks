package com.apocollis.aqtweaks.rarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure Quality Tools color-to-tier mapping. The NBT string read stays in {@code RarityOverrides}. */
public final class RarityQualityTier {
    private RarityQualityTier() {}

    public static Map<String, RarityTier> defaults() {
        Map<String, RarityTier> m = new HashMap<>();
        m.put("green", RarityTier.UNCOMMON);
        m.put("yellow", RarityTier.UNCOMMON);
        m.put("dark_aqua", RarityTier.RARE);
        m.put("gold", RarityTier.LEGENDARY);
        return m;
    }

    public static final class Parsed {
        public final Map<String, RarityTier> map = new HashMap<>();
        public final List<String> warnings = new ArrayList<>();
    }

    /** Lines are {@code color=TIER}. A bad line is skipped with a warning. Colors not listed have no effect. */
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
            String color = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            RarityTier tier = RarityTier.parse(line.substring(eq + 1));
            if (color.isEmpty() || tier == null) {
                p.warnings.add("bad quality color line \"" + line + "\"");
                continue;
            }
            p.map.put(color, tier);
        }
        return p;
    }

    /** The tier for a stored color name, or null for a missing, empty or unmapped color. */
    public static RarityTier tierFor(Map<String, RarityTier> map, String color) {
        if (color == null || color.isEmpty()) return null;
        return map.get(color.toLowerCase(Locale.ROOT));
    }
}

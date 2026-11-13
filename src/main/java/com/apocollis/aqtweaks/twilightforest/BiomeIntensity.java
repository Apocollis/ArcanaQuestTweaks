package com.apocollis.aqtweaks.twilightforest;

import java.util.HashMap;
import java.util.Map;

/** Biome multiplier and extra-effect tables parsed from the config. No Minecraft types. */
public final class BiomeIntensity {
    private BiomeIntensity() {}

    public static final class Extra {
        public final String effect;
        public final int ticks;
        public final int amplifier;

        Extra(String effect, int ticks, int amplifier) {
            this.effect = effect;
            this.ticks = ticks;
            this.amplifier = amplifier;
        }
    }

    /** {@code name=multiplier} entries; malformed ones are skipped. */
    public static Map<String, Double> parseIntensity(String[] entries) {
        Map<String, Double> out = new HashMap<>();
        for (String entry : entries) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            try {
                out.put(entry.substring(0, eq).trim(), Double.parseDouble(entry.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    /** {@code name=effect,ticks[,amplifier]} entries; malformed ones are skipped. */
    public static Map<String, Extra> parseExtras(String[] entries) {
        Map<String, Extra> out = new HashMap<>();
        for (String entry : entries) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String[] parts = entry.substring(eq + 1).split(",");
            if (parts.length < 2) continue;
            try {
                int ticks = Integer.parseInt(parts[1].trim());
                int amp = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
                out.put(entry.substring(0, eq).trim(), new Extra(parts[0].trim(), ticks, amp));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public static double intensityOf(Map<String, Double> table, String biome, double max) {
        Double value = table.get(biome);
        double v = value == null ? 1.0D : value;
        return Math.max(0.0D, Math.min(max, v));
    }
}

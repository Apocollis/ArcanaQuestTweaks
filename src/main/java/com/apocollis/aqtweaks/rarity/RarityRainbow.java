package com.apocollis.aqtweaks.rarity;

/** Pure per-character rainbow text for Mythic names. Colors are section-sign codes. */
public final class RarityRainbow {
    private RarityRainbow() {}

    private static final char[] SPECTRUM = {'d', '5', '9', 'b', 'a', 'e', '6'};

    /** Wraps each non-space character in a color code. {@code animated} shifts the colors with {@code timeMs}. */
    public static String apply(String plain, long timeMs, boolean animated) {
        if (plain == null || plain.isEmpty()) return plain;
        int shift = animated ? (int) ((timeMs / 120L) % SPECTRUM.length) : 0;
        StringBuilder sb = new StringBuilder(plain.length() * 3);
        int i = 0;
        for (int n = 0; n < plain.length(); n++) {
            char ch = plain.charAt(n);
            if (ch == ' ') {
                sb.append(ch);
                continue;
            }
            sb.append(RarityColors.SECTION).append(SPECTRUM[(i + shift) % SPECTRUM.length]).append(ch);
            i++;
        }
        return sb.toString();
    }
}

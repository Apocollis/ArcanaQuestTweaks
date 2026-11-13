package com.apocollis.aqtweaks.rarity;

import java.util.Locale;

/** Pure color-code parsing for the rarity config. */
public final class RarityColors {
    private RarityColors() {}

    /** The section sign, written as a code point so source encoding cannot change it. */
    public static final char SECTION = (char) 0xA7;

    /**
     * Accepts one character 0-9 or a-f (case-insensitive), optionally prefixed by a section sign or an ampersand.
     * Returns the lower-case code, or 0 when the value is not a plain color code (formatting codes are rejected).
     */
    public static char parse(String raw) {
        if (raw == null) return 0;
        String s = raw.trim();
        if (s.length() == 2 && (s.charAt(0) == SECTION || s.charAt(0) == '&')) s = s.substring(1);
        if (s.length() != 1) return 0;
        char c = s.toLowerCase(Locale.ROOT).charAt(0);
        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) return c;
        return 0;
    }

    /** Parses, or returns the fallback. */
    public static char parseOr(String raw, char fallback) {
        char c = parse(raw);
        return c == 0 ? fallback : c;
    }

    public static boolean isInvalid(String raw) {
        return parse(raw) == 0;
    }
}

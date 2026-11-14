package com.apocollis.aqtweaks.weather;

/** Case-insensitive registry-name matcher with {@code *} wildcards. No Minecraft types, no regex, no allocation. */
public final class Wildcard {
    private Wildcard() {}

    public static boolean matches(String text, String pattern) {
        if (text == null || pattern == null) return false;
        if (pattern.equals("*") || pattern.equalsIgnoreCase(text)) return true;
        if (pattern.indexOf('*') < 0) return false;
        return glob(text, pattern);
    }

    /** Iterative glob with single-star backtracking: {@code *} matches any run of characters, including none. */
    private static boolean glob(String text, String pattern) {
        int t = 0;
        int p = 0;
        int starP = -1;
        int starT = 0;
        int tl = text.length();
        int pl = pattern.length();
        while (t < tl) {
            if (p < pl && pattern.charAt(p) == '*') {
                starP = p++;
                starT = t;
            } else if (p < pl && sameChar(pattern.charAt(p), text.charAt(t))) {
                p++;
                t++;
            } else if (starP >= 0) {
                p = starP + 1;
                t = ++starT;
            } else {
                return false;
            }
        }
        while (p < pl && pattern.charAt(p) == '*') p++;
        return p == pl;
    }

    private static boolean sameChar(char a, char b) {
        return a == b || Character.toUpperCase(a) == Character.toUpperCase(b)
                || Character.toLowerCase(a) == Character.toLowerCase(b);
    }

    public static boolean matchesAny(String text, String[] patterns) {
        if (patterns == null) return false;
        for (String pattern : patterns) {
            if (matches(text, pattern)) return true;
        }
        return false;
    }

    /**
     * Matches an item registry name and metadata against {@code pattern}. Supports an optional trailing
     * {@code :<meta>} (for example {@code bibliocraft:biblioglasses:0} or {@code botania:cosmetic:4});
     * patterns without a numeric suffix match any metadata.
     */
    public static boolean matchesItem(String registryName, int meta, String pattern) {
        if (registryName == null || pattern == null) return false;
        int firstColon = pattern.indexOf(':');
        int lastColon = pattern.lastIndexOf(':');
        if (firstColon >= 0 && lastColon > firstColon && isDigits(pattern, lastColon + 1)) {
            int wantedMeta;
            try {
                wantedMeta = Integer.parseInt(pattern.substring(lastColon + 1));
            } catch (NumberFormatException e) {
                return false;
            }
            if (meta != wantedMeta) return false;
            return matches(registryName, pattern.substring(0, lastColon));
        }
        return matches(registryName, pattern);
    }

    /** Strips an optional trailing {@code :<meta>} suffix from an item pattern for registry-key auditing. */
    public static String basePattern(String pattern) {
        if (pattern == null) return null;
        int firstColon = pattern.indexOf(':');
        int lastColon = pattern.lastIndexOf(':');
        if (firstColon >= 0 && lastColon > firstColon && isDigits(pattern, lastColon + 1)) {
            return pattern.substring(0, lastColon);
        }
        return pattern;
    }

    public static boolean matchesAnyItem(String registryName, int meta, String[] patterns) {
        if (patterns == null) return false;
        for (String pattern : patterns) {
            if (matchesItem(registryName, meta, pattern)) return true;
        }
        return false;
    }

    private static boolean isDigits(String s, int start) {
        if (start >= s.length()) return false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}

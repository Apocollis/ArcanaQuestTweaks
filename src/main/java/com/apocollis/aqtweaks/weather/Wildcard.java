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
}

package com.apocollis.aqtweaks.comfort.home;

/** Display name of a home structure. Pure, so the format fallback is tested. */
public final class HomeNames {

    public static final String DEFAULT_FORMAT = "%s's Home";
    public static final String TYPE = "home";

    private HomeNames() {}

    public static String format(String format, String ownerName) {
        String name = ownerName == null || ownerName.isEmpty() ? "Someone" : ownerName;
        String pattern = format == null || format.isEmpty() ? DEFAULT_FORMAT : format;
        try {
            return String.format(pattern, name);
        } catch (RuntimeException e) {
            return String.format(DEFAULT_FORMAT, name);
        }
    }

    /** Vanilla's structure test passes the structure name as written; compare ignoring case. */
    public static boolean isHomeName(String structureName) {
        return structureName != null && TYPE.equalsIgnoreCase(structureName);
    }
}

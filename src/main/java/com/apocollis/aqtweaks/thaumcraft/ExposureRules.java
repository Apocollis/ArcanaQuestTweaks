package com.apocollis.aqtweaks.thaumcraft;

/** Pure decisions for the warp exposure pass. */
public final class ExposureRules {

    public static final String UNDER = "under";
    public static final String UNDER_DEEP = "underDeep";

    private ExposureRules() {}

    /**
     * Underground exposure key, or null. Needs the Y band and light at or below {@code maxLight};
     * {@code maxLight} 15 or more turns the darkness gate off.
     */
    public static String undergroundKey(int y, int yMin, int yMax, int light, int maxLight) {
        if (y > yMax) return null;
        if (maxLight < 15 && light > maxLight) return null;
        return y < yMin ? UNDER_DEEP : UNDER;
    }
}

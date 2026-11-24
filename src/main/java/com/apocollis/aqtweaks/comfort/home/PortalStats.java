package com.apocollis.aqtweaks.comfort.home;

/**
 * How the doors, fence gates and trapdoors of a home fill were judged. Pure counters, printed by
 * {@code /aqhome info}.
 */
public final class PortalStats {
    public static final PortalStats NONE = new PortalStats();

    /** Judged interior after the sub-fill closed; the ray test said clearly enclosed. */
    public int interiorRays;
    /** Judged interior after the sub-fill closed; the ray test was ambiguous. */
    public int interiorFill;
    /** Far side was already part of the home. */
    public int interiorKnown;
    /** Far side is open to the sky (rain-exposed). */
    public int exteriorOpenAir;
    /** Most rays from the far side escaped. */
    public int exteriorRays;
    /** The sub-fill did not close within its budget, hit the cell cap, or joined known outside space. */
    public int exteriorFill;
    /** More doors than {@code home_max_doors}, or no budget left for a nested door. */
    public int exteriorLimit;
    /** The far side was in an unloaded chunk or outside the world. */
    public int exteriorUnknown;

    public int interior() {
        return interiorRays + interiorFill + interiorKnown;
    }

    public int exterior() {
        return exteriorOpenAir + exteriorRays + exteriorFill + exteriorLimit + exteriorUnknown;
    }

    public int total() {
        return interior() + exterior();
    }

    public String summary() {
        if (total() == 0) {
            return "no doors";
        }
        return "doors: " + interior() + " interior (rays " + interiorRays + ", fill " + interiorFill + ", known "
                + interiorKnown + "), " + exterior() + " exterior (open air " + exteriorOpenAir + ", rays "
                + exteriorRays + ", fill " + exteriorFill + ", limit " + exteriorLimit + ", unloaded "
                + exteriorUnknown + ")";
    }
}

package com.apocollis.aqtweaks.comfort.home;

/** Runtime values for player homes; filled from {@code aqtweaks_comfort_settings.json} ({@code home_*} keys). */
public final class HomeConfig {

    public static boolean enabled = true;
    public static int maxCells = 4000;
    public static int fillCellsPerTick = 400;
    public static int recheckMinutes = 10;
    public static int rebuildCooldownTicks = 100;
    public static int fallbackRadius = 12;
    public static int fallbackDown = 6;
    public static int fallbackUp = 8;
    /** A player this close (horizontal, plus 16 vertical) to an unbuilt home's bed starts its build. */
    public static int triggerRange = 24;
    public static String nameFormat = HomeNames.DEFAULT_FORMAT;
    public static int doorRayRange = 16;
    public static double doorExteriorFraction = 0.70;
    public static double doorInteriorFraction = 0.30;
    public static int doorProbeCells = 800;
    public static int maxDoors = 24;

    private HomeConfig() {}

    public static HomeFiller.DoorSettings doorSettings() {
        HomeFiller.DoorSettings s = new HomeFiller.DoorSettings();
        s.rayRange = doorRayRange;
        s.exteriorFraction = doorExteriorFraction;
        s.interiorFraction = doorInteriorFraction;
        s.probeCells = doorProbeCells;
        s.maxDoors = maxDoors;
        return s;
    }

    public static long recheckTicks() {
        return Math.max(1, recheckMinutes) * 1200L;
    }
}

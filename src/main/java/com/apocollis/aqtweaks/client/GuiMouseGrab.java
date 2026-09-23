package com.apocollis.aqtweaks.client;

/**
 * Drops mouse look for a few camera frames after a GUI close so the recenter warp
 * is not applied as yaw.
 */
public final class GuiMouseGrab {

    private static int suppressFrames;

    private GuiMouseGrab() {
    }

    public static void armSuppress() {
        suppressFrames = 2;
    }

    /** True once per camera update while the post-close window is still open. */
    public static boolean consumeLook() {
        if (suppressFrames <= 0) {
            return false;
        }
        suppressFrames--;
        return true;
    }
}

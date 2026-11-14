package com.apocollis.aqtweaks.aether;

import com.apocollis.aqtweaks.aether.AetherSquallGear.GearClass;

/** How strong the lightning storm warning is at a point of the warning (0 to 1) for a gear class. No Minecraft types. */
public final class StormWarningMath {
    private StormWarningMath() {}

    /** Static-charge motes per tick on an exposed player: 1 to 6 with the warning, doubled or tripled by gear. */
    public static int moteCount(float warning, GearClass gear) {
        float w = Math.max(0.0F, Math.min(1.0F, warning));
        int base = 1 + Math.round(w * 5.0F);
        if (gear == GearClass.CHANNELING) return base * 3;
        if (gear == GearClass.CONDUCTIVE) return base * 2;
        return base;
    }

    /** Ticks between distant thunder rolls: about 4 s at the start of the warning, 1.5 s at the end. */
    public static int thunderGapTicks(float warning) {
        float w = Math.max(0.0F, Math.min(1.0F, warning));
        return Math.round(80.0F + (30.0F - 80.0F) * w);
    }

    /** Volume of a thunder roll: 0.15 at the start of the warning to 0.6 at the end. */
    public static float thunderVolume(float warning) {
        float w = Math.max(0.0F, Math.min(1.0F, warning));
        return 0.15F + 0.45F * w;
    }
}

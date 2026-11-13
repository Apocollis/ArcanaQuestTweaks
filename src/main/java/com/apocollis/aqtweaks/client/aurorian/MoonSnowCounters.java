package com.apocollis.aqtweaks.client.aurorian;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Live Moonfrost snow particle counts. The particle manager clears particles without calling {@code setExpired},
 * so the counts are reset by hand on disconnect, world unload and when leaving The Aurorian.
 */
@SideOnly(Side.CLIENT)
public final class MoonSnowCounters {
    private MoonSnowCounters() {}

    public static final AtomicInteger ACTIVE = new AtomicInteger(0);
    public static final AtomicInteger PERIMETER = new AtomicInteger(0);

    public static void reset() {
        ACTIVE.set(0);
        PERIMETER.set(0);
        MoonSnowRoof.reset();
    }
}

package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

public final class MagicSchoolFoundry {

    private static final ThreadLocal<Boolean> PULSING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private MagicSchoolFoundry() {}

    public static void pulse(ITickable tickable, TileEntity te) {
        if (Boolean.TRUE.equals(PULSING.get()) || te == null || tickable == null) {
            return;
        }
        World world = te.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        if (world.getTotalWorldTime() % magic.foundryPulseEvery != 0) {
            return;
        }
        if (!MagicSchoolPresence.nearby(world, te.getPos(), magic.groveRange, "aqtweaks:artificer",
                perks.artificer.enable)) {
            return;
        }
        PULSING.set(Boolean.TRUE);
        try {
            tickable.update();
        } finally {
            PULSING.set(Boolean.FALSE);
        }
    }
}

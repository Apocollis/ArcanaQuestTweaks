package com.apocollis.aqtweaks.thaumcraft.studious;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;

/** Steady Focus: the Scholar's focus cooldowns are shorter. Called from {@code MixinCasterManager.setCooldown}. */
public final class StudiousHooks {

    private StudiousHooks() {}

    public static int focusCooldownTicks(EntityLivingBase entity, int ticks) {
        if (ticks <= 0 || !(entity instanceof EntityPlayer) || entity instanceof FakePlayer) {
            return ticks;
        }
        double cut = StudiousModule.config().steadyFocusCooldownReduction;
        if (cut <= 0.0D || !StudiousModule.isScholar((EntityPlayer) entity)) {
            return ticks;
        }
        return StudiousMath.scaleCooldown(ticks, cut);
    }
}

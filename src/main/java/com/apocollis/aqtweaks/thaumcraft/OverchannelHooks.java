package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import thaumcraft.api.casters.FocusPackage;

/**
 * Neutral entry points for the Ley-Rupture overchannel (surge-exposed casters cast at a higher power and pay for it).
 * The mixin on {@code FocusEngine.castFocusPackage}, the Aegis channel and the Battlemage AI call here; the Twilight
 * Forest module registers the {@link Source}. No Twilight Forest type is imported, so any module can use it, and with
 * no source registered everything is a no-op.
 */
public final class OverchannelHooks {

    /** Implemented by the Twilight Forest module. */
    public interface Source {
        /** True while the entity is exposed in an active surge and overchanneling applies to it. */
        boolean overchanneling(EntityLivingBase entity);

        /** A cast is about to run with this (already copied) package: double it and charge the cost. */
        void onCast(EntityLivingBase caster, FocusPackage pkg);

        /** Aegis pool and cap multiplier for the raising player (1.0 when not overchanneling). */
        double aegisMultiplier(EntityPlayer player);

        /** Called every server tick while a player's Aegis stays raised; pays the hold cost at its interval. */
        void aegisHold(EntityPlayer player, int ticksHeld);
    }

    private static volatile Source source;

    private OverchannelHooks() {}

    public static void setSource(Source s) {
        source = s;
    }

    public static boolean overchanneling(EntityLivingBase entity) {
        Source s = source;
        return s != null && entity != null && s.overchanneling(entity);
    }

    public static void onCast(EntityLivingBase caster, FocusPackage pkg) {
        Source s = source;
        if (s != null && caster != null && pkg != null && !caster.world.isRemote) {
            s.onCast(caster, pkg);
        }
    }

    public static double aegisMultiplier(EntityPlayer player) {
        Source s = source;
        return s == null || player == null ? 1.0D : s.aegisMultiplier(player);
    }

    public static void aegisHold(EntityPlayer player, int ticksHeld) {
        Source s = source;
        if (s != null && player != null) {
            s.aegisHold(player, ticksHeld);
        }
    }
}

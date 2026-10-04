package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import com.apocollis.aqtweaks.reskillable.ReskillableBonuses;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.Loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Called from optional TC focus mixins. No Thaumcraft imports. The Reskillable multiply is a direct
 * call into {@link ReskillableBonuses}, guarded by {@link #RESKILLABLE} so Reskillable's classes are
 * only touched when the mod is present.
 */
public final class ThaumcraftFocusHooks {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Thaumcraft");

    private static final boolean RESKILLABLE = Loader.isModLoaded("reskillable");
    private static boolean warnedInvoke;

    private ThaumcraftFocusHooks() {}

    public static DamageSource markMagic(DamageSource source) {
        if (source == null) return source;
        source.setMagicDamage();
        return source;
    }

    public static void healScaled(EntityLivingBase target, Entity caster, float amount) {
        if (target == null) return;
        target.heal(scaleHeal(caster, target, amount));
    }

    public static float scaleOutgoing(Entity caster, float amount) {
        return scaleHeal(caster, null, amount);
    }

    private static float scaleHeal(Entity caster, EntityLivingBase target, float amount) {
        if (!(caster instanceof EntityPlayer) || amount == 0.0f || !RESKILLABLE) return amount;
        try {
            return ReskillableBonuses.scaleOutgoingHeal((EntityPlayer) caster, target, amount);
        } catch (RuntimeException | LinkageError t) {
            if (!warnedInvoke) {
                warnedInvoke = true;
                LOGGER.warn("[AQ-TC] Reskillable magic multiplier threw; focus output will not be "
                        + "scaled for this session", t);
            }
            return amount;
        }
    }
}

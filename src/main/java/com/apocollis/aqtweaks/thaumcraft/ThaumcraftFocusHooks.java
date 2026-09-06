package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * Called from optional TC focus mixins. No Thaumcraft imports. Reskillable multiply
 * is reflection so this class still loads when Reskillable is absent.
 */
public final class ThaumcraftFocusHooks {

    private static Method magicMultiplier;
    private static boolean resolved;

    private ThaumcraftFocusHooks() {}

    public static DamageSource markMagic(DamageSource source) {
        if (source == null) return source;
        source.setMagicDamage();
        return source;
    }

    public static void healScaled(EntityLivingBase target, Entity caster, float amount) {
        if (target == null) return;
        target.heal(scaleOutgoing(caster, amount));
    }

    public static float scaleOutgoing(Entity caster, float amount) {
        if (!(caster instanceof EntityPlayer) || amount == 0.0f) return amount;
        resolve();
        if (magicMultiplier == null) return amount;
        try {
            Object out = magicMultiplier.invoke(null, caster, Boolean.TRUE);
            if (out instanceof Number) {
                return amount * ((Number) out).floatValue();
            }
        } catch (Throwable ignored) {
        }
        return amount;
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!Loader.isModLoaded("reskillable")) return;
        try {
            Class<?> bonuses = Class.forName("com.apocollis.aqtweaks.reskillable.ReskillableBonuses");
            magicMultiplier = bonuses.getMethod("magicMultiplier", EntityPlayer.class, boolean.class);
        } catch (Throwable ignored) {
            magicMultiplier = null;
        }
    }
}

package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import com.apocollis.aqtweaks.reskillable.ReskillableBonuses;
import mod.emt.thaumictweaker.config.ConfigOverhaulsTT;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Called from TC focus mixins and events. The Reskillable multiply is a direct
 * call into {@link ReskillableBonuses}, guarded by {@link #RESKILLABLE} so Reskillable's classes are
 * only touched when the mod is present.
 */
public final class ThaumcraftFocusHooks {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Thaumcraft");

    private static final boolean RESKILLABLE = Loader.isModLoaded("reskillable");
    private static boolean warnedInvoke;
    private static boolean warnedNerfedHeal;
    private static final ThreadLocal<HealMark> HEAL = new ThreadLocal<>();

    private ThaumcraftFocusHooks() {}

    /** The living target of the current heal-focus execute, or null when the hit is not a living heal. */
    public static void beginHeal(Entity caster, EntityLivingBase target) {
        long worldTime = Long.MIN_VALUE;
        if (target != null && target.world != null) {
            worldTime = target.world.getTotalWorldTime();
        }
        HEAL.set(new HealMark(caster, target, worldTime));
    }

    public static void endHeal() {
        HEAL.remove();
    }

    /**
     * Scales only the marked focus target. Thaumic Tweaker's redirect still calls {@code heal}, which
     * fires this event, when Nerfed Heal Focus Effect is off.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHeal(LivingHealEvent event) {
        HealMark mark = HEAL.get();
        if (mark == null || mark.target == null) return;
        EntityLivingBase entity = event.getEntityLiving();
        if (entity != mark.target || entity.world == null || entity.world.isRemote) return;
        if (entity.world.getTotalWorldTime() != mark.worldTime) return;
        event.setAmount(scaleHeal(mark.caster, entity, event.getAmount()));
    }

    /** Once, when Thaumic Tweaker turns the heal focus into Regeneration and this scale never runs. */
    public static void warnNerfedHealFocus() {
        if (warnedNerfedHeal || !Loader.isModLoaded("thaumictweaker")) return;
        if (!ConfigOverhaulsTT.nerfedHealFocus) return;
        warnedNerfedHeal = true;
        LOGGER.info("[AQ-TC] Thaumic Tweaker 'Nerfed Heal Focus Effect' is on: heal focus applies "
                + "Regeneration and Tweaks' heal scaling is inactive");
    }

    public static DamageSource markMagic(DamageSource source) {
        if (source == null) return source;
        source.setMagicDamage();
        return source;
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

    private static final class HealMark {
        final Entity caster;
        final EntityLivingBase target;
        final long worldTime;

        HealMark(Entity caster, EntityLivingBase target, long worldTime) {
            this.caster = caster;
            this.target = target;
            this.worldTime = worldTime;
        }
    }
}

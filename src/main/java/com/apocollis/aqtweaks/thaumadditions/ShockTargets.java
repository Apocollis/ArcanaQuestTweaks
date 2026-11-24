package com.apocollis.aqtweaks.thaumadditions;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import thaumcraft.common.lib.utils.EntityUtils;

/** Who Shock may chain to and what counts as wet. */
public final class ShockTargets {
    private ShockTargets() {}

    public static boolean wet(Entity e) {
        if (e == null) return false;
        return ArcanaQuestTweaksConfig.ThaumAdditionsConfig.rainCountsAsWet ? e.isWet() : e.isInWater();
    }

    /** Alive, not the caster, not creative or spectator, and not an ally by Thaumcraft's own friendliness rule. */
    public static boolean validChainTarget(EntityLivingBase caster, Entity e) {
        if (!(e instanceof EntityLivingBase) || e.isDead || ((EntityLivingBase) e).getHealth() <= 0.0F) return false;
        if (caster != null && (e == caster || EntityUtils.isFriendly(caster, e))) return false;
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            if (p.isCreative() || p.isSpectator()) return false;
        }
        return true;
    }
}

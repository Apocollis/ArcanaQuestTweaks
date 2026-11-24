package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.thaumcraft.OverchannelHooks;
import com.apocollis.aqtweaks.thaumcraft.ThaumcraftHelper;
import com.apocollis.aqtweaks.weather.GearTierRule;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;

import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Ley-Rupture overchannel (plan 6.13 to 6.20). A caster exposed in an active surge casts at
 * {@code Overchannel Power Multiplier} and pays: backlash damage (armor-bypassing, absorbed by shields first) and, for
 * players only, 1 temporary warp per cast. An Aegis cast pays warp only and gets a doubled pool and cap; while the
 * shield stays up it pays warp again every 20 to 30 ticks. Mobs pay damage, never warp. Registered as the
 * {@link OverchannelHooks.Source}.
 */
public final class LeyOverchannel implements OverchannelHooks.Source {

    private static final String AEGIS_KEY = "focus.aqtweaks.aegis";

    private final Random rand = new Random();
    private final Map<UUID, Long> nextAegisPay = new HashMap<>();
    /** Mob exposure cache: entity id to {tick, exposure}. */
    private final Map<Integer, double[]> mobCache = new HashMap<>();

    private static ArcanaQuestTweaksConfig.LeyRupture cfg() {
        return ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
    }

    /** Exposure for overchanneling, 0 when it does not apply to this entity. */
    private double exposureOf(EntityLivingBase entity) {
        ArcanaQuestTweaksConfig.LeyRupture c = cfg();
        LeyRuptureHandler handler = LeyChannels.handler;
        if (!c.enable || !c.enableOverchannel || handler == null || entity.world == null || entity.world.isRemote
                || !LeyChannels.isTf(entity.world)) {
            return 0.0D;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.isCreative() || player.isSpectator() || !handler.active(player)) return 0.0D;
            return handler.exposureOf(player);
        }
        if (!c.overchannelMobs || !handler.activeIn(entity.world)) return 0.0D;
        long now = entity.world.getTotalWorldTime();
        double[] cached = mobCache.get(entity.getEntityId());
        if (cached != null && now - (long) cached[0] < 20L) return cached[1];
        BlockPos pos = new BlockPos(entity.posX, entity.posY, entity.posZ);
        double exposure = LeyTerrain.structureShelter(entity.world, pos) ? 0.0D
                : LeyTerrain.heightExposure(entity.world, entity.posX, entity.posY, entity.posZ);
        if (mobCache.size() > 256) mobCache.clear();
        mobCache.put(entity.getEntityId(), new double[] {now, exposure});
        return exposure;
    }

    @Override
    public boolean overchanneling(EntityLivingBase entity) {
        return exposureOf(entity) >= cfg().chargeMinExposure;
    }

    @Override
    public void onCast(EntityLivingBase caster, FocusPackage pkg) {
        double exposure = exposureOf(caster);
        ArcanaQuestTweaksConfig.LeyRupture c = cfg();
        if (exposure < c.chargeMinExposure) return;
        pkg.multiplyPower((float) c.overchannelMultiplier);
        boolean player = caster instanceof EntityPlayer;
        boolean aegis = player && FocusEngine.doesPackageContainElement(pkg, AEGIS_KEY);
        if (!aegis) {
            boolean proper = player && GearTierRule.allSlotsOnList((EntityPlayer) caster,
                    ArcanaQuestTweaksConfig.HazardGearConfig.twilightforest.properGear);
            double damage = SurgeMath.backlash(c.backlashDamage, exposure, proper, c.scourUnprotected, c.scourProper);
            if (damage > 0.0D) {
                caster.attackEntityFrom(LeyRuptureHandler.DAMAGE_LEY, (float) damage);
            }
            cue(caster);
        }
        if (player && c.backlashWarp > 0 && caster instanceof EntityPlayerMP) {
            warp((EntityPlayerMP) caster, c.backlashWarp);
        }
        if (aegis && caster instanceof EntityPlayerMP) {
            nextAegisPay.put(caster.getUniqueID(), caster.world.getTotalWorldTime() + roll(c));
        }
    }

    @Override
    public double aegisMultiplier(EntityPlayer player) {
        return exposureOf(player) >= cfg().chargeMinExposure ? cfg().overchannelMultiplier : 1.0D;
    }

    @Override
    public void aegisHold(EntityPlayer player, int ticksHeld) {
        ArcanaQuestTweaksConfig.LeyRupture c = cfg();
        if (!(player instanceof EntityPlayerMP) || exposureOf(player) < c.chargeMinExposure) {
            nextAegisPay.remove(player.getUniqueID());
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long due = nextAegisPay.get(player.getUniqueID());
        if (due == null) {
            nextAegisPay.put(player.getUniqueID(), now + roll(c));
            return;
        }
        if (now < due) return;
        nextAegisPay.put(player.getUniqueID(), now + roll(c));
        if (c.backlashWarp > 0) warp((EntityPlayerMP) player, c.backlashWarp);
    }

    /** The shield dropped or was never raised: forget its payment clock. */
    public void forget(UUID id) {
        nextAegisPay.remove(id);
    }

    private long roll(ArcanaQuestTweaksConfig.LeyRupture c) {
        int min = Math.max(1, Math.min(c.overchannelChannelMinTicks, c.overchannelChannelMaxTicks));
        int max = Math.max(min, Math.max(c.overchannelChannelMinTicks, c.overchannelChannelMaxTicks));
        return min + rand.nextInt(max - min + 1);
    }

    private static void warp(EntityPlayerMP player, int amount) {
        if (!Loader.isModLoaded("thaumcraft") || !ThaumcraftHelper.available()) return;
        ThaumcraftHelper.addWarp(player, 1, amount);
        ThaumcraftHelper.syncWarp(player);
    }

    private static void cue(EntityLivingBase caster) {
        if (!(caster.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) caster.world;
        world.spawnParticle(EnumParticleTypes.SPELL_WITCH, caster.posX, caster.posY + caster.height * 0.5D, caster.posZ,
                20, 0.3D, 0.3D, 0.3D, 0.2D);
        world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS, 0.5F, 1.6F);
    }
}

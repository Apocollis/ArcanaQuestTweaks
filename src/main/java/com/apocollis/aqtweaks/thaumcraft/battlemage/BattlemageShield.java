package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXShield;

/**
 * The faked runic shield of one Battlemage (plan 3.7). It lives in vanilla absorption, exactly as a player's stock runic
 * shield does, so damage order, saving and syncing are vanilla's. This class only ever raises the value (the recharge,
 * {@link ShieldModel}) and plays the hit cue; <b>nothing but damage lowers it</b>.
 *
 * <p>Tweaker's {@code RunicShieldingHandler} is deliberately not used: with the pack's Runic Shielding Overhaul off it is
 * plain absorption, and reading absorption directly keeps this class free of a Tweaker dependency.
 */
public final class BattlemageShield {

    private ShieldModel model;

    private ShieldModel model(AiTuning t) {
        if (model == null) {
            model = new ShieldModel(t.shieldRechargeIntervalTicks, t.shieldBreakWaitTicks, t.shieldResumeFraction);
        }
        return model;
    }

    /** Recharge, once per server tick. */
    public void tick(EntityCultistBattlemage mage) {
        double max = mage.getShieldMax();
        if (max <= 0.0D || !BattlemageConfig.shieldEnabled) {
            return;
        }
        AiTuning t = mage.getAi();
        float current = mage.getAbsorptionAmount();
        double next = model(t).next(mage.ticksExisted, current, max, true);
        if (next <= current) {
            return;
        }
        // A surge stops the refill (Ley-Rupture, plan 6.17); damage still lowers it as always.
        if (com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.surgeStopsMobShieldRecharge
                && com.apocollis.aqtweaks.thaumcraft.OverchannelHooks.overchanneling(mage)) {
            return;
        }
        if (BattlemageConfig.shieldDrainsAura && AuraHelper.drainVis(mage.world, new BlockPos(mage), 1.0F, false) < 1.0F) {
            return;
        }
        mage.setAbsorptionAmount((float) next);
    }

    /** True while the shield is broken or still rebuilding: the mage keeps its wider band and peels sooner. */
    public boolean widenBand(EntityCultistBattlemage mage) {
        double max = mage.getShieldMax();
        if (max <= 0.0D || !BattlemageConfig.shieldEnabled) {
            return false;
        }
        return model(mage.getAi()).widenBand(mage.getAbsorptionAmount(), max);
    }

    /**
     * {@code LivingHurtEvent} for a mage: effects only, damage is untouched. When the shield is up at impact the real
     * {@code PacketFXShield} and the runic charge sound go out within 32 blocks, as for a player; a big hit or a hit that
     * breaks the shield staggers the mage for a moment.
     */
    public static void onHurt(EntityCultistBattlemage mage, LivingHurtEvent event) {
        if (mage.world.isRemote) {
            return;
        }
        AiTuning t = mage.getAi();
        float absorption = mage.getAbsorptionAmount();
        if (BattlemageConfig.shieldEnabled && absorption > 0.0F && mage.getShieldMax() > 0.0D) {
            int attacker = event.getSource().getTrueSource() != null ? event.getSource().getTrueSource().getEntityId() : -2;
            PacketHandler.INSTANCE.sendToAllAround(new PacketFXShield(attacker, mage.getEntityId()),
                    new NetworkRegistry.TargetPoint(mage.world.provider.getDimension(), mage.posX, mage.posY, mage.posZ, 32.0D));
            mage.world.playSound(null, mage.posX, mage.posY, mage.posZ, SoundsTC.runicShieldCharge, SoundCategory.HOSTILE,
                    0.66F, 1.0F + (mage.getRNG().nextFloat() - mage.getRNG().nextFloat()) * 0.2F);
        }
        float amount = event.getAmount();
        boolean breaks = absorption > 0.0F && amount >= absorption;
        boolean big = amount >= mage.getMaxHealth() * t.staggerDamageFraction;
        if (t.staggerTicks > 0 && (breaks || big)) {
            mage.stagger(t.staggerTicks);
        }
    }
}

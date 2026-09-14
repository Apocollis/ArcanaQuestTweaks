package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.potion.PerkCooldownEffects;
import com.apocollis.aqtweaks.potion.PotionPerkCooldown;
import com.apocollis.aqtweaks.stamina.StaminaPerks;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RespiteHandler {

    public static final String NBT_UNTIL = "StaminaTweaksRespiteUntil";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP player)) return;
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        var cfg = ArcanaQuestTweaksConfig.ReskillableModuleConfig.respite;
        if (!StaminaPerks.unlocked(player, cfg.perkId)) return;

        DamageSource source = event.getSource();
        if (source != null && source.canHarmInCreative()) return;

        float amount = event.getAmount();
        if (amount <= 0.0f || amount < player.getHealth()) return;

        NBTTagCompound data = player.getEntityData();
        int now = player.ticksExisted;
        if (data.getInteger(NBT_UNTIL) > now) return;

        event.setAmount(0.0f);

        int cooldown = Math.max(0, cfg.cooldownTicks);
        data.setInteger(NBT_UNTIL, now + cooldown);
        PerkCooldownEffects.apply(player, PotionPerkCooldown.RESPITE, cooldown);

        int duration = Math.max(1, cfg.effectDurationTicks);
        player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, duration, Math.max(0, cfg.regenAmplifier), false, true));

        Potion defence = Potion.getPotionFromResourceLocation(cfg.defencePotionId);
        if (defence != null) {
            player.addPotionEffect(new PotionEffect(defence, duration, Math.max(0, cfg.defenceAmplifier), false, true));
        }
    }
}

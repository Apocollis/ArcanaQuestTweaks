package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class MagicSchoolEffects {

    private MagicSchoolEffects() {}

    public static void onOutgoingHurt(EntityPlayer player, LivingHurtEvent event) {
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        float amount = event.getAmount();
        DamageSource source = event.getSource();
        if (starPoweredActive(player)
                && MagicSchoolPresence.unlocked(player, "aqtweaks:star_powered", perks.starPowered.enable)) {
            amount *= (float) magic.starPoweredDamage;
        }
        if (MagicSchoolPresence.unlocked(player, "aqtweaks:living_edge", perks.livingEdge.enable)
                && isBotaniaDamage(source)) {
            amount *= (float) magic.livingEdge;
        }
        if (MagicSchoolPresence.unlocked(player, "aqtweaks:live_spark", perks.liveSpark.enable)
                && isEmberDamage(source)) {
            amount *= (float) magic.liveSpark;
        }
        event.setAmount(amount);
    }

    public static void onIncomingHurt(EntityPlayer player, LivingHurtEvent event) {
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        DamageSource source = event.getSource();
        if (source != null && source.canHarmInCreative()) {
            return;
        }
        if (source != null && "outOfWorld".equals(source.getDamageType())) {
            return;
        }
        if (MagicSchoolPresence.unlocked(player, "aqtweaks:cinder_ward", perks.cinderWard.enable)
                && source != null && source.isFireDamage()
                && !player.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
            event.setAmount(event.getAmount() * (float) magic.cinderWardFire);
        }
        if (MagicSchoolPresence.unlocked(player, "aqtweaks:mana_veil", perks.manaVeil.enable)
                && Loader.isModLoaded("botania")) {
            MagicSchoolBotania.applyManaVeil(player, event);
        }
    }

    public static void onIncomingAttack(EntityPlayer player, LivingAttackEvent event) {
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        DamageSource source = event.getSource();
        if (!MagicSchoolPresence.unlocked(player, "aqtweaks:cinder_ward", perks.cinderWard.enable)) {
            return;
        }
        if (source == null || !source.isFireDamage()) {
            return;
        }
        if (!player.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
            return;
        }
        float heal = event.getAmount() * (float) magic.cinderWardHeal;
        if (heal > 0f) {
            player.heal(heal);
        }
    }

    public static void onPlayerTick(EntityPlayer player, TickEvent.Phase phase) {
        if (phase != TickEvent.Phase.END || player.world == null || player.world.isRemote) {
            return;
        }
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        boolean want = MagicSchoolPresence.unlocked(player, "aqtweaks:star_powered", perks.starPowered.enable)
                && starPoweredActive(player);
        keepAmbient(player, MobEffects.REGENERATION, want);
        Potion replenish = Potion.getPotionFromResourceLocation("elenaidodge2:replenishment");
        if (replenish != null) {
            keepAmbient(player, replenish, want);
        }
    }

    public static boolean starPoweredActive(EntityPlayer player) {
        if (player.world == null) {
            return false;
        }
        BlockPos pos = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        return player.world.canSeeSky(pos) && !player.world.isDaytime();
    }

    private static void keepAmbient(EntityPlayer player, Potion potion, boolean want) {
        if (potion == null) {
            return;
        }
        PotionEffect current = player.getActivePotionEffect(potion);
        if (want) {
            if (current == null || current.getAmplifier() != 0 || current.getDuration() < 40) {
                player.addPotionEffect(new PotionEffect(potion, 80, 0, true, false));
            }
            return;
        }
        if (current != null && current.getIsAmbient() && current.getAmplifier() == 0) {
            player.removePotionEffect(potion);
        }
    }

    private static boolean isEmberDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        if ("ember".equals(source.getDamageType())) {
            return true;
        }
        return source.getClass().getName().startsWith("teamroots.embers.damage");
    }

    private static boolean isBotaniaDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        Entity immediate = source.getImmediateSource();
        if (immediate != null && immediate.getClass().getName().startsWith("vazkii.botania.")) {
            return true;
        }
        Entity trueSource = source.getTrueSource();
        if (trueSource instanceof EntityPlayer player) {
            net.minecraft.item.ItemStack held = player.getHeldItemMainhand();
            if (!held.isEmpty() && held.getItem().getRegistryName() != null
                    && "botania".equals(held.getItem().getRegistryName().getNamespace())) {
                return true;
            }
        }
        return source.getClass().getName().startsWith("vazkii.botania.");
    }
}

package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.potion.PerkCooldownEffects;
import com.apocollis.aqtweaks.potion.PotionPerkCooldown;
import com.apocollis.aqtweaks.stamina.StaminaModule.WeaponType;
import com.apocollis.aqtweaks.util.Reflect;
import com.elenai.elenaidodge2.ModConfig;
import com.elenai.elenaidodge2.api.FeathersHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

public final class StaminaPerks {

    public static final String NBT_ADRENALINE_UNTIL = "StaminaTweaksAdrenalineUntil";
    public static final String NBT_EVASION_UNTIL = "StaminaTweaksEvasionUntil";
    public static final String NBT_POWER_ATTACK = "StaminaTweaksPowerAttack";
    public static final String NBT_ATTACK_PENALTY = "StaminaTweaksAttackPenalty";

    private StaminaPerks() {}

    public static boolean unlocked(EntityPlayer player, String id) {
        return ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable
                && Reflect.hasUnlockable(player, id);
    }

    public static int minus(int cost, EntityPlayer player, String id, int reduction) {
        if (!unlocked(player, id)) return cost;
        return Math.max(0, cost - reduction);
    }

    public static int gatheringForageCost(EntityPlayer player, int base,
            net.minecraft.world.World world, net.minecraft.block.state.IBlockState state) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        if (!unlocked(player, cfg.gatheringEfficiencyPerkId)) return base;
        if (!isForageBlock(world, state)) return base;
        return Math.max(0, base - cfg.gatheringEfficiencyReduction);
    }

    private static boolean isForageBlock(net.minecraft.world.World world,
            net.minecraft.block.state.IBlockState state) {
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("reskillable")) return false;
        try {
            Object out = Class.forName("com.apocollis.aqtweaks.reskillable.ReskillableBonuses")
                    .getMethod("isForageBlock", net.minecraft.world.World.class,
                            net.minecraft.block.state.IBlockState.class)
                    .invoke(null, world, state);
            return Boolean.TRUE.equals(out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int meleeCost(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        return minus(base, player, cfg.meleeEfficiencyPerkId, cfg.meleeEfficiencyReduction);
    }

    public static int jumpCost(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        return minus(base, player, cfg.cardioMasterPerkId, cfg.cardioMasterReduction);
    }

    public static int sprintCost(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        return minus(base, player, cfg.cardioMasterPerkId, cfg.cardioMasterReduction);
    }

    public static int climbCost(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        return minus(base, player, cfg.expertClimberPerkId, cfg.expertClimberReduction);
    }

    public static int bowDrawCost(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        return minus(base, player, cfg.rangedEfficiencyPerkId, cfg.rangedDrawReduction);
    }

    public static int bowHoldInterval(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        if (!unlocked(player, cfg.rangedEfficiencyPerkId)) return Math.max(1, base);
        return Math.max(1, Math.round(base * (float) cfg.rangedHoldIntervalMultiplier));
    }

    public static int shieldHoldInterval(EntityPlayer player, int base) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        if (!unlocked(player, cfg.shieldEfficiencyPerkId)) return Math.max(1, base);
        return Math.max(1, Math.round(base * (float) cfg.shieldHoldIntervalMultiplier));
    }

    public static void tryAdrenaline(EntityPlayer player, float damageAmount) {
        if (!(player instanceof EntityPlayerMP)) return;
        if (player.capabilities.isCreativeMode || player.isSpectator()) return;
        if (damageAmount <= 0.0f) return;
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        if (!unlocked(player, cfg.adrenalinePerkId)) return;

        EntityPlayerMP mp = (EntityPlayerMP) player;
        int current = FeathersHelper.getFeatherLevel(mp);
        if (current >= cfg.adrenalineThreshold) return;

        NBTTagCompound data = player.getEntityData();
        int now = player.ticksExisted;
        if (data.getInteger(NBT_ADRENALINE_UNTIL) > now) return;

        int max = FeathersHelper.getMaxFeatherLevel(mp);
        int target = Math.min(cfg.adrenalineRestore, max);
        if (current >= target) return;
        FeathersHelper.increaseFeathers(mp, target - current);
        data.setInteger("StaminaTweaksPrevFeathers", FeathersHelper.getFeatherLevel(mp));
        int cooldown = Math.max(0, cfg.adrenalineCooldownTicks);
        data.setInteger(NBT_ADRENALINE_UNTIL, now + cooldown);
        PerkCooldownEffects.apply(player, PotionPerkCooldown.ADRENALINE, cooldown);
    }

    public static boolean tryEvasion(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP player)) return false;
        if (player.capabilities.isCreativeMode || player.isSpectator()) return false;
        if (event.getAmount() <= 0.0f) return false;

        Entity source = event.getSource() != null ? event.getSource().getTrueSource() : null;
        if (!(source instanceof EntityLivingBase) || source == player) return false;

        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        if (!unlocked(player, cfg.evasionPerkId)) return false;

        NBTTagCompound data = player.getEntityData();
        int now = player.ticksExisted;
        if (data.getInteger(NBT_EVASION_UNTIL) > now) return false;

        int dodgeCost = elenaiDodgeCost(player);
        if (dodgeCost > 0 && !Reflect.hasEnoughStamina(player, dodgeCost)) return false;
        if (dodgeCost > 0) {
            Reflect.decreaseFeathers(player, dodgeCost);
        }

        int cooldown = Math.max(0, cfg.evasionCooldownTicks);
        data.setInteger(NBT_EVASION_UNTIL, now + cooldown);
        PerkCooldownEffects.apply(player, PotionPerkCooldown.EVASION, cooldown);
        playDodgeSound(player);
        event.setCanceled(true);
        return true;
    }

    public static boolean tryPowerAttack(EntityPlayerMP player, WeaponType type, int meleeCost) {
        if (type != WeaponType.MEDIUM && type != WeaponType.HEAVY) return false;
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable;
        if (!unlocked(player, cfg.powerAttackPerkId)) return false;

        int current = FeathersHelper.getFeatherLevel(player);
        int max = FeathersHelper.getMaxFeatherLevel(player);
        if (current != max) return false;

        int extra = Math.max(0, cfg.powerAttackExtraSpend);
        int total = meleeCost + extra;
        if (total > 0 && !Reflect.hasEnoughStamina(player, total)) return false;
        if (total > 0) {
            Reflect.decreaseFeathers(player, total);
        }
        double multiplier = type == WeaponType.HEAVY
                ? cfg.powerAttackHeavyMultiplier
                : cfg.powerAttackMediumMultiplier;
        player.getEntityData().setDouble(NBT_POWER_ATTACK, multiplier);
        return true;
    }

    public static void applyOutgoingMeleeModifiers(Entity attacker, net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(attacker instanceof EntityPlayer)) return;
        NBTTagCompound data = attacker.getEntityData();
        if (data.hasKey(NBT_POWER_ATTACK)) {
            event.setAmount((float) (event.getAmount() * data.getDouble(NBT_POWER_ATTACK)));
            data.removeTag(NBT_POWER_ATTACK);
            return;
        }
        if (data.hasKey(NBT_ATTACK_PENALTY)) {
            event.setAmount((float) (event.getAmount() * data.getDouble(NBT_ATTACK_PENALTY)));
            data.removeTag(NBT_ATTACK_PENALTY);
        }
    }

    private static int elenaiDodgeCost(EntityPlayer player) {
        var feathers = ModConfig.common != null ? ModConfig.common.feathers : null;
        if (feathers == null) return 0;
        return player.onGround ? feathers.cost : feathers.airborneCost;
    }

    private static void playDodgeSound(EntityPlayerMP player) {
        boolean enhanced = ModConfig.common != null
                && ModConfig.common.misc != null
                && ModConfig.common.misc.enhancedDodgeEffects;
        float volume = enhanced ? 0.55F : 0.8F;
        float pitch = enhanced ? 2.9F : 4.0F;
        BlockPos pos = player.getPosition();
        player.world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, volume, pitch);
    }
}

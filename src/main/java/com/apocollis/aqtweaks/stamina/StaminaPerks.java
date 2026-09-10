package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.elenai.elenaidodge2.api.FeathersHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public final class StaminaPerks {

    private StaminaPerks() {}

    public static boolean unlocked(EntityPlayer player, String id) {
        return ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable
                && Reflect.hasUnlockable(player, id);
    }

    public static int minus(int cost, EntityPlayer player, String id, int reduction) {
        if (!unlocked(player, id)) return cost;
        return Math.max(0, cost - reduction);
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
        if (data.getInteger("StaminaTweaksAdrenalineUntil") > now) return;

        int max = FeathersHelper.getMaxFeatherLevel(mp);
        int target = Math.min(cfg.adrenalineRestore, max);
        if (current >= target) return;
        FeathersHelper.increaseFeathers(mp, target - current);
        data.setInteger("StaminaTweaksPrevFeathers", FeathersHelper.getFeatherLevel(mp));
        data.setInteger("StaminaTweaksAdrenalineUntil", now + cfg.adrenalineCooldownTicks);
    }
}

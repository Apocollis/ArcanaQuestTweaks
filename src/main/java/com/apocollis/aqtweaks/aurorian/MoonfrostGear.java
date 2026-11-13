package com.apocollis.aqtweaks.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.weather.Wildcard;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/**
 * Atum's whole-set rule for the cold: EXPOSED (any slot empty), ARMORED (four ordinary pieces) or PROPER.
 * PROPER means the configured effect is active (the pack's SetBonus gives {@code simpledifficulty:cold_resist}
 * to full Aurorian, Arctic and Yeti sets; a Cold Resistance potion gives it too) or all four slots are on the
 * proper-gear whitelist. Atum armor gives heat resist, so it is ordinary armor here.
 */
public final class MoonfrostGear {
    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
    };

    private MoonfrostGear() {}

    public static MoonfrostTier tierOf(EntityPlayer player) {
        boolean allArmor = true;
        boolean allWhitelisted = true;
        for (EntityEquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            boolean armor = !stack.isEmpty() && stack.getItem() instanceof ItemArmor;
            if (!armor) {
                allArmor = false;
                allWhitelisted = false;
                break;
            }
            if (allWhitelisted && !isWhitelisted(stack)) {
                allWhitelisted = false;
            }
        }
        return MoonfrostTier.classify(allArmor, allArmor && hasGearEffect(player), allWhitelisted);
    }

    private static boolean isWhitelisted(ItemStack stack) {
        ResourceLocation reg = stack.getItem().getRegistryName();
        return reg != null && Wildcard.matchesAny(reg.toString(),
                ArcanaQuestTweaksConfig.AurorianModuleConfig.properGearWhitelist);
    }

    private static boolean hasGearEffect(EntityPlayer player) {
        String name = ArcanaQuestTweaksConfig.AurorianModuleConfig.properGearEffect;
        if (name == null || name.trim().isEmpty()) return false;
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(name.trim()));
        return potion != null && player.isPotionActive(potion);
    }

    public static float frostbiteDamage(MoonfrostTier tier) {
        switch (tier) {
            case EXPOSED:
                return (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.frostbiteDamageExposed;
            case PROPER:
                return (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.frostbiteDamageProper;
            default:
                return (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.frostbiteDamageArmored;
        }
    }

    public static double rimeGain(MoonfrostTier tier) {
        switch (tier) {
            case EXPOSED:
                return ArcanaQuestTweaksConfig.AurorianModuleConfig.rimeGainExposed;
            case PROPER:
                return ArcanaQuestTweaksConfig.AurorianModuleConfig.rimeGainProper;
            default:
                return ArcanaQuestTweaksConfig.AurorianModuleConfig.rimeGainArmored;
        }
    }

    public static double regenMultiplier(MoonfrostTier tier) {
        switch (tier) {
            case EXPOSED:
                return ArcanaQuestTweaksConfig.AurorianModuleConfig.regenIntervalMultiplierExposed;
            case PROPER:
                return ArcanaQuestTweaksConfig.AurorianModuleConfig.regenIntervalMultiplierProper;
            default:
                return ArcanaQuestTweaksConfig.AurorianModuleConfig.regenIntervalMultiplierArmored;
        }
    }
}

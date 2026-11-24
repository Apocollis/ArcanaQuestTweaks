package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.DssWeaponConfig;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Weapon-category values for the DSS balance mixins and the heavy armor bypass (plan 20261122-0327). */
public final class DssWeaponBalance {

    private DssWeaponBalance() {}

    private static StaminaModule.WeaponType typeOf(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        return StaminaModule.getWeaponType(held);
    }

    /** Factor for DSS's flat combo bonus, by the stamina weapon category of the main-hand item. */
    public static float comboBonusMultiplier(EntityPlayer player) {
        if (!DssWeaponConfig.scaleComboBonus) return 1.0F;
        switch (typeOf(player)) {
            case LIGHT: return (float) DssWeaponConfig.comboBonusLight;
            case MEDIUM: return (float) DssWeaponConfig.comboBonusMedium;
            case HEAVY: return (float) DssWeaponConfig.comboBonusHeavy;
            default: return (float) DssWeaponConfig.comboBonusUnclassified;
        }
    }

    /** Share (0 to 1) of the target's armor reduction a melee hit ignores. */
    public static float armorBypassShare(EntityPlayer player) {
        if (!DssWeaponConfig.enableArmorBypass) return 0.0F;
        switch (typeOf(player)) {
            case LIGHT: return (float) DssWeaponConfig.armorBypassLight;
            case MEDIUM: return (float) DssWeaponConfig.armorBypassMedium;
            case HEAVY: return (float) DssWeaponConfig.armorBypassHeavy;
            default: return 0.0F;
        }
    }

    /** Ticks of one full attack cooldown for this player, from the attack speed attribute (at least 1). */
    public static int attackPeriodTicks(EntityPlayer player) {
        double speed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).getAttributeValue();
        double ticks = 20.0D / Math.max(0.1D, speed);
        return (int) Math.min(200.0D, Math.max(1.0D, Math.ceil(ticks)));
    }
}

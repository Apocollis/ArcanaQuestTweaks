package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.stamina.DssWeaponBalance;
import dynamicswordskills.DSSCombatEvents;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * DSS adds a flat +1 damage per combo hit already landed in {@code onHurt}. That bonus is scaled by the weapon
 * category of the stamina module (light, medium, heavy).
 */
@Mixin(value = DSSCombatEvents.class, remap = false)
public abstract class MixinDSSCombatEvents {

    @Redirect(
            method = "onHurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/living/LivingHurtEvent;setAmount(F)V"))
    private void aqtweaks$scaleComboBonus(LivingHurtEvent event, float newAmount) {
        float base = event.getAmount();
        if (event.getSource().getTrueSource() instanceof EntityPlayer player) {
            newAmount = base + (newAmount - base) * DssWeaponBalance.comboBonusMultiplier(player);
        }
        event.setAmount(newAmount);
    }
}

package com.apocollis.aqtweaks.mixin.elenaidodge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Elenai's Replenishment applies its own regeneration modifier ({@code (amplifier + 1) * 20 + 30} ticks off the
 * interval, which clamps to one half-feather per tick). Tweaks sets the interval itself from the amplifier
 * (StaminaRegenPace, plan 20261122-0610), so the potion's own modifier is cancelled on both start and end. The potion
 * has no attribute modifiers, so skipping the super call changes nothing else.
 */
@Mixin(targets = "com.elenai.elenaidodge2.potions.ReplenishmentPotion", remap = false)
public class MixinReplenishmentPotion {

    @Inject(method = "func_111185_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$noModifierOnStart(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "func_111187_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$noModifierOnEnd(CallbackInfo ci) {
        ci.cancel();
    }
}

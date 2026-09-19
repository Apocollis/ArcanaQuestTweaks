package com.apocollis.aqtweaks.mixin.effortlessbuilding;

import com.apocollis.aqtweaks.reskillable.EffortlessBuildingHooks;
import net.minecraft.entity.player.EntityPlayer;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager.ModifierSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModifierSettingsManager.class, remap = false)
public abstract class MixinModifierSettingsManager {

    @Inject(method = "sanitize", at = @At("RETURN"))
    private static void aqtweaks$clampQuickReplace(ModifierSettings settings, EntityPlayer player,
            CallbackInfoReturnable<String> cir) {
        if (settings == null || player == null) return;
        if (settings.doQuickReplace() && !EffortlessBuildingHooks.allowQuickReplace(player)) {
            settings.setQuickReplace(false);
        }
    }
}

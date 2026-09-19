package com.apocollis.aqtweaks.mixin.effortlessbuilding;

import com.apocollis.aqtweaks.reskillable.EffortlessBuildingHooks;
import net.minecraft.entity.player.EntityPlayer;
import nl.requios.effortlessbuilding.buildmode.BuildModes.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModeSettingsManager.class, remap = false)
public abstract class MixinModeSettingsManager {

    @Inject(method = "sanitize", at = @At("RETURN"))
    private static void aqtweaks$clampBuildMode(ModeSettings settings, EntityPlayer player,
            CallbackInfoReturnable<String> cir) {
        if (settings == null || player == null) return;
        BuildModeEnum mode = settings.getBuildMode();
        if (mode == null) return;
        String clamped = EffortlessBuildingHooks.clampBuildMode(player, mode.name());
        if (!clamped.equals(mode.name())) {
            settings.setBuildMode(BuildModeEnum.valueOf(clamped));
        }
    }
}

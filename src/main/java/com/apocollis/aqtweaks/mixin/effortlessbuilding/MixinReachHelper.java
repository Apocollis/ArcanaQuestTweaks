package com.apocollis.aqtweaks.mixin.effortlessbuilding;

import com.apocollis.aqtweaks.reskillable.EffortlessBuildingHooks;
import net.minecraft.entity.player.EntityPlayer;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ReachHelper.class, remap = false)
public abstract class MixinReachHelper {

    @Inject(method = "getPlacementReach", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$buildingPlaceReach(EntityPlayer player, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(EffortlessBuildingHooks.placementReach(player, cir.getReturnValueI()));
    }

    @Inject(method = "getMaxBlocksPlacedAtOnce", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$buildingMaxBlocks(EntityPlayer player, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(EffortlessBuildingHooks.maxBlocksPlaced(player, cir.getReturnValueI()));
    }
}

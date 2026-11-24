package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageTouchHooks;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.items.casters.foci.FocusMediumTouch;

/**
 * Touch and Bolt for a mob. Stock {@code supplyTrajectories} casts the caster to a player for its reach, and
 * {@code supplyTargets} returns nothing for any non-player (Bolt inherits both). For a non-player caster these
 * return the same results with a mob reach; for a player the stock code runs untouched.
 */
@Mixin(value = FocusMediumTouch.class, remap = false)
public abstract class MixinFocusMediumTouch {

    @Inject(method = "supplyTrajectories", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$mobTrajectories(CallbackInfoReturnable<Trajectory[]> cir) {
        FocusMediumTouch self = (FocusMediumTouch) (Object) this;
        if (BattlemageTouchHooks.isNonPlayerCaster(self)) {
            cir.setReturnValue(BattlemageTouchHooks.trajectories(self));
        }
    }

    @Inject(method = "supplyTargets", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$mobTargets(CallbackInfoReturnable<RayTraceResult[]> cir) {
        FocusMediumTouch self = (FocusMediumTouch) (Object) this;
        if (BattlemageTouchHooks.isNonPlayerCaster(self)) {
            cir.setReturnValue(BattlemageTouchHooks.targets(self));
        }
    }
}

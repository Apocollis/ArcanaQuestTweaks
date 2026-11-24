package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekHooks;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.utils.EntityUtils;

/** A valid player is never friendly to a Battlemage, whatever the server PvP flag says (plan 2.2). */
@Mixin(value = EntityUtils.class, remap = false)
public abstract class MixinEntityUtilsFriendly {

    @Inject(method = "isFriendly", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$battlemageHostileToPlayers(Entity caster, Entity target,
            CallbackInfoReturnable<Boolean> cir) {
        if (BattlemageSeekHooks.playerNeverFriendly(caster, target)) {
            cir.setReturnValue(false);
        }
    }
}

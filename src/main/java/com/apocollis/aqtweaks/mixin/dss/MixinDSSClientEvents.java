package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.DssWeaponConfig;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.skills.ILockOnTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DSS's own swing limiter is a flat tick count for every weapon (and is 0 in this pack). A lock-on or combo swing now
 * also waits for the weapon's vanilla attack cooldown, so no weapon swings faster than its attack speed. Skills and
 * special attacks do not go through this method.
 */
@Mixin(value = DSSClientEvents.class, remap = false)
public abstract class MixinDSSClientEvents {

    @Inject(method = "performComboAttack", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$waitForWeaponCooldown(Minecraft mc, ILockOnTarget skill, CallbackInfo ci) {
        if (!DssWeaponConfig.enforceWeaponSpeed) return;
        EntityPlayerSP player = mc.player;
        if (player == null) return;
        if (player.getCooledAttackStrength(0.0F) < (float) DssWeaponConfig.attackReadyThreshold) {
            ci.cancel();
        }
    }
}

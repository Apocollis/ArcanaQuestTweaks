package com.apocollis.aqtweaks.mixin.gaia;

import com.apocollis.aqtweaks.gaia.GaiaPierce;
import gaia.entity.ai.Ranged;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Ranged.class, remap = false)
public abstract class MixinRanged {

    @Redirect(
            method = "rangedAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/EntityTippedArrow;func_184558_a:(Lnet/minecraft/potion/PotionEffect;)V"
            )
    )
    private static void aqtweaks$skipInstantTip(EntityTippedArrow arrow, PotionEffect effect) {
        GaiaPierce.onArcherInstantTip(arrow, effect);
    }
}

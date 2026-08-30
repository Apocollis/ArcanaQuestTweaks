package com.apocollis.aqtweaks.mixin.gaia;

import com.apocollis.aqtweaks.gaia.GaiaPierce;
import gaia.entity.EntityMobHostileBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityMobHostileBase.class, remap = false)
public abstract class MixinEntityMobHostileBase {

    @Redirect(
            method = "func_70652_k",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;func_70690_d:(Lnet/minecraft/potion/PotionEffect;)V"
            )
    )
    private void aqtweaks$skipMeleeInstantDamage(EntityLivingBase victim, PotionEffect effect) {
        GaiaPierce.onMeleeInstantDamage(victim, effect);
    }
}

package com.apocollis.aqtweaks.mixin.gaia;

import com.apocollis.aqtweaks.gaia.GaiaPierce;
import gaia.entity.projectile.EntityGaiaProjectileBomb;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityGaiaProjectileBomb.class, remap = false)
public abstract class MixinEntityGaiaProjectileBomb {

    @Redirect(
            method = "func_70184_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    remap = true
            )
    )
    private boolean aqtweaks$retypeBomb(Entity victim, DamageSource source, float amount) {
        return GaiaPierce.onBombHit((Entity) (Object) this, victim, source, amount);
    }
}

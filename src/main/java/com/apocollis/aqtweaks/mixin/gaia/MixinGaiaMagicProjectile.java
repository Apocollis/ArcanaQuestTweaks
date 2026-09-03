package com.apocollis.aqtweaks.mixin.gaia;

import com.apocollis.aqtweaks.gaia.GaiaPierce;
import gaia.entity.projectile.EntityGaiaProjectileBubble;
import gaia.entity.projectile.EntityGaiaProjectileMagic;
import gaia.entity.projectile.EntityGaiaProjectileMagicRandom;
import gaia.entity.projectile.EntityGaiaProjectilePoison;
import gaia.entity.projectile.EntityGaiaProjectileWeb;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {
        EntityGaiaProjectileMagic.class,
        EntityGaiaProjectileMagicRandom.class,
        EntityGaiaProjectileBubble.class,
        EntityGaiaProjectilePoison.class,
        EntityGaiaProjectileWeb.class
}, remap = false)
public abstract class MixinGaiaMagicProjectile {

    @Redirect(
            method = "func_70227_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    remap = true
            )
    )
    private boolean aqtweaks$retypeMagic(Entity victim, DamageSource source, float amount) {
        return GaiaPierce.onMagicBoltHit((Entity) (Object) this, victim, source, amount);
    }
}

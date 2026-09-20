package com.apocollis.aqtweaks.mixin.dss;

import dynamicswordskills.entity.EntitySwordBeam;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntitySwordBeam.class, remap = false)
public abstract class MixinEntitySwordBeam {

    @Redirect(
            method = "func_70184_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    remap = true
            )
    )
    private boolean aqtweaks$stampBeamMagic(Entity target, DamageSource source, float amount) {
        if (source != null) {
            source.setMagicDamage();
            source.setDamageBypassesArmor();
        }
        return target.attackEntityFrom(source, amount);
    }
}

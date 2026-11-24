package com.apocollis.aqtweaks.mixin.explosion;

import com.apocollis.aqtweaks.botania.BurstShooter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Botania's Explosive and Storm lenses explode with the mana burst as the exploder, and vanilla only credits a
 * living exploder or primed TNT, so the blast damage has no attacker. When the exploder is a Botania mana burst
 * with a shooter, the shooter is reported as who placed it: victims take player-sourced explosion damage (same
 * amount) and mobs target the shooter. The burst stays the exploder, so the shooter still takes their own blast.
 * Early json, so no Botania import: the burst is matched by class name.
 */
@Mixin(Explosion.class)
public abstract class MixinExplosionBurstOwner {

    private static final String MANA_BURST = "vazkii.botania.common.entity.EntityManaBurst";

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private Entity exploder;

    @Inject(method = "getExplosivePlacedBy", at = @At("RETURN"), cancellable = true, require = 0)
    private void aqtweaks$burstShooter(CallbackInfoReturnable<EntityLivingBase> cir) {
        if (cir.getReturnValue() == null && this.exploder instanceof BurstShooter
                && MANA_BURST.equals(this.exploder.getClass().getName())) {
            EntityLivingBase shooter = ((BurstShooter) this.exploder).aqtweaks$getShooter();
            if (shooter != null) {
                cir.setReturnValue(shooter);
            }
        }
    }
}

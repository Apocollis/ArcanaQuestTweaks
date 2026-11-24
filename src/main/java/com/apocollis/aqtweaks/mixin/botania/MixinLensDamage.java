package com.apocollis.aqtweaks.mixin.botania;

import com.apocollis.aqtweaks.botania.BurstShooter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.common.item.lens.LensDamage;

/**
 * Botania's Damage lens hurts with {@code DamageSource.MAGIC}, which has no attacker: struck mobs never target the
 * shooter and kills are not credited to them. With a shooter the hit is {@code causeIndirectMagicDamage(burst,
 * shooter)} instead: still magic and armour-bypassing, so the amount is unchanged. Spreaders (no shooter) keep
 * plain magic damage. The lens is a singleton used on the server thread, so the burst is parked in a field for
 * the redirect.
 */
@Mixin(value = LensDamage.class, remap = false)
public abstract class MixinLensDamage {

    private EntityThrowable aqtweaks$burst;

    @Inject(method = "updateBurst", at = @At("HEAD"), remap = false, require = 0)
    private void aqtweaks$captureBurst(IManaBurst burst, EntityThrowable entity, ItemStack stack, CallbackInfo ci) {
        this.aqtweaks$burst = entity;
    }

    @Inject(method = "updateBurst", at = @At("RETURN"), remap = false, require = 0)
    private void aqtweaks$releaseBurst(IManaBurst burst, EntityThrowable entity, ItemStack stack, CallbackInfo ci) {
        this.aqtweaks$burst = null;
    }

    @Redirect(method = "updateBurst", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/entity/EntityLivingBase;func_70097_a(Lnet/minecraft/util/DamageSource;F)Z"))
    private boolean aqtweaks$attributeToShooter(EntityLivingBase victim, DamageSource source, float amount) {
        EntityThrowable burst = this.aqtweaks$burst;
        EntityLivingBase shooter = ((Object) burst) instanceof BurstShooter ? ((BurstShooter) (Object) burst).aqtweaks$getShooter() : null;
        DamageSource attributed = shooter == null ? source : DamageSource.causeIndirectMagicDamage(burst, shooter);
        return victim.attackEntityFrom(attributed, amount);
    }
}

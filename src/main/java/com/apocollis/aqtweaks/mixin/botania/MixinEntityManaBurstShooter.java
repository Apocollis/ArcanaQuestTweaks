package com.apocollis.aqtweaks.mixin.botania;

import com.apocollis.aqtweaks.botania.BurstShooter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.entity.EntityManaBurst;

/**
 * The mana blaster's burst constructor ({@code EntityManaBurst(EntityPlayer, EnumHand)}) calls the world-only
 * {@code EntityThrowable} constructor, so the burst has no thrower and nothing records the shooter. The player is
 * parked on the burst so the Damage lens and the Explosive/Storm blasts can attribute their damage to them.
 * Spreader bursts use another constructor and stay unattributed.
 */
@Mixin(value = EntityManaBurst.class, remap = false)
public abstract class MixinEntityManaBurstShooter implements BurstShooter {

    @Unique
    private EntityLivingBase aqtweaks$shooter;

    @Inject(method = "<init>(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void aqtweaks$recordShooter(EntityPlayer player, EnumHand hand, CallbackInfo ci) {
        this.aqtweaks$shooter = player;
    }

    @Override
    public EntityLivingBase aqtweaks$getShooter() {
        return this.aqtweaks$shooter;
    }
}

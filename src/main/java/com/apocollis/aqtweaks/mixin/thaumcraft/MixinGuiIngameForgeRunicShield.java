package com.apocollis.aqtweaks.mixin.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apocollis.aqtweaks.thaumcraft.RunicShieldHud;

/**
 * While runic gear is charging the absorption pool, keep Forge from painting that pool as gold hearts.
 * The rune overlay is drawn from the same method after the red hearts.
 */
@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForgeRunicShield {

    @Inject(method = "renderHealth", at = @At("HEAD"), remap = false)
    private void aqtweaks$runicPrepare(int width, int height, CallbackInfo ci) {
        RunicShieldHud.prepare(width, height);
    }

    @Redirect(
            method = "renderHealth",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;getAbsorptionAmount()F",
                    remap = true))
    private float aqtweaks$hideRunicAbsorption(EntityPlayer player) {
        return RunicShieldHud.absorptionForHealth(player);
    }

    @Inject(method = "renderHealth", at = @At("RETURN"), remap = false)
    private void aqtweaks$runicDraw(int width, int height, CallbackInfo ci) {
        RunicShieldHud.draw();
    }
}

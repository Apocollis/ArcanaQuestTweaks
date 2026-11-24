package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.RunicShieldHud;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mantle's {@code ExtraHeartRenderHandler} cancels {@code RenderGameOverlayEvent.Pre(HEALTH)} and paints the whole
 * health bar itself, including absorption as gold hearts. {@code GuiIngameForge.renderHealth} (and with it our
 * redirect there and Thaumic Tweaker's {@code Post} overlay) then never runs. This applies the same three steps to
 * Mantle's renderer: record the layout, hide the runic part of the absorption, draw the rune rows afterwards.
 * Mantle is a plain class target by name, so no Mantle type is imported.
 */
@Mixin(targets = "slimeknights.mantle.client.ExtraHeartRenderHandler", remap = false)
public abstract class MixinMantleExtraHeartRunicShield {

    @Inject(method = "renderHealthbar", at = @At("HEAD"))
    private void aqtweaks$runicPrepare(RenderGameOverlayEvent.Pre event, CallbackInfo ci) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.HEALTH && !event.isCanceled()) {
            ScaledResolution res = event.getResolution();
            RunicShieldHud.prepare(res.getScaledWidth(), res.getScaledHeight());
        }
    }

    @Redirect(
            method = {"renderHealthbar", "renderExtraAbsorption"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;func_110139_bj()F"))
    private float aqtweaks$hideRunicAbsorption(EntityPlayer player) {
        return RunicShieldHud.absorptionForHealth(player);
    }

    @Inject(method = "renderHealthbar", at = @At("RETURN"))
    private void aqtweaks$runicDraw(RenderGameOverlayEvent.Pre event, CallbackInfo ci) {
        RunicShieldHud.draw();
    }
}

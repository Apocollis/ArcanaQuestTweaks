package com.apocollis.aqtweaks.mixin.elenaidodge;

import com.apocollis.aqtweaks.stamina.ElenaiFeatherHudColors;
import com.elenai.elenaidodge2.gui.DodgeGui;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extended paints layer 0 pure red on the gray tint sprites, and absorption brown on those same sprites.
 * Cancel those two private draws and use the sheet's blue, green, and gold instead.
 */
@Mixin(value = DodgeGui.class, remap = false)
public class MixinDodgeGui {

    @Inject(method = "renderLayeredBar", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$featherColors(
            GuiIngame gui,
            int width,
            int y,
            int amount,
            int vOffset,
            int uBackground,
            int uWhiteHalf,
            int uWhiteFull,
            int uTintHalf,
            int uTintFull,
            float[][] colors,
            boolean healing,
            boolean failed,
            boolean drawBackground,
            CallbackInfo ci) {
        ElenaiFeatherHudColors.renderLayeredBar(
                gui,
                width,
                y,
                amount,
                vOffset,
                uBackground,
                uWhiteHalf,
                uWhiteFull,
                uTintHalf,
                uTintFull,
                healing,
                failed,
                drawBackground);
        ci.cancel();
    }

    @Inject(method = "renderBaseWeightBar", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$weightColors(
            GuiIngame gui,
            int width,
            int y,
            int usable,
            int cap,
            int blocked,
            int vPatron,
            int uOutline,
            int uTintHalf,
            int uTintFull,
            boolean healing,
            boolean failed,
            CallbackInfo ci) {
        ElenaiFeatherHudColors.renderBaseWeightBar(
                gui, width, y, usable, cap, blocked, vPatron, uOutline, uTintHalf, uTintFull, healing, failed);
        ci.cancel();
    }

    /**
     * Extended blocks {@code clamp(weight - max(0, dodges - 20), 0, cap)} of the base row: bonus stamina absorbs
     * weight, so a full bar with a little armor reports nothing blocked, takes the plain layered path, and loses
     * both the iron and the green. Weight is a fixed cost, so the blocked part is the weight itself and the
     * armored bar stays on screen at every stamina level.
     */
    @Inject(method = "getBlockedBaseLayerAmount", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$fixedBlocked(int dodges, int weight, int cap, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(0, Math.min(Math.max(0, weight), cap)));
    }
}

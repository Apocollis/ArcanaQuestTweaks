package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.client.GuiMouseGrab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client-only: do not apply mouse look while a screen is open.
 * Vanilla gates {@code turn} on {@code inGameHasFocus} only, so a non-pausing GUI that
 * leaves focus set still yaws the player.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererMouse {

    @Redirect(
            method = "updateCameraAndRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"
            )
    )
    private void aqtweaks$turnUnlessScreen(EntityPlayerSP player, float yaw, float pitch) {
        if (!ArcanaQuestTweaksConfig.MineMenuModuleConfig.general.fixGuiMouseGrab) {
            player.turn(yaw, pitch);
            return;
        }
        if (Minecraft.getMinecraft().currentScreen != null || GuiMouseGrab.consumeLook()) {
            return;
        }
        player.turn(yaw, pitch);
    }
}

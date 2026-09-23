package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.client.GuiMouseGrab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Mouse;
import org.lwjglx.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only: when a screen closes back to play, re-grab the cursor and drop the
 * LWJGL recenter delta so the next camera update does not snap yaw.
 * Lives in the early json because {@link Minecraft} is already loaded when late mixins prepare.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftMouseGrab {

    @Inject(method = "displayGuiScreen", at = @At("RETURN"))
    private void aqtweaks$regrabMouse(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.MineMenuModuleConfig.general.fixGuiMouseGrab) {
            return;
        }
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.currentScreen != null || mc.world == null || mc.player == null) {
            return;
        }
        if (!Display.isActive()) {
            return;
        }
        Mouse.setGrabbed(false);
        Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
        Mouse.getDX();
        Mouse.getDY();
        mc.mouseHelper.grabMouseCursor();
        Mouse.getDX();
        Mouse.getDY();
        mc.inGameHasFocus = true;
        GuiMouseGrab.armSuppress();
    }
}

package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.client.GuiMouseGrab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Mouse;
import org.lwjglx.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

    /** Nested {@code setIngameFocus} re-enters {@code displayGuiScreen}. Resync only the outer return. */
    @Unique
    private int aqtweaks$guiDepth;

    /**
     * Somnia's wake packet calls {@code displayGuiScreen(null)} on the network thread. The cursor grab only
     * works on the client thread, yet {@code inGameHasFocus} is set, so the camera stops following the mouse
     * until a click or Escape. Run any off-thread screen change on the client thread instead.
     */
    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$marshalToClientThread(GuiScreen guiScreenIn, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (!mc.isCallingFromMinecraftThread()) {
            mc.addScheduledTask(() -> mc.displayGuiScreen(guiScreenIn));
            ci.cancel();
        }
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void aqtweaks$enterGui(GuiScreen guiScreenIn, CallbackInfo ci) {
        aqtweaks$guiDepth++;
    }

    @Inject(method = "displayGuiScreen", at = @At("RETURN"))
    private void aqtweaks$regrabMouse(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (aqtweaks$guiDepth > 0) {
            aqtweaks$guiDepth--;
        }
        if (aqtweaks$guiDepth != 0) {
            return;
        }
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

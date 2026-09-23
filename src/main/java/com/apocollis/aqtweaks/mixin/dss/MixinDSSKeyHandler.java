package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.stamina.DssSkillsGuiClient;
import dynamicswordskills.client.DSSKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjglx.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Key code 0 is {@code KEY_NONE}. lwjglx character events also use key 0, so an
 * unbound skills bind matches space. A real skills-key press is left to DSS.
 */
@Mixin(targets = "dynamicswordskills.client.DSSKeyHandler", remap = false)
public class MixinDSSKeyHandler {

    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$skillsKey(Minecraft mc, int key, CallbackInfo ci) {
        if (key == Keyboard.KEY_NONE) {
            ci.cancel();
            return;
        }
        KeyBinding skills = DSSKeyHandler.keys[DSSKeyHandler.KEY_SKILLS_GUI];
        if (skills != null && key == skills.getKeyCode()) {
            DssSkillsGuiClient.noteHardwareSkillsKey();
        }
    }
}

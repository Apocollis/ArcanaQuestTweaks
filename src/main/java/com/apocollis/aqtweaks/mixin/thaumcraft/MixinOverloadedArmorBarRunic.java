package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.RunicShieldHud;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Overloaded Armor Bar cancels the ARMOR element and places its own bar from the raw absorption amount, ignoring
 * {@code GuiIngameForge.left_height}. Runic charge is stored as absorption but drawn on the health row, so the bar
 * reserved a row that is not there; and it clamps absorption to 20 (one row) unless its "Override for Armor shift"
 * option is on, so it could never clear two or more extra rows (a rune row plus a gold row). The fix: read no
 * absorption at all while our runic HUD replaces the hearts, and move the finished Y up by the real extra rows.
 * Targeted by name; no type from that mod is imported. {@code index = 12} is the {@code int y} local in
 * overloadedarmorbar 1.0.4g ({@code istore 12} after the {@code - 10} that finishes the row maths).
 */
@Pseudo
@Mixin(targets = "locusway.overpoweredarmorbar.overlay.OverlayEventHandler", remap = false)
public abstract class MixinOverloadedArmorBarRunic {

    @Redirect(
            method = "renderArmorBar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;func_110139_bj()F"))
    private float aqtweaks$runicLayoutAbsorption(EntityPlayer player) {
        return RunicShieldHud.absorptionForArmorBar(player);
    }

    @ModifyVariable(method = "renderArmorBar", at = @At(value = "STORE", ordinal = 0), index = 12, require = 0)
    private int aqtweaks$clearRunicRows(int y) {
        return y - RunicShieldHud.armorBarShift(Minecraft.getMinecraft().player);
    }
}

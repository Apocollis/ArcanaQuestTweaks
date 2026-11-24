package com.apocollis.aqtweaks.mixin.somnia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla shows "Press SHIFT to dismount" when the client is seated. On Tweaks' invisible bed seat it shows one
 * line, "You're not tired yet. Press SHIFT to get up.", instead of two action-bar lines on top of each other.
 * Early json, so no Tweaks types: the seat is matched by class name. Other mounts keep vanilla's hint.
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClientBedSeat {

    private static final String BED_SEAT = "com.apocollis.aqtweaks.somnia.EntityBedSeat";

    @Redirect(method = "handleSetPassengers", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;setOverlayMessage(Ljava/lang/String;Z)V"))
    private void aqtweaks$bedSeatHint(GuiIngame gui, String message, boolean animate) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity vehicle = mc.player == null ? null : mc.player.getRidingEntity();
        if (vehicle != null && BED_SEAT.equals(vehicle.getClass().getName())) {
            message = I18n.format("aqtweaks.somnia.resting", mc.gameSettings.keyBindSneak.getDisplayName());
        }
        gui.setOverlayMessage(message, animate);
    }
}

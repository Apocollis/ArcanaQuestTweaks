package com.apocollis.aqtweaks.mixin.hud;

import com.apocollis.aqtweaks.client.HotbarSideLayout;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla draws the offhand slot on the side opposite the main hand. With {@code Offhand Slot Side = right} the
 * main hand is reported as LEFT here, so the slot (and its item) go right of the hotbar for any main hand.
 * Early json: {@code GuiIngame} is already loaded when late mixins prepare.
 */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngameOffhandSide {

    @Redirect(method = "renderHotbar", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;getPrimaryHand()Lnet/minecraft/util/EnumHandSide;"))
    private EnumHandSide aqtweaks$offhandRight(EntityPlayer player) {
        return HotbarSideLayout.rightSide() ? EnumHandSide.LEFT : player.getPrimaryHand();
    }
}

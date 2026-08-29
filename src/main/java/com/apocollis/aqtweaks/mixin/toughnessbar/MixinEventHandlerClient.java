package com.apocollis.aqtweaks.mixin.toughnessbar;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraftforge.client.GuiIngameForge;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Toughness Bar 2.4 hardcodes the hunger column ({@code right_height}, X = width/2+82, IINC -8).
 * Move it to the armor column so Elenai feathers can stay stacked above thirst on the right.
 * Mixin does not rewrite IINC; LTR is {@code 2 * origin - x} on each icon draw.
 */
@Mixin(targets = "com.tfar.toughnessbar.EventHandlerClient", remap = false)
public abstract class MixinEventHandlerClient {

    @Unique
    private int aqtweaks$ltrOrigin;

    private static boolean aqtweaks$armorSide() {
        return ArcanaQuestTweaksConfig.ClientModuleConfig.hud.moveToughnessBarToArmorSide;
    }

    @Inject(method = "onRenderArmorToughnessEvent", at = @At("HEAD"))
    private void aqtweaks$resetLtrOrigin(CallbackInfo ci) {
        aqtweaks$ltrOrigin = Integer.MIN_VALUE;
    }

    @Redirect(
            method = "onRenderArmorToughnessEvent",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraftforge/client/GuiIngameForge;right_height:I",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private static int aqtweaks$getHudColumnHeight() {
        if (!aqtweaks$armorSide()) {
            return GuiIngameForge.right_height;
        }
        // Overloaded Armor Bar cancels vanilla ARMOR and never increments left_height.
        return GuiIngameForge.left_height + 10;
    }

    @Redirect(
            method = "onRenderArmorToughnessEvent",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraftforge/client/GuiIngameForge;right_height:I",
                    opcode = Opcodes.PUTSTATIC
            )
    )
    private static void aqtweaks$setHudColumnHeight(int value) {
        if (aqtweaks$armorSide()) {
            GuiIngameForge.left_height = value;
            return;
        }
        GuiIngameForge.right_height = value;
    }

    /**
     * Stock start is {@code width/2 + 82} (food-aligned, drawing left).
     * Armor’s leftmost icon is {@code width/2 - 91}.
     */
    @ModifyConstant(method = "onRenderArmorToughnessEvent", constant = @Constant(intValue = 82))
    private int aqtweaks$armorColumnX(int original) {
        return aqtweaks$armorSide() ? -91 : original;
    }

    @ModifyArg(
            method = "onRenderArmorToughnessEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tfar/toughnessbar/EventHandlerClient;fullIcon(Lnet/minecraft/util/ResourceLocation;Lcom/tfar/toughnessbar/EventHandlerClient$ToughnessColor;Lnet/minecraft/util/ResourceLocation;III)Lnet/minecraft/util/ResourceLocation;"
            ),
            index = 3
    )
    private int aqtweaks$ltrFullX(int x) {
        return aqtweaks$ltrX(x);
    }

    @ModifyArg(
            method = "onRenderArmorToughnessEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tfar/toughnessbar/EventHandlerClient;halfIcon(Lnet/minecraft/util/ResourceLocation;Lcom/tfar/toughnessbar/EventHandlerClient$ToughnessColor;Lcom/tfar/toughnessbar/EventHandlerClient$ToughnessColor;Lnet/minecraft/util/ResourceLocation;II)Lnet/minecraft/util/ResourceLocation;"
            ),
            index = 4
    )
    private int aqtweaks$ltrHalfX(int x) {
        return aqtweaks$ltrX(x);
    }

    @Unique
    private int aqtweaks$ltrX(int x) {
        if (!aqtweaks$armorSide()) {
            return x;
        }
        if (aqtweaks$ltrOrigin == Integer.MIN_VALUE) {
            aqtweaks$ltrOrigin = x;
            return x;
        }
        return 2 * aqtweaks$ltrOrigin - x;
    }
}

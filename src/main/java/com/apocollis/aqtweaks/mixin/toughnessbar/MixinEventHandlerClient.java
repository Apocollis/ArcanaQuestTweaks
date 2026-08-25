package com.apocollis.aqtweaks.mixin.toughnessbar;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraftforge.client.GuiIngameForge;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Toughness Bar 2.4 hardcodes the hunger column ({@code right_height}, X = width/2+82).
 * Move it to the armor column so Elenai feathers can stay stacked above thirst on the right.
 */
@Mixin(targets = "com.tfar.toughnessbar.EventHandlerClient", remap = false)
public abstract class MixinEventHandlerClient {

    private static boolean aqtweaks$armorSide() {
        return ArcanaQuestTweaksConfig.ClientModuleConfig.hud.moveToughnessBarToArmorSide;
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
     * Vanilla armor starts at {@code width/2 - 91} and steps {@code +8}.
     */
    @ModifyConstant(method = "onRenderArmorToughnessEvent", constant = @Constant(intValue = 82))
    private int aqtweaks$armorColumnX(int original) {
        return aqtweaks$armorSide() ? -91 : original;
    }

    @ModifyConstant(method = "onRenderArmorToughnessEvent", constant = @Constant(intValue = -8))
    private int aqtweaks$iconStep(int original) {
        return aqtweaks$armorSide() ? 8 : original;
    }
}

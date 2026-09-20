package com.apocollis.aqtweaks.mixin.qualitytools;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.tmtravlr.qualitytools.CommonEventHandler;
import com.tmtravlr.qualitytools.QualityToolsHelper;

import net.minecraft.item.ItemStack;

@Mixin(value = CommonEventHandler.class, remap = false)
public abstract class MixinCommonEventHandler {

    @Redirect(
            method = "lambda$onLivingUpdate$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tmtravlr/qualitytools/QualityToolsHelper;generateQualityTag(Lnet/minecraft/item/ItemStack;Z)Z"))
    private static boolean aqtweaks$skipLivingStamp(ItemStack stack, boolean reforge) {
        if (ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable && !reforge) {
            return false;
        }
        return QualityToolsHelper.generateQualityTag(stack, reforge);
    }
}

package com.apocollis.aqtweaks.mixin.qualitytools;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apocollis.aqtweaks.qualitytools.QualityNbt;
import com.apocollis.aqtweaks.qualitytools.QualityStamp;
import com.tmtravlr.qualitytools.reforging.TileEntityReforgingStation;

import net.minecraft.item.ItemStack;

@Mixin(value = TileEntityReforgingStation.class, remap = false)
public abstract class MixinTileEntityReforgingStation {

    @Inject(method = "reforgeTool", at = @At("RETURN"))
    private void aqtweaks$syncQualityBase(CallbackInfo ci) {
        if (!QualityStamp.enabled()) {
            return;
        }
        ItemStack stack = ((TileEntityReforgingStation) (Object) this).func_70301_a(0);
        QualityNbt.syncBaseAfterReforge(stack);
    }
}

package com.apocollis.aqtweaks.mixin.qualitytools;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apocollis.aqtweaks.qualitytools.QualityRuneAnvilRecipe;

import net.minecraft.tileentity.TileEntity;
import teamroots.embers.tileentity.TileEntityDawnstoneAnvil;

@Mixin(value = TileEntityDawnstoneAnvil.class, remap = false)
public abstract class MixinTileEntityDawnstoneAnvil {

    @Inject(method = "onHit()V", at = @At("HEAD"))
    private void aqtweaks$runeMismatch(CallbackInfo ci) {
        QualityRuneAnvilRecipe.onFailedHammer((TileEntity) (Object) this);
    }
}

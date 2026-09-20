package com.apocollis.aqtweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityLockableLoot;

@Mixin(TileEntityLockableLoot.class)
public abstract class MixinTileEntityLockableLoot {

    @Inject(method = "fillWithLoot", at = @At("RETURN"))
    private void aqtweaks$stampLoot(EntityPlayer player, CallbackInfo ci) {
        com.apocollis.aqtweaks.qualitytools.QualityStamp.stampInventory((IInventory) this);
    }
}

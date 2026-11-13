package com.apocollis.aqtweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

@Mixin(TileEntityLockableLoot.class)
public abstract class MixinTileEntityLockableLoot {

    /** Nesting depth of fillWithLoot on this tile entity; only the outermost call decides whether to stamp. */
    @Unique
    private int aqtweaks$fillDepth;

    /** Whether the outermost call started with a loot table set. */
    @Unique
    private boolean aqtweaks$hadLoot;

    @Shadow
    protected ResourceLocation lootTable;

    @Shadow
    protected abstract NonNullList<ItemStack> getItems();

    @Inject(method = "fillWithLoot", at = @At("HEAD"))
    private void aqtweaks$markLootFill(EntityPlayer player, CallbackInfo ci) {
        if (this.aqtweaks$fillDepth++ == 0) {
            this.aqtweaks$hadLoot = this.lootTable != null;
        }
    }

    @Inject(method = "fillWithLoot", at = @At("RETURN"))
    private void aqtweaks$stampLoot(EntityPlayer player, CallbackInfo ci) {
        if (this.aqtweaks$fillDepth > 0) this.aqtweaks$fillDepth--;
        if (this.aqtweaks$fillDepth != 0 || !this.aqtweaks$hadLoot) {
            return;
        }
        this.aqtweaks$hadLoot = false;
        com.apocollis.aqtweaks.qualitytools.QualityStamp.stampStacks(this.getItems());
    }
}

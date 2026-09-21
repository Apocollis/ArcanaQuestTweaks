package com.apocollis.aqtweaks.mixin;

import java.util.ArrayDeque;

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

    @Unique
    private static final ThreadLocal<ArrayDeque<Boolean>> AQTWEAKS$FILL =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Shadow
    protected ResourceLocation lootTable;

    @Shadow
    protected abstract NonNullList<ItemStack> getItems();

    @Inject(method = "fillWithLoot", at = @At("HEAD"))
    private void aqtweaks$markLootFill(EntityPlayer player, CallbackInfo ci) {
        AQTWEAKS$FILL.get().addLast(Boolean.valueOf(this.lootTable != null));
    }

    @Inject(method = "fillWithLoot", at = @At("RETURN"))
    private void aqtweaks$stampLoot(EntityPlayer player, CallbackInfo ci) {
        ArrayDeque<Boolean> stack = AQTWEAKS$FILL.get();
        if (stack.isEmpty()) {
            return;
        }
        if (!Boolean.TRUE.equals(stack.removeLast())) {
            return;
        }
        com.apocollis.aqtweaks.qualitytools.QualityStamp.stampStacks(this.getItems());
    }
}

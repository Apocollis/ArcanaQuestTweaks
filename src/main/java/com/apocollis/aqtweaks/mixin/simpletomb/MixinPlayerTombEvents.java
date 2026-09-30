package com.apocollis.aqtweaks.mixin.simpletomb;

import com.apocollis.aqtweaks.simpletomb.TombSlotMaps;
import com.lothrazar.simpletomb.event.PlayerTombEvents;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PlayerTombEvents.class, remap = false)
public abstract class MixinPlayerTombEvents {

    @Redirect(
            method = "onPlayerDrops",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItemStacked(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/item/ItemStack;"
            )
    )
    private ItemStack aqtweaks$redirectInsertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        return TombSlotMaps.redirectInsertItemStacked(inventory, stack, simulate);
    }
}

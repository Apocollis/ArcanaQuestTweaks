package com.apocollis.aqtweaks.mixin.chisel;

import com.apocollis.aqtweaks.gamestages.GameStagesChiselHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.common.inventory.ContainerChisel;
import team.chisel.common.inventory.SlotChiselSelection;

@Mixin(value = SlotChiselSelection.class, remap = false)
public abstract class MixinSlotChiselSelection {

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$stageCraft(ContainerChisel container, EntityPlayer player, ItemStack selected,
            boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        String stage = GameStagesChiselHooks.lockedStage(player, selected);
        if (stage == null) {
            return;
        }
        if (!simulate) {
            GameStagesChiselHooks.notifyLocked(player, stage);
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
